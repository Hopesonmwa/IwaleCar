import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

admin.initializeApp();

export { runLegacyBookingMigration } from "./migrateLegacyBookings";

/**
 * Callable Function: createBooking
 */
export const createBooking = onCall(
  { region: "us-central1", enforceAppCheck: true },
  async (request) => {
    // 1. Authentication check
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated to book a bay.");
    }
    const customerId = request.auth.uid;
    const customerEmail = request.auth.token.email || "";

    // 2. Validate payload object
    if (!request.data || typeof request.data !== "object" || Array.isArray(request.data)) {
      throw new HttpsError("invalid-argument", "Invalid payload format. Expected a JSON object.");
    }

    const { facilityId, bayId, tierId, startTimeMs, idempotencyKey } = request.data;

    // Validate ID strings (non-empty, max 128 chars, no slashes or spaces)
    const idFieldRegex = /^[a-zA-Z0-9_\-]{1,128}$/;
    if (!facilityId || typeof facilityId !== "string" || !idFieldRegex.test(facilityId) ||
        !bayId || typeof bayId !== "string" || !idFieldRegex.test(bayId) ||
        !tierId || typeof tierId !== "string" || !idFieldRegex.test(tierId)) {
      throw new HttpsError("invalid-argument", "Invalid or malformed facilityId, bayId, or tierId.");
    }

    // Timestamp validation: Must be finite integer and aligned to full minute (ms % 60000 === 0)
    if (typeof startTimeMs !== "number" || !Number.isInteger(startTimeMs) || !Number.isFinite(startTimeMs)) {
      throw new HttpsError("invalid-argument", "Invalid or missing startTimeMs timestamp.");
    }
    if (startTimeMs % 60000 !== 0) {
      throw new HttpsError("invalid-argument", "Booking start time must be aligned to a full minute (HH:mm:00).");
    }

    // Validate idempotency key format and length (secure alphanumeric 16-64 chars)
    const idempotencyRegex = /^[a-zA-Z0-9\-_]{16,64}$/;
    if (!idempotencyKey || typeof idempotencyKey !== "string" || !idempotencyRegex.test(idempotencyKey)) {
      throw new HttpsError("invalid-argument", "Invalid idempotency key format or length.");
    }

    const db = admin.firestore();

    // Deterministic safe document ID using SHA-256 hash of customerUid and idempotencyKey
    const hashedKey = crypto.createHash("sha256").update(`${customerId}_${idempotencyKey}`).digest("hex");
    const requestDocRef = db.collection("bookingRequests").doc(hashedKey);

    const facilityDocRef = db.collection("facilities").doc(facilityId);
    const bayDocRef = facilityDocRef.collection("bays").doc(bayId);
    const tierDocRef = bayDocRef.collection("tiers").doc(tierId);
    const scheduleDocRef = bayDocRef.collection("schedule").doc("state");
    const userDocRef = db.collection("users").doc(customerId);

    // 3. Atomic Transaction (All reads MUST precede all writes)
    return await db.runTransaction(async (transaction) => {
      const nowTimestamp = admin.firestore.Timestamp.now();

      // --- READ PHASE ---
      const requestSnap = await transaction.get(requestDocRef);
      if (requestSnap.exists) {
        const existingReq = requestSnap.data();
        if (!existingReq ||
            typeof existingReq.bookingId !== "string" ||
            typeof existingReq.bookingReference !== "string" ||
            typeof existingReq.customerId !== "string" ||
            typeof existingReq.facilityId !== "string" ||
            typeof existingReq.bayId !== "string" ||
            typeof existingReq.tierId !== "string" ||
            typeof existingReq.startTimeMs !== "number" ||
            typeof existingReq.amountValue !== "number" ||
            typeof existingReq.amountFormatted !== "string") {
          throw new HttpsError("internal", "Corrupted or incomplete idempotency record.");
        }

        // Verify submitted request parameters against original request
        if (
          existingReq.customerId !== customerId ||
          existingReq.facilityId !== facilityId ||
          existingReq.bayId !== bayId ||
          existingReq.tierId !== tierId ||
          existingReq.startTimeMs !== startTimeMs
        ) {
          throw new HttpsError("invalid-argument", "Idempotency key reused with different booking parameters.");
        }

        // Read referenced booking document inside the same transaction
        const referencedBookingDocRef = db.collection("bookings").doc(existingReq.bookingId);
        const bookingSnap = await transaction.get(referencedBookingDocRef);

        if (!bookingSnap.exists) {
          throw new HttpsError("internal", "Referenced booking for idempotency record no longer exists.");
        }

        const bookingData = bookingSnap.data()!;
        const bookingStartTimeMs = (bookingData.startTime instanceof admin.firestore.Timestamp)
          ? bookingData.startTime.toMillis()
          : 0;

        // Strict agreement check
        if (
          bookingData.bookingId !== existingReq.bookingId ||
          bookingData.bookingReference !== existingReq.bookingReference ||
          bookingData.customerId !== customerId ||
          bookingData.facilityId !== facilityId ||
          bookingData.bayId !== bayId ||
          bookingData.tierId !== tierId ||
          bookingData.amountValue !== existingReq.amountValue ||
          bookingData.amountFormatted !== existingReq.amountFormatted ||
          bookingData.idempotencyKey !== idempotencyKey ||
          bookingStartTimeMs !== startTimeMs
        ) {
          throw new HttpsError("internal", "Idempotency record and booking state mismatch.");
        }

        // --- IDEMPOTENCY STATE MATRIX EVALUATION ---
        const bStatus = bookingData.bookingStatus;
        const pStatus = bookingData.paymentStatus;
        const bExpiresAt = bookingData.expiresAt;

        if (bStatus === "CANCELLED") {
          return {
            success: false,
            bookingId: existingReq.bookingId,
            bookingReference: existingReq.bookingReference,
            bookingStatus: "CANCELLED",
            message: "Original booking request was cancelled or expired. Idempotency preserved; create a new booking with a new key."
          };
        }

        if (bStatus === "COMPLETED") {
          return {
            success: false,
            bookingId: existingReq.bookingId,
            bookingReference: existingReq.bookingReference,
            bookingStatus: "COMPLETED",
            message: "Original booking session has already been completed."
          };
        }

        if (bStatus === "PENDING_PAYMENT" && pStatus === "PENDING") {
          if (bExpiresAt instanceof admin.firestore.Timestamp && bExpiresAt.toMillis() > nowTimestamp.toMillis()) {
            return {
              success: true,
              bookingId: existingReq.bookingId,
              bookingReference: existingReq.bookingReference,
              bookingStatus: "PENDING_PAYMENT",
              amountValue: existingReq.amountValue,
              amountFormatted: existingReq.amountFormatted,
              message: "Idempotent request: returning active pending booking hold."
            };
          } else {
            return {
              success: false,
              bookingId: existingReq.bookingId,
              bookingReference: existingReq.bookingReference,
              bookingStatus: "EXPIRED",
              message: "Original booking hold has expired."
            };
          }
        }

        if (["UPCOMING", "IN_PROGRESS", "PAUSED"].includes(bStatus) && pStatus === "SUCCESSFUL") {
          return {
            success: true,
            bookingId: existingReq.bookingId,
            bookingReference: existingReq.bookingReference,
            bookingStatus: bStatus,
            amountValue: existingReq.amountValue,
            amountFormatted: existingReq.amountFormatted,
            message: "Idempotent request: returning active confirmed booking."
          };
        }

        throw new HttpsError("internal", "Idempotency check encountered an invalid or inconsistent booking state.");
      }

      const [userSnap, facilitySnap, baySnap, tierSnap, scheduleSnap] = await Promise.all([
        transaction.get(userDocRef),
        transaction.get(facilityDocRef),
        transaction.get(bayDocRef),
        transaction.get(tierDocRef),
        transaction.get(scheduleDocRef)
      ]);

      if (!userSnap.exists) throw new HttpsError("not-found", "User profile not found.");
      if (!facilitySnap.exists) throw new HttpsError("not-found", "Facility not found.");
      if (!baySnap.exists) throw new HttpsError("not-found", "Washing bay not found.");
      if (!tierSnap.exists) throw new HttpsError("not-found", "Service tier not found.");

      const userData = userSnap.data()!;
      const facilityData = facilitySnap.data()!;
      const bayData = baySnap.data()!;
      const tierData = tierSnap.data()!;

      // Validate customer authorization
      if (userData.role !== "Customer") {
        throw new HttpsError("permission-denied", "Only authorized customers can create bookings.");
      }

      if (facilityData.status !== "Approved") {
        throw new HttpsError("failed-precondition", "Facility is not approved for bookings.");
      }
      if (typeof facilityData.ownerId !== "string" || !facilityData.ownerId.trim()) {
        throw new HttpsError("internal", "Facility owner configuration is invalid.");
      }
      if (bayData.status !== "Available") {
        throw new HttpsError("failed-precondition", "Selected washing bay is currently unavailable.");
      }
      if (tierData.status !== "Available") {
        throw new HttpsError("failed-precondition", "Selected service tier is currently unavailable.");
      }

      if (typeof facilityData.name !== "string" || !facilityData.name.trim() ||
          typeof bayData.bayNumber !== "string" || !bayData.bayNumber.trim() ||
          typeof tierData.title !== "string" || !tierData.title.trim()) {
        throw new HttpsError("internal", "Invalid facility, bay, or tier naming configuration.");
      }

      const priceValue = tierData.priceValue;
      const durationMinutes = tierData.durationMinutes;
      if (typeof priceValue !== "number" || !Number.isInteger(priceValue) || !Number.isFinite(priceValue) || priceValue <= 0 ||
          typeof durationMinutes !== "number" || !Number.isInteger(durationMinutes) || !Number.isFinite(durationMinutes) || durationMinutes <= 0 || durationMinutes > 240) {
        throw new HttpsError("internal", "Invalid service tier pricing or duration configuration.");
      }

      const transactionNowMs = Date.now();
      const maxFutureMs = transactionNowMs + 90 * 24 * 60 * 60 * 1000;
      if (startTimeMs <= transactionNowMs || startTimeMs > maxFutureMs) {
        throw new HttpsError("invalid-argument", "Booking start time must be in the future and within 90 days.");
      }

      const endTimeMs = startTimeMs + durationMinutes * 60 * 1000;
      if (!Number.isInteger(endTimeMs) || !Number.isFinite(endTimeMs) || endTimeMs <= startTimeMs) {
        throw new HttpsError("invalid-argument", "Invalid calculated end timestamp.");
      }

      const startTime = admin.firestore.Timestamp.fromMillis(startTimeMs);
      const endTime = admin.firestore.Timestamp.fromMillis(endTimeMs);
      const expiresAt = admin.firestore.Timestamp.fromMillis(nowTimestamp.toMillis() + 10 * 60 * 1000);

      const openingTimeStr = facilityData.openingTime;
      const closingTimeStr = facilityData.closingTime;
      if (typeof openingTimeStr !== "string" || typeof closingTimeStr !== "string") {
        throw new HttpsError("internal", "Facility operating hours are improperly configured.");
      }

      if (!validateBlantyreOperatingHours(startTimeMs, endTimeMs, openingTimeStr, closingTimeStr)) {
        throw new HttpsError("out-of-range", "Requested time falls outside facility operating hours (Africa/Blantyre) or crosses midnight.");
      }

      // --- QUARANTINE SLOT GUARD EVALUATION INSIDE TRANSACTION ---
      const activeGuardsQuery = await transaction.get(
        db.collection("quarantineSlotGuards")
          .where("facilityId", "==", facilityId)
          .where("bayId", "==", bayId)
          .where("status", "==", "ACTIVE")
      );

      for (const guardDoc of activeGuardsQuery.docs) {
        const guard = guardDoc.data();
        if (guard.guardType === "ENTIRE_BAY") {
          throw new HttpsError("already-exists", "This washing bay is under administrative quarantine review and currently unavailable.");
        } else if (guard.guardType === "INTERVAL" && guard.startTime instanceof admin.firestore.Timestamp && guard.endTime instanceof admin.firestore.Timestamp) {
          const guardStartMs = guard.startTime.toMillis();
          const guardEndMs = guard.endTime.toMillis();
          const isOverlapping = guardStartMs < endTimeMs && guardEndMs > startTimeMs;
          if (isOverlapping) {
            throw new HttpsError("already-exists", "This washing bay time slot is reserved under quarantine review.");
          }
        }
      }

      // --- CONFLICT CHECK OVER EXISTING BOOKINGS ---
      const activeBookingsQuery = await transaction.get(
        db.collection("bookings")
          .where("facilityId", "==", facilityId)
          .where("bayId", "==", bayId)
          .where("startTime", "<", endTime)
      );

      for (const doc of activeBookingsQuery.docs) {
        const b = doc.data();
        const bStart = b.startTime;
        const bEnd = b.endTime;
        const bBookingStatus = b.bookingStatus;
        const bPaymentStatus = b.paymentStatus;
        const bExpiresAt = b.expiresAt;
        const isQuarantined = b.isQuarantined === true;
        const blocksSlot = b.blocksSlot === true;

        if (!(bStart instanceof admin.firestore.Timestamp) ||
            !(bEnd instanceof admin.firestore.Timestamp)) {
          throw new HttpsError("internal", "Conflict check encountered a malformed reservation record.");
        }

        if (isQuarantined) {
          if (blocksSlot && bEnd.toMillis() > nowTimestamp.toMillis()) {
            const isOverlapping = bStart.toMillis() < endTime.toMillis() && bEnd.toMillis() > startTime.toMillis();
            if (isOverlapping) {
              throw new HttpsError("already-exists", "This washing bay is reserved by a quarantined reservation undergoing review.");
            }
          }
          continue;
        }

        if (typeof bBookingStatus !== "string" || typeof bPaymentStatus !== "string") {
          throw new HttpsError("internal", "Conflict check encountered a malformed reservation record.");
        }

        if (bBookingStatus === "CANCELLED" || bBookingStatus === "COMPLETED") {
          continue;
        }

        let isBlockingSlot = false;

        if (["UPCOMING", "IN_PROGRESS", "PAUSED"].includes(bBookingStatus)) {
          if (bPaymentStatus === "SUCCESSFUL") {
            isBlockingSlot = true;
          } else {
            throw new HttpsError("internal", "Conflict check encountered an inconsistent reservation state.");
          }
        } else if (bBookingStatus === "PENDING_PAYMENT") {
          if (bPaymentStatus === "PENDING") {
            if (bExpiresAt instanceof admin.firestore.Timestamp) {
              const isExpired = bExpiresAt.toMillis() <= nowTimestamp.toMillis();
              if (!isExpired) {
                isBlockingSlot = true;
              }
            } else {
              isBlockingSlot = true;
            }
          } else {
            throw new HttpsError("internal", "Conflict check encountered an inconsistent reservation state.");
          }
        } else {
          throw new HttpsError("internal", "Conflict check encountered an unrecognized booking status.");
        }

        if (!isBlockingSlot) continue;

        const isOverlapping = bStart.toMillis() < endTime.toMillis() && bEnd.toMillis() > startTime.toMillis();
        if (isOverlapping) {
          throw new HttpsError("already-exists", "This washing bay is already reserved for the selected time slot.");
        }
      }

      // --- WRITE PHASE ---
      if (!scheduleSnap.exists) {
        transaction.set(scheduleDocRef, { lastUpdated: nowTimestamp, activeVersion: 1 });
      } else {
        const currentVersion = scheduleSnap.data()?.activeVersion || 1;
        transaction.update(scheduleDocRef, { lastUpdated: nowTimestamp, activeVersion: currentVersion + 1 });
      }

      const bookingRef = db.collection("bookings").doc();
      const bookingId = bookingRef.id;

      const blantyreDateStr = formatBlantyreDateString(startTimeMs);
      const reference = `IW-${blantyreDateStr}-${crypto.randomBytes(8).toString("hex").toUpperCase()}`;

      const newBooking = {
        bookingId,
        bookingReference: reference,
        facilityId,
        facilityName: facilityData.name,
        bayId,
        bayTitle: `${bayData.bayNumber} — ${tierData.title}`,
        tierId,
        customerId,
        customerName: userData.fullName || "Customer",
        customerEmail: customerEmail,
        customerPhone: userData.phoneNumber || "",
        ownerId: facilityData.ownerId,
        bookingStatus: "PENDING_PAYMENT",
        paymentStatus: "PENDING",
        refundStatus: "NOT_REQUIRED",
        amountValue: priceValue,
        amountFormatted: `MWK ${priceValue.toLocaleString()}`,
        paymentMethod: "PENDING_SELECTION",
        idempotencyKey,
        startTime,
        endTime,
        createdAt: nowTimestamp,
        updatedAt: nowTimestamp,
        expiresAt
      };

      transaction.set(bookingRef, newBooking);

      const idempotencyRecord = {
        bookingId,
        bookingReference: reference,
        customerId,
        facilityId,
        bayId,
        tierId,
        startTimeMs,
        amountValue: priceValue,
        amountFormatted: newBooking.amountFormatted,
        createdAt: nowTimestamp
      };
      transaction.set(requestDocRef, idempotencyRecord);

      return {
        success: true,
        bookingId,
        bookingReference: reference,
        amountValue: priceValue,
        amountFormatted: newBooking.amountFormatted
      };
    });
  }
);

/**
 * Scheduled Function: expirePendingBookings
 */
export const expirePendingBookings = onSchedule(
  {
    schedule: "every 1 minutes",
    timeZone: "Africa/Blantyre",
    region: "us-central1"
  },
  async () => {
    const db = admin.firestore();
    const nowTimestamp = admin.firestore.Timestamp.now();

    let expiredQuery;
    try {
      expiredQuery = await db.collection("bookings")
        .where("bookingStatus", "==", "PENDING_PAYMENT")
        .where("paymentStatus", "==", "PENDING")
        .where("expiresAt", "<=", nowTimestamp)
        .limit(100)
        .get();
    } catch (queryError) {
      console.error("FATAL: expirePendingBookings query failed:", queryError);
      throw queryError;
    }

    if (expiredQuery.empty) {
      return;
    }

    const results = await Promise.allSettled(
      expiredQuery.docs.map(async (doc) => {
        const docRef = doc.ref;
        await db.runTransaction(async (transaction) => {
          const transactionNowMs = admin.firestore.Timestamp.now();
          const snap = await transaction.get(docRef);
          if (!snap.exists) return;

          const b = snap.data()!;
          if (
            b.bookingStatus === "PENDING_PAYMENT" &&
            b.paymentStatus === "PENDING" &&
            b.expiresAt &&
            b.expiresAt instanceof admin.firestore.Timestamp &&
            b.expiresAt.toMillis() <= transactionNowMs.toMillis()
          ) {
            transaction.update(docRef, {
              bookingStatus: "CANCELLED",
              updatedAt: transactionNowMs
            });
          }
        });
      })
    );

    for (let i = 0; i < results.length; i++) {
      if (results[i].status === "rejected") {
        console.error(`Failed to process expiry for booking ${expiredQuery.docs[i].id}:`, (results[i] as PromiseRejectedResult).reason);
      }
    }
  }
);

/**
 * Callable Function: resolveQuarantinedBooking (Admin Resolution)
 */
export const resolveQuarantinedBooking = onCall(
  { region: "us-central1", enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }
    const callerRole = request.auth.token.role || "";
    const isAdmin = callerRole === "Administrator" || callerRole === "SuperAdmin" || request.auth.token.admin === true;
    if (!isAdmin) {
      throw new HttpsError("permission-denied", "Only administrators can resolve quarantined records.");
    }

    const { bookingId, resolution, newBookingStatus, newPaymentStatus, resolutionNotes, hasVerifiedPaymentEvidence } = request.data;
    if (!bookingId || typeof bookingId !== "string" ||
        !resolution || typeof resolution !== "string" ||
        !resolutionNotes || typeof resolutionNotes !== "string") {
      throw new HttpsError("invalid-argument", "Missing resolution parameters.");
    }

    const db = admin.firestore();
    const bookingRef = db.collection("bookings").doc(bookingId);
    const auditLogRef = db.collection("quarantineAuditLogs").doc();

    return await db.runTransaction(async (transaction) => {
      const snap = await transaction.get(bookingRef);
      if (!snap.exists) {
        throw new HttpsError("not-found", "Booking record not found.");
      }

      const data = snap.data()!;
      if (!data.isQuarantined) {
        throw new HttpsError("failed-precondition", "Booking record is not quarantined.");
      }

      const facilityId = data.facilityId;
      const bayId = data.bayId;
      const guardId = `guard_${facilityId}_${bayId}_${bookingId}`;
      const guardRef = db.collection("quarantineSlotGuards").doc(guardId);

      const nowTimestamp = admin.firestore.Timestamp.now();
      const updates: Record<string, any> = {
        isQuarantined: false,
        resolvedBy: request.auth.uid,
        resolvedAt: nowTimestamp,
        resolutionNotes: resolutionNotes,
        updatedAt: nowTimestamp
      };

      if (resolution === "APPROVE_ACTIVE") {
        if (newPaymentStatus === "SUCCESSFUL" && hasVerifiedPaymentEvidence !== true) {
          throw new HttpsError("failed-precondition", "Payment status cannot be set to SUCCESSFUL without verified payment evidence.");
        }
        updates.bookingStatus = newBookingStatus || "UPCOMING";
        updates.paymentStatus = newPaymentStatus || "SUCCESSFUL";
        updates.blocksSlot = true;
      } else if (resolution === "CANCEL_AND_RELEASE") {
        updates.bookingStatus = "CANCELLED";
        updates.blocksSlot = false;
      } else {
        throw new HttpsError("invalid-argument", "Unsupported resolution type.");
      }

      transaction.update(bookingRef, updates);

      // Atomically resolve corresponding Quarantine Slot Guard
      const guardSnap = await transaction.get(guardRef);
      if (guardSnap.exists) {
        transaction.update(guardRef, {
          status: "RESOLVED",
          resolvedAt: nowTimestamp,
          resolvedBy: request.auth.uid,
          resolutionNotes
        });
      }

      // Write immutable audit record
      transaction.set(auditLogRef, {
        logId: auditLogRef.id,
        bookingId,
        guardId,
        adminUid: request.auth.uid,
        previousBookingState: data,
        newBookingState: updates,
        resolution,
        resolutionNotes,
        timestamp: nowTimestamp
      });

      return { success: true, bookingId, resolution, guardId };
    });
  }
);

function validateBlantyreOperatingHours(startMs: number, endMs: number, openingStr: string, closingStr: string): boolean {
  const parseTimeString = (timeStr: string): number | null => {
    const trimmed = timeStr.trim();
    const match12 = trimmed.match(/^(0?[1-9]|1[0-2]):([0-5][0-9])\s?(AM|PM)$/i);
    if (match12) {
      let hours = parseInt(match12[1], 10);
      const minutes = parseInt(match12[2], 10);
      const meridiem = match12[3].toUpperCase();
      if (meridiem === "PM" && hours < 12) hours += 12;
      if (meridiem === "AM" && hours === 12) hours = 0;
      return hours * 60 + minutes;
    }

    const match24 = trimmed.match(/^([01]?[0-9]|2[0-3]):([0-5][0-9])$/);
    if (match24) {
      const hours = parseInt(match24[1], 10);
      const minutes = parseInt(match24[2], 10);
      return hours * 60 + minutes;
    }

    return null;
  };

  const openMins = parseTimeString(openingStr);
  const closeMins = parseTimeString(closingStr);
  if (openMins === null || closeMins === null || openMins >= closeMins) {
    return false;
  }

  const getBlantyreParts = (ms: number) => {
    const formatter = new Intl.DateTimeFormat("en-US", {
      timeZone: "Africa/Blantyre",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23"
    });
    const parts = formatter.formatToParts(new Date(ms));
    const year = parseInt(parts.find(p => p.type === "year")?.value || "0", 10);
    const month = parseInt(parts.find(p => p.type === "month")?.value || "0", 10);
    const day = parseInt(parts.find(p => p.type === "day")?.value || "0", 10);
    let hour = parseInt(parts.find(p => p.type === "hour")?.value || "0", 10);
    if (hour === 24) hour = 0;
    const minute = parseInt(parts.find(p => p.type === "minute")?.value || "0", 10);
    return { year, month, day, totalMinutes: hour * 60 + minute };
  };

  const startParts = getBlantyreParts(startMs);
  const endParts = getBlantyreParts(endMs);

  if (startParts.year !== endParts.year || startParts.month !== endParts.month || startParts.day !== endParts.day) {
    return false;
  }

  return startParts.totalMinutes >= openMins && endParts.totalMinutes <= closeMins && startParts.totalMinutes < endParts.totalMinutes;
}

function formatBlantyreDateString(ms: number): string {
  try {
    const formatter = new Intl.DateTimeFormat("en-US", {
      timeZone: "Africa/Blantyre",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    });
    const parts = formatter.formatToParts(new Date(ms));
    const y = parts.find(p => p.type === "year")?.value;
    const m = parts.find(p => p.type === "month")?.value;
    const d = parts.find(p => p.type === "day")?.value;
    if (!y || !m || !d) {
      throw new Error("Date formatting parts missing.");
    }
    return `${y}${m}${d}`;
  } catch (e) {
    throw new HttpsError("internal", "Failed to format local booking reference date.");
  }
}
