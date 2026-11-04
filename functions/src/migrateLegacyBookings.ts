import * as admin from "firebase-admin";

export interface MigrationOptions {
  dryRun: boolean;
  verifyMode: boolean;
  batchSize?: number;
}

export interface MigrationSummary {
  totalEvaluated: number;
  alreadyCompliant: number;
  migratedCount: number;
  quarantinedCount: number;
  failedCount: number;
  activeGuardsCreatedCount: number;
  entireBayGuardsCount: number;
  intervalGuardsCount: number;
  errors: string[];
}

export async function runLegacyBookingMigration(
  db: admin.firestore.Firestore,
  options: MigrationOptions = { dryRun: true, verifyMode: false, batchSize: 100 }
): Promise<MigrationSummary> {
  const rawBatchSize = options.batchSize || 100;
  if (!Number.isInteger(rawBatchSize) || rawBatchSize < 1 || rawBatchSize > 500) {
    throw new Error("Invalid batchSize: must be an integer between 1 and 500.");
  }
  const batchSize = rawBatchSize;

  console.log(`Starting Legacy Booking Migration [dryRun: ${options.dryRun}, verifyMode: ${options.verifyMode}, batchSize: ${batchSize}]...`);

  const summary: MigrationSummary = {
    totalEvaluated: 0,
    alreadyCompliant: 0,
    migratedCount: 0,
    quarantinedCount: 0,
    failedCount: 0,
    activeGuardsCreatedCount: 0,
    entireBayGuardsCount: 0,
    intervalGuardsCount: 0,
    errors: []
  };

  const nowTimestamp = admin.firestore.Timestamp.now();
  const nowMs = nowTimestamp.toMillis();

  let lastDoc: admin.firestore.QueryDocumentSnapshot | null = null;
  let hasMore = true;

  while (hasMore) {
    let query = db.collection("bookings").orderBy(admin.firestore.FieldPath.documentId()).limit(batchSize);
    if (lastDoc) {
      query = query.startAfter(lastDoc);
    }

    const snapshot = await query.get();
    if (snapshot.empty) {
      hasMore = false;
      break;
    }

    summary.totalEvaluated += snapshot.size;
    lastDoc = snapshot.docs[snapshot.docs.length - 1];

    let batch = db.batch();
    let batchOperationCount = 0;

    for (const doc of snapshot.docs) {
      try {
        const data = doc.data();
        const updates: Record<string, any> = {};

        const alreadyQuarantined = data.isQuarantined === true;
        const existingBlocksSlot = data.blocksSlot === true;
        const existingQuarantinedAt = data.quarantinedAt;
        const existingQuarantineReason = data.quarantineReason;

        let needsQuarantine = false;
        let quarantineReason = "";
        let guardType: "INTERVAL" | "ENTIRE_BAY" = "INTERVAL";

        // 1. Validate Timestamps
        let startTimeMs: number | null = null;
        let endTimeMs: number | null = null;

        if (data.startTime instanceof admin.firestore.Timestamp) {
          startTimeMs = data.startTime.toMillis();
        } else if (typeof data.startTimeMs === "number" && Number.isFinite(data.startTimeMs)) {
          startTimeMs = data.startTimeMs;
          updates.startTime = admin.firestore.Timestamp.fromMillis(data.startTimeMs);
        }

        if (data.endTime instanceof admin.firestore.Timestamp) {
          endTimeMs = data.endTime.toMillis();
        } else if (typeof data.endTimeMs === "number" && Number.isFinite(data.endTimeMs)) {
          endTimeMs = data.endTimeMs;
          updates.endTime = admin.firestore.Timestamp.fromMillis(data.endTimeMs);
        }

        if (startTimeMs === null || endTimeMs === null || startTimeMs >= endTimeMs) {
          needsQuarantine = true;
          quarantineReason = "INVALID_OR_MISSING_TIMESTAMPS";
          guardType = "ENTIRE_BAY"; // Conservative strategy: lock entire bay if timestamps missing/invalid
        }

        // 2. Validate Required Identifiers
        const facilityId = typeof data.facilityId === "string" ? data.facilityId.trim() : "";
        const bayId = typeof data.bayId === "string" ? data.bayId.trim() : "";
        const customerId = typeof data.customerId === "string" ? data.customerId.trim() : "";

        if (!facilityId || !bayId || !customerId) {
          needsQuarantine = true;
          quarantineReason = quarantineReason || "MISSING_REQUIRED_IDENTIFIERS";
          if (!facilityId || !bayId) {
            guardType = "ENTIRE_BAY";
          }
        }

        // 3. Validate Statuses
        let currentBookingStatus = data.bookingStatus;
        if (typeof currentBookingStatus !== "string" && typeof data.status === "string") {
          const mapped = data.status.trim().toUpperCase().replace(/\s+/g, "_");
          if (["PENDING_PAYMENT", "UPCOMING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED"].includes(mapped)) {
            currentBookingStatus = mapped;
            updates.bookingStatus = mapped;
          }
        }

        const validBookingStatuses = ["PENDING_PAYMENT", "UPCOMING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CANCELLED"];
        if (typeof currentBookingStatus !== "string" || !validBookingStatuses.includes(currentBookingStatus)) {
          needsQuarantine = true;
          quarantineReason = quarantineReason || "MISSING_OR_INVALID_BOOKING_STATUS";
        }

        const validPaymentStatuses = ["PENDING", "SUCCESSFUL", "FAILED"];
        let currentPaymentStatus = data.paymentStatus;
        if (typeof currentPaymentStatus !== "string" || !validPaymentStatuses.includes(currentPaymentStatus)) {
          needsQuarantine = true;
          quarantineReason = quarantineReason || "MISSING_OR_AMBIGUOUS_PAYMENT_STATUS";
        }

        // Validate refundStatus against full enum
        const validRefundStatuses = ["NOT_REQUIRED", "PENDING", "REFUND_PENDING", "REFUNDED", "REFUND_FAILED"];
        let currentRefundStatus = data.refundStatus;
        if (typeof currentRefundStatus !== "string" || !validRefundStatuses.includes(currentRefundStatus)) {
          if (currentPaymentStatus === "FAILED" || (currentBookingStatus === "CANCELLED" && currentPaymentStatus === "PENDING")) {
            updates.refundStatus = "NOT_REQUIRED";
          } else {
            needsQuarantine = true;
            quarantineReason = quarantineReason || "AMBIGUOUS_FINANCIAL_REFUND_STATE";
          }
        }

        // 4. Enforce Financial Consistency Matrix
        if (!needsQuarantine) {
          if (["UPCOMING", "IN_PROGRESS", "PAUSED"].includes(currentBookingStatus) && currentPaymentStatus !== "SUCCESSFUL") {
            needsQuarantine = true;
            quarantineReason = "INCONSISTENT_ACTIVE_STATE_PAYMENT_NOT_SUCCESSFUL";
          } else if (currentBookingStatus === "PENDING_PAYMENT" && currentPaymentStatus !== "PENDING") {
            needsQuarantine = true;
            quarantineReason = "INCONSISTENT_PENDING_STATE";
          } else if (currentBookingStatus === "CANCELLED" && currentPaymentStatus === "SUCCESSFUL" && !["REFUND_PENDING", "REFUNDED", "REFUND_FAILED"].includes(data.refundStatus)) {
            needsQuarantine = true;
            quarantineReason = "AMBIGUOUS_FINANCIAL_REFUND_STATE";
          }
        }

        // 5. Handle Quarantine and Slot Guard Synchronization
        if (alreadyQuarantined || needsQuarantine) {
          summary.quarantinedCount++;

          const isSlotActiveOrFuture = endTimeMs !== null && endTimeMs > nowMs;
          const blocksSlot = existingBlocksSlot || isSlotActiveOrFuture || guardType === "ENTIRE_BAY";

          updates.isQuarantined = true;
          updates.quarantineReason = existingQuarantineReason || quarantineReason || "MIGRATION_QUARANTINE";
          updates.quarantinedAt = existingQuarantinedAt || nowTimestamp;
          updates.blocksSlot = blocksSlot;
          updates.guardType = guardType;

          if (facilityId && bayId && blocksSlot) {
            const guardId = `guard_${facilityId}_${bayId}_${doc.id}`;
            const guardRef = db.collection("quarantineSlotGuards").doc(guardId);

            summary.activeGuardsCreatedCount++;
            if (guardType === "ENTIRE_BAY") {
              summary.entireBayGuardsCount++;
            } else {
              summary.intervalGuardsCount++;
            }

            if (!options.dryRun && !options.verifyMode) {
              batch.set(guardRef, {
                guardId,
                bookingId: doc.id,
                facilityId,
                bayId,
                guardType,
                startTime: startTimeMs ? admin.firestore.Timestamp.fromMillis(startTimeMs) : null,
                endTime: endTimeMs ? admin.firestore.Timestamp.fromMillis(endTimeMs) : null,
                status: "ACTIVE",
                quarantineReason: updates.quarantineReason,
                createdAt: updates.quarantinedAt,
                resolvedAt: null,
                resolvedBy: null
              }, { merge: true });
              batchOperationCount++;
            }
          }

          if (!options.dryRun && !options.verifyMode) {
            batch.update(doc.ref, updates);
            batchOperationCount++;
          }
        } else if (Object.keys(updates).length > 0) {
          summary.migratedCount++;
          if (!options.dryRun && !options.verifyMode) {
            batch.update(doc.ref, updates);
            batchOperationCount++;
          }
        } else {
          summary.alreadyCompliant++;
        }

      } catch (docError: any) {
        summary.failedCount++;
        const errMsg = `Document ${doc.id} failed processing: ${docError?.message || docError}`;
        summary.errors.push(errMsg);
        console.error(errMsg);
      }
    }

    if (!options.dryRun && !options.verifyMode && batchOperationCount > 0) {
      try {
        await batch.commit();
      } catch (commitError: any) {
        summary.failedCount += batchOperationCount;
        summary.errors.push(`Batch commit failed: ${commitError?.message || commitError}`);
        console.error("Batch commit failed:", commitError);
      }
    }

    if (snapshot.size < batchSize) {
      hasMore = false;
    }
  }

  console.log("=========================================");
  console.log("=== MIGRATION SUMMARY REPORT ===");
  console.log("=========================================");
  console.log(`Execution Mode:                 ${options.dryRun ? "DRY-RUN (0 Writes)" : options.verifyMode ? "VERIFY-MODE" : "COMMIT-MODE"}`);
  console.log(`Total Documents Evaluated:      ${summary.totalEvaluated}`);
  console.log(`Already Compliant:              ${summary.alreadyCompliant}`);
  console.log(`Planned/Committed Migrations:   ${summary.migratedCount}`);
  console.log(`Quarantined Documents:          ${summary.quarantinedCount}`);
  console.log(`Active Quarantine Guards:       ${summary.activeGuardsCreatedCount}`);
  console.log(`  - Entire Bay Guards:          ${summary.entireBayGuardsCount}`);
  console.log(`  - Interval Guards:            ${summary.intervalGuardsCount}`);
  console.log(`Failed Operations:              ${summary.failedCount}`);
  console.log("=========================================");

  if (options.verifyMode) {
    if (summary.migratedCount > 0 || summary.quarantinedCount > 0 || summary.failedCount > 0) {
      const verifyError = `VERIFICATION FAILED: Found ${summary.migratedCount} unmigrated docs, ${summary.quarantinedCount} quarantined docs (${summary.activeGuardsCreatedCount} active guards), and ${summary.failedCount} failed docs.`;
      console.error(verifyError);
      throw new Error(verifyError);
    } else {
      console.log("VERIFICATION SUCCESS: All documents in Firestore are 100% compliant.");
    }
  }

  return summary;
}
