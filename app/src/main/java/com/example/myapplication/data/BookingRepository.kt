package com.example.myapplication.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class BookingRecord(
    val id: String,
    val bookingReference: String,
    val facilityId: String,
    val bayId: String,
    val ownerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val bayTitle: String,
    val status: String, // "In Progress", "Paused", "Upcoming", "Completed", "Cancelled"
    val timeSlotText: String,
    val slotDetailText: String,
    val amountValue: Int,
    val amountFormatted: String,
    val paymentMethod: String,
    val paymentStatus: String = "SUCCESSFUL", // "SUCCESSFUL", "FAILED", "PENDING"
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long = System.currentTimeMillis() + 30 * 60 * 1000,
    val timestampMs: Long = System.currentTimeMillis()
)

object BookingRepository {
    private const val BOOKINGS_COLLECTION = "bookings"
    private val timeZone = TimeZone.getTimeZone("Africa/Blantyre")
    private val notifiedSessionIds = mutableSetOf<String>()

    private val initialBookings = listOf(
        // Area 4 Express Station (Facility "1", owner@gmail.com)
        BookingRecord(
            id = "BK-101",
            bookingReference = "IW-8924",
            facilityId = "1",
            bayId = "1_2",
            ownerId = "owner@gmail.com",
            customerName = "Kondwani Phiri",
            customerEmail = "user@gmail.com",
            customerPhone = "992929988",
            bayTitle = "Bay 02 — High-Pressure Lance",
            status = "In Progress",
            timeSlotText = "16:30 – 17:00 CAT (30 min)",
            slotDetailText = "Slot Active",
            amountValue = 28000,
            amountFormatted = "MWK 28,000",
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            startTimeMs = System.currentTimeMillis() - 10 * 60 * 1000,
            endTimeMs = System.currentTimeMillis() + 20 * 60 * 1000,
            timestampMs = System.currentTimeMillis()
        ),
        BookingRecord(
            id = "BK-102",
            bookingReference = "IW-3120",
            facilityId = "1",
            bayId = "1_1",
            ownerId = "owner@gmail.com",
            customerName = "Mwai Kanyama",
            customerEmail = "user@gmail.com",
            customerPhone = "992929988",
            bayTitle = "Bay 01 — Foam Cannon Express",
            status = "In Progress",
            timeSlotText = "14:40 – 15:00 CAT (20 min)",
            slotDetailText = "Temporarily Paused",
            amountValue = 22000,
            amountFormatted = "MWK 22,000",
            paymentMethod = "Iwale Wallet",
            paymentStatus = "SUCCESSFUL",
            startTimeMs = System.currentTimeMillis() - 15 * 60 * 1000,
            endTimeMs = System.currentTimeMillis() + 5 * 60 * 1000,
            timestampMs = System.currentTimeMillis() - 2 * 3600 * 1000
        ),
        BookingRecord(
            id = "BK-103",
            bookingReference = "IW-4412",
            facilityId = "1",
            bayId = "1_3",
            ownerId = "owner@gmail.com",
            customerName = "Tiwonge Banda",
            customerEmail = "user2@gmail.com",
            customerPhone = "999555666",
            bayTitle = "Bay 03 — Heavy Duty Underbody",
            status = "Upcoming",
            timeSlotText = "18:00 – 18:45 CAT (45 min)",
            slotDetailText = "Upcoming",
            amountValue = 18000,
            amountFormatted = "MWK 18,000",
            paymentMethod = "TNM Mpamba",
            paymentStatus = "SUCCESSFUL",
            startTimeMs = System.currentTimeMillis() + 2 * 3600 * 1000,
            endTimeMs = System.currentTimeMillis() + 2 * 3600 * 1000 + 45 * 60 * 1000,
            timestampMs = System.currentTimeMillis() - 4 * 3600 * 1000
        ),
        BookingRecord(
            id = "BK-104",
            bookingReference = "IW-5589",
            facilityId = "1",
            bayId = "1_4",
            ownerId = "owner@gmail.com",
            customerName = "Chifundo Moyo",
            customerEmail = "user@gmail.com",
            customerPhone = "992929988",
            bayTitle = "Bay 04 — Spot-Free Rinse",
            status = "Completed",
            timeSlotText = "10:30 – 11:00 CAT (30 min)",
            slotDetailText = "Completed",
            amountValue = 12000,
            amountFormatted = "MWK 12,000",
            paymentMethod = "Iwale Wallet",
            paymentStatus = "SUCCESSFUL",
            startTimeMs = System.currentTimeMillis() - 6 * 3600 * 1000,
            endTimeMs = System.currentTimeMillis() - 5 * 3600 * 1000,
            timestampMs = System.currentTimeMillis() - 6 * 3600 * 1000
        ),
        BookingRecord(
            id = "BK-105",
            bookingReference = "IW-6621",
            facilityId = "1",
            bayId = "1_1",
            ownerId = "owner@gmail.com",
            customerName = "Yamikani Chisi",
            customerEmail = "user@gmail.com",
            customerPhone = "992929988",
            bayTitle = "Bay 01 — Foam Cannon Express",
            status = "Cancelled",
            timeSlotText = "08:15 – 08:45 CAT (30 min)",
            slotDetailText = "Cancelled",
            amountValue = 5000,
            amountFormatted = "MWK 5,000",
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            startTimeMs = System.currentTimeMillis() - 8 * 3600 * 1000,
            endTimeMs = System.currentTimeMillis() - 7 * 3600 * 1000,
            timestampMs = System.currentTimeMillis() - 8 * 3600 * 1000
        )
    )

    private val bookings = mutableStateListOf<BookingRecord>().apply { addAll(initialBookings) }

    fun resetToDefaultData() {
        bookings.clear()
        bookings.addAll(initialBookings)
        notifiedSessionIds.clear()
    }

    suspend fun refreshBookingsFromFirestore(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = FirebaseServices.db.collection(BOOKINGS_COLLECTION).get().await()
            val fetchedList = mutableListOf<BookingRecord>()
            for (doc in snapshot.documents) {
                val id = doc.getString("bookingId") ?: doc.id
                val bookingReference = doc.getString("bookingReference") ?: "IW-1000"
                val facilityId = doc.getString("facilityId") ?: "1"
                val bayId = doc.getString("bayId") ?: "1_1"
                val ownerId = doc.getString("ownerId") ?: "owner@gmail.com"
                val customerName = doc.getString("customerName") ?: "Customer"
                val customerEmail = doc.getString("customerEmail") ?: ""
                val customerPhone = doc.getString("customerPhone") ?: ""
                val bayTitle = doc.getString("bayTitle") ?: "Bay 01"
                val status = doc.getString("bookingStatus") ?: "In Progress"
                val timeSlotText = doc.getString("timeSlotText") ?: "Active Slot"
                val slotDetailText = doc.getString("slotDetailText") ?: "Slot Active"
                val amountValue = doc.getLong("amountValue")?.toInt() ?: 15000
                val amountFormatted = doc.getString("amountFormatted") ?: "MWK 15,000"
                val paymentMethod = doc.getString("paymentMethod") ?: "Airtel Money"
                val paymentStatus = doc.getString("paymentStatus") ?: "SUCCESSFUL"
                val startTimeMs = doc.getLong("startTimeMs") ?: System.currentTimeMillis()
                val endTimeMs = doc.getLong("endTimeMs") ?: (System.currentTimeMillis() + 1800000L)
                val timestampMs = doc.getLong("timestampMs") ?: System.currentTimeMillis()

                fetchedList.add(
                    BookingRecord(
                        id = id,
                        bookingReference = bookingReference,
                        facilityId = facilityId,
                        bayId = bayId,
                        ownerId = ownerId,
                        customerName = customerName,
                        customerEmail = customerEmail,
                        customerPhone = customerPhone,
                        bayTitle = bayTitle,
                        status = status,
                        timeSlotText = timeSlotText,
                        slotDetailText = slotDetailText,
                        amountValue = amountValue,
                        amountFormatted = amountFormatted,
                        paymentMethod = paymentMethod,
                        paymentStatus = paymentStatus,
                        startTimeMs = startTimeMs,
                        endTimeMs = endTimeMs,
                        timestampMs = timestampMs
                    )
                )
            }

            if (fetchedList.isNotEmpty()) {
                for (fetched in fetchedList) {
                    val index = bookings.indexOfFirst { it.id == fetched.id }
                    if (index >= 0) {
                        bookings[index] = fetched
                    } else {
                        bookings.add(0, fetched)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addBookingRecord(record: BookingRecord) {
        val existingIndex = bookings.indexOfFirst { it.id == record.id }
        if (existingIndex >= 0) {
            bookings[existingIndex] = record
        } else {
            bookings.add(0, record)
        }
    }

    private fun isSameDayInMalawi(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getAllBookings(): List<BookingRecord> = bookings

    fun getBookingsForFacilityAndOwner(facilityId: String, ownerEmail: String): List<BookingRecord> {
        val ownerToMatch = ownerEmail.trim().lowercase()
        return bookings.filter {
            it.facilityId == facilityId && it.ownerId.trim().lowercase() == ownerToMatch
        }
    }

    fun getCustomerBookings(customerEmail: String, filter: String = "All"): List<BookingRecord> {
        val emailToMatch = customerEmail.trim().lowercase()
        val userBookings = bookings.filter { it.customerEmail.trim().lowercase() == emailToMatch }
        return when (filter.lowercase()) {
            "completed" -> userBookings.filter { it.status.equals("Completed", ignoreCase = true) }
            "cancelled" -> userBookings.filter { it.status.equals("Cancelled", ignoreCase = true) }
            else -> userBookings
        }
    }

    fun hasActiveOrUpcomingBookingForBay(facilityId: String, bayId: String): Boolean {
        return bookings.any {
            it.facilityId == facilityId &&
                    it.bayId == bayId &&
                    it.paymentStatus == "SUCCESSFUL" &&
                    (it.status.equals("In Progress", ignoreCase = true) ||
                            it.status.equals("Paused", ignoreCase = true) ||
                            it.status.equals("Upcoming", ignoreCase = true))
        }
    }

    fun hasOverlappingReservation(
        facilityId: String,
        bayId: String,
        newStartMs: Long,
        newEndMs: Long,
        excludeBookingId: String? = null
    ): Boolean {
        return bookings.any { booking ->
            if (booking.id == excludeBookingId) return@any false
            if (booking.facilityId != facilityId || booking.bayId != bayId) return@any false
            if (booking.status.equals("Cancelled", ignoreCase = true) || booking.paymentStatus == "FAILED") return@any false

            // Interval overlap rule: newStartMs < existingEndMs && existingStartMs < newEndMs
            newStartMs < booking.endTimeMs && booking.startTimeMs < newEndMs
        }
    }

    fun getReservationConflictType(
        facilityId: String,
        bayId: String,
        newStartMs: Long,
        newEndMs: Long,
        excludeBookingId: String? = null
    ): String? {
        val matching = bookings.find { booking ->
            if (booking.id == excludeBookingId) return@find false
            if (booking.facilityId != facilityId || booking.bayId != bayId) return@find false
            if (booking.status.equals("Cancelled", ignoreCase = true) || booking.paymentStatus == "FAILED") return@find false

            newStartMs < booking.endTimeMs && booking.startTimeMs < newEndMs
        } ?: return null

        return if (matching.paymentStatus == "PENDING") "Reserved" else "Booked"
    }

    fun createBooking(
        facilityId: String,
        bayId: String,
        ownerId: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        bayTitle: String,
        amountValue: Int,
        paymentMethod: String,
        paymentStatus: String = "SUCCESSFUL",
        durationMinutes: Int = 30,
        startTimeMs: Long = System.currentTimeMillis()
    ): Result<BookingRecord> {
        val endTimeMs = startTimeMs + durationMinutes * 60 * 1000L

        if (paymentStatus == "SUCCESSFUL" && hasOverlappingReservation(facilityId, bayId, startTimeMs, endTimeMs)) {
            return Result.failure(IllegalStateException("This bay is already reserved for the selected time slot."))
        }

        val reference = "IW-${(1000..9999).random()}"
        val record = BookingRecord(
            id = "BK-${UUID.randomUUID()}",
            bookingReference = reference,
            facilityId = facilityId,
            bayId = bayId,
            ownerId = ownerId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            bayTitle = bayTitle,
            status = if (paymentStatus == "SUCCESSFUL") "In Progress" else if (paymentStatus == "PENDING") "PENDING_PAYMENT" else "Cancelled",
            timeSlotText = "Active Slot ($durationMinutes min)",
            slotDetailText = if (paymentStatus == "SUCCESSFUL") "Slot Active" else if (paymentStatus == "PENDING") "Pending Payment Hold" else "Payment Failed",
            amountValue = amountValue,
            amountFormatted = "MWK " + NumberFormat.getNumberInstance().format(amountValue),
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            timestampMs = System.currentTimeMillis()
        )

        if (paymentStatus != "FAILED") {
            addBookingRecord(record)

            // Asynchronously persist customer booking to Firestore collection "bookings"
            try {
                val customerUid = try { FirebaseServices.auth.currentUser?.uid ?: "" } catch (e: Throwable) { "" }
                val bookingMap = hashMapOf(
                    "bookingId" to record.id,
                    "bookingReference" to record.bookingReference,
                    "facilityId" to record.facilityId,
                    "bayId" to record.bayId,
                    "ownerId" to record.ownerId,
                    "customerId" to customerUid,
                    "customerName" to record.customerName,
                    "customerEmail" to record.customerEmail,
                    "customerPhone" to record.customerPhone,
                    "bayTitle" to record.bayTitle,
                    "bookingStatus" to record.status,
                    "paymentStatus" to record.paymentStatus,
                    "amountValue" to record.amountValue,
                    "amountFormatted" to record.amountFormatted,
                    "paymentMethod" to record.paymentMethod,
                    "timeSlotText" to record.timeSlotText,
                    "slotDetailText" to record.slotDetailText,
                    "startTimeMs" to record.startTimeMs,
                    "endTimeMs" to record.endTimeMs,
                    "timestampMs" to record.timestampMs
                )
                FirebaseServices.db.collection(BOOKINGS_COLLECTION).document(record.id).set(bookingMap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(record)
    }

    fun extendSession(
        bookingId: String,
        additionalMinutes: Int,
        additionalAmount: Int,
        paymentMethod: String
    ): Result<BookingRecord> {
        val index = bookings.indexOfFirst { it.id == bookingId }
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Booking session not found."))
        }

        val currentBooking = bookings[index]
        val proposedNewEndMs = currentBooking.endTimeMs + additionalMinutes * 60 * 1000L

        if (hasOverlappingReservation(
                facilityId = currentBooking.facilityId,
                bayId = currentBooking.bayId,
                newStartMs = currentBooking.endTimeMs,
                newEndMs = proposedNewEndMs,
                excludeBookingId = currentBooking.id
            )
        ) {
            return Result.failure(IllegalStateException("Extension unavailable: This bay is reserved for a subsequent session."))
        }

        val newTotalAmount = currentBooking.amountValue + additionalAmount
        val updatedBooking = currentBooking.copy(
            endTimeMs = proposedNewEndMs,
            amountValue = newTotalAmount,
            amountFormatted = "MWK " + NumberFormat.getNumberInstance().format(newTotalAmount)
        )

        bookings[index] = updatedBooking
        return Result.success(updatedBooking)
    }

    fun shouldNotifySessionExpiry(bookingId: String, remainingSeconds: Long): Boolean {
        if (remainingSeconds in 1..300 && !notifiedSessionIds.contains(bookingId)) {
            notifiedSessionIds.add(bookingId)
            return true
        }
        return false
    }

    fun getTodayRevenueValue(facilityId: String, ownerEmail: String): Int {
        val now = System.currentTimeMillis()
        return getBookingsForFacilityAndOwner(facilityId, ownerEmail)
            .filter {
                it.paymentStatus == "SUCCESSFUL" && isSameDayInMalawi(it.timestampMs, now)
            }
            .sumOf { it.amountValue }
    }

    fun getTodayRevenueFormatted(facilityId: String, ownerEmail: String): String {
        val total = getTodayRevenueValue(facilityId, ownerEmail)
        return "MK " + NumberFormat.getNumberInstance().format(total)
    }

    fun getTodayBookingsCount(facilityId: String, ownerEmail: String): Int {
        val now = System.currentTimeMillis()
        return getBookingsForFacilityAndOwner(facilityId, ownerEmail)
            .count { isSameDayInMalawi(it.timestampMs, now) }
    }

    fun getActiveSessionsCount(facilityId: String, ownerEmail: String): Int {
        return getBookingsForFacilityAndOwner(facilityId, ownerEmail)
            .count { it.status.equals("In Progress", ignoreCase = true) || it.status.equals("Paused", ignoreCase = true) }
    }
}
