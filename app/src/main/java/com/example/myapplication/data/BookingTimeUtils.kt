package com.example.myapplication.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object BookingTimeUtils {
    private val timeZone = TimeZone.getTimeZone("Africa/Blantyre")

    fun isDateInPast(dateMs: Long): Boolean {
        val todayCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return targetCal.timeInMillis < todayCal.timeInMillis
    }

    fun parseTimeStringToMinutes(timeStr: String): Int {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US).apply {
                this.timeZone = timeZone
            }
            val date = sdf.parse(timeStr.trim()) ?: return 0
            val cal = Calendar.getInstance(timeZone, Locale.ROOT).apply { time = date }
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            try {
                // Fallback for 24-hour format e.g. "06:30"
                val sdf2 = SimpleDateFormat("HH:mm", Locale.US).apply {
                    this.timeZone = timeZone
                }
                val date = sdf2.parse(timeStr.trim()) ?: return 0
                val cal = Calendar.getInstance(timeZone, Locale.ROOT).apply { time = date }
                return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            } catch (ex: Exception) {
                return 390 // Default 06:30 AM in minutes
            }
        }
    }

    fun isWithinOperatingHours(
        startMs: Long,
        endMs: Long,
        openingTimeStr: String,
        closingTimeStr: String
    ): Boolean {
        val openMinutes = parseTimeStringToMinutes(openingTimeStr)
        val closeMinutes = parseTimeStringToMinutes(closingTimeStr)

        val startCal = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = startMs }
        val endCal = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = endMs }

        val startMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)
        val endMinutes = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)

        return startMinutes >= openMinutes && endMinutes <= closeMinutes && startMinutes < endMinutes
    }

    fun generateAvailableSlots(
        dateMs: Long,
        durationMinutes: Int,
        openingTimeStr: String,
        closingTimeStr: String,
        facilityId: String,
        bayId: String
    ): List<TimeSlotInfo> {
        val slots = mutableListOf<TimeSlotInfo>()
        val openMinutes = parseTimeStringToMinutes(openingTimeStr)
        val closeMinutes = parseTimeStringToMinutes(closingTimeStr)

        val baseCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nowMs = System.currentTimeMillis()
        var currentMinutes = openMinutes
        val slotInterval = if (durationMinutes in 5..180) durationMinutes else 30

        while (currentMinutes + slotInterval <= closeMinutes) {
            val slotStartCal = baseCal.clone() as Calendar
            slotStartCal.add(Calendar.MINUTE, currentMinutes)
            val startMs = slotStartCal.timeInMillis
            val endMs = startMs + slotInterval * 60 * 1000L

            val isPast = startMs < nowMs
            val conflictType = BookingRepository.getReservationConflictType(facilityId, bayId, startMs, endMs)
            val isConflicting = conflictType != null

            val reasonText = when {
                isPast -> "Time Passed"
                conflictType == "Booked" -> "Booked"
                conflictType == "Reserved" -> "Reserved"
                isConflicting -> "Reserved"
                else -> "Available"
            }

            val startTimeText = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = timeZone }.format(slotStartCal.time)
            val endCal = (slotStartCal.clone() as Calendar).apply { add(Calendar.MINUTE, slotInterval) }
            val endTimeText = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = timeZone }.format(endCal.time)

            slots.add(
                TimeSlotInfo(
                    startMs = startMs,
                    endMs = endMs,
                    timeLabel = "$startTimeText – $endTimeText CAT",
                    isAvailable = !isPast && !isConflicting,
                    reason = reasonText
                )
            )

            currentMinutes += slotInterval
        }

        return slots
    }
}

data class TimeSlotInfo(
    val startMs: Long,
    val endMs: Long,
    val timeLabel: String,
    val isAvailable: Boolean,
    val reason: String
)
