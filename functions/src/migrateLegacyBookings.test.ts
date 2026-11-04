import { runLegacyBookingMigration, MigrationOptions } from "./migrateLegacyBookings";

describe("migrateLegacyBookings Audit Suite", () => {
  let mockDb: any;
  let docsStore: Map<string, any>;

  beforeEach(() => {
    docsStore = new Map();

    mockDb = {
      collection: (colName: string) => ({
        orderBy: () => ({
          limit: (batchSize: number) => ({
            startAfter: (lastDoc: any) => ({
              get: async () => {
                const docs = Array.from(docsStore.entries()).map(([id, data]) => ({
                  id,
                  data: () => data,
                  ref: { id }
                }));
                return {
                  empty: docs.length === 0,
                  size: docs.length,
                  docs
                };
              }
            }),
            get: async () => {
              const docs = Array.from(docsStore.entries()).map(([id, data]) => ({
                id,
                data: () => data,
                ref: { id }
              }));
              return {
                empty: docs.length === 0,
                size: docs.length,
                docs
              };
            }
          })
        })
      }),
      batch: () => ({
        update: (ref: any, updates: any) => {
          const current = docsStore.get(ref.id) || {};
          docsStore.set(ref.id, { ...current, ...updates });
        },
        commit: async () => {}
      })
    };
  });

  it("should recognize compliant legacy bookings without modifying them", async () => {
    const now = Date.now();
    docsStore.set("BK-101", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      bookingStatus: "UPCOMING",
      paymentStatus: "SUCCESSFUL",
      refundStatus: "NOT_REQUIRED",
      startTimeMs: now + 3600000,
      endTimeMs: now + 5400000
    });

    const options: MigrationOptions = { dryRun: true, verifyMode: false };
    const summary = await runLegacyBookingMigration(mockDb, options);

    expect(summary.totalEvaluated).toBe(1);
    expect(summary.alreadyCompliant).toBe(1);
    expect(summary.quarantinedCount).toBe(0);
    expect(summary.migratedCount).toBe(0);
  });

  it("should quarantine booking with missing paymentStatus without assuming SUCCESSFUL", async () => {
    const now = Date.now();
    docsStore.set("BK-AMBIGUOUS-1", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      bookingStatus: "UPCOMING",
      // paymentStatus is MISSING
      startTimeMs: now + 3600000,
      endTimeMs: now + 5400000
    });

    const options: MigrationOptions = { dryRun: false, verifyMode: false };
    const summary = await runLegacyBookingMigration(mockDb, options);

    expect(summary.quarantinedCount).toBe(1);
    const updated = docsStore.get("BK-AMBIGUOUS-1");
    expect(updated.isQuarantined).toBe(true);
    expect(updated.quarantineReason).toBe("MISSING_OR_AMBIGUOUS_PAYMENT_STATUS");
    expect(updated.paymentStatus).toBeUndefined(); // MUST NOT assume SUCCESSFUL
    expect(updated.blocksSlot).toBe(true); // Future window blocks slot
  });

  it("should quarantine booking with invalid timestamp ordering (startTime >= endTime)", async () => {
    const now = Date.now();
    docsStore.set("BK-TIMESTAMP-ERR", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      bookingStatus: "UPCOMING",
      paymentStatus: "SUCCESSFUL",
      startTimeMs: now + 5400000,
      endTimeMs: now + 3600000 // End before start
    });

    const options: MigrationOptions = { dryRun: false, verifyMode: false };
    const summary = await runLegacyBookingMigration(mockDb, options);

    expect(summary.quarantinedCount).toBe(1);
    const updated = docsStore.get("BK-TIMESTAMP-ERR");
    expect(updated.isQuarantined).toBe(true);
    expect(updated.quarantineReason).toBe("INVALID_OR_MISSING_TIMESTAMPS");
  });

  it("should quarantine inconsistent state combination (UPCOMING + FAILED)", async () => {
    const now = Date.now();
    docsStore.set("BK-INCONSISTENT-1", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      bookingStatus: "UPCOMING",
      paymentStatus: "FAILED", // Inconsistent
      startTimeMs: now + 3600000,
      endTimeMs: now + 5400000
    });

    const options: MigrationOptions = { dryRun: false, verifyMode: false };
    const summary = await runLegacyBookingMigration(mockDb, options);

    expect(summary.quarantinedCount).toBe(1);
    const updated = docsStore.get("BK-INCONSISTENT-1");
    expect(updated.isQuarantined).toBe(true);
    expect(updated.quarantineReason).toBe("INCONSISTENT_BOOKING_AND_PAYMENT_STATUS");
  });

  it("should execute dry-run mode without committing updates", async () => {
    const now = Date.now();
    docsStore.set("BK-DRY-RUN", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      status: "Upcoming", // Legacy status text
      paymentStatus: "SUCCESSFUL",
      startTimeMs: now + 3600000,
      endTimeMs: now + 5400000
    });

    const options: MigrationOptions = { dryRun: true, verifyMode: false };
    const summary = await runLegacyBookingMigration(mockDb, options);

    expect(summary.migratedCount).toBe(1);
    const docInDb = docsStore.get("BK-DRY-RUN");
    expect(docInDb.bookingStatus).toBeUndefined(); // Dry run made no writes
  });

  it("should fail verification mode when unmigrated documents exist", async () => {
    const now = Date.now();
    docsStore.set("BK-UNMIGRATED", {
      facilityId: "1",
      bayId: "1_1",
      customerId: "cust_123",
      status: "Upcoming",
      paymentStatus: "SUCCESSFUL",
      startTimeMs: now + 3600000,
      endTimeMs: now + 5400000
    });

    const options: MigrationOptions = { dryRun: false, verifyMode: true };
    await expect(runLegacyBookingMigration(mockDb, options)).rejects.toThrow("VERIFICATION FAILED");
  });
});
