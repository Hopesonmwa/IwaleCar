package com.example.myapplication.data

enum class ApplicationStatus { PENDING, APPROVED, REJECTED }

/**
 * A car wash owner's detailed application to be verified as a Car Wash Owner.
 * Firestore document: ownerApplications/{applicantUid}
 */
data class OwnerApplication(
    val applicantUid: String,
    val applicantEmail: String,
    val applicantName: String,
    val phoneNumber: String,
    val businessName: String, // Facility Name
    val facilityContactNumber: String = "",
    val description: String = "", // Short Description
    val photoCount: Int = 2,
    val city: String = "Lilongwe",
    val district: String = "Lilongwe Urban",
    val businessAddress: String = "", // Physical address / area
    val landmark: String = "", // Nearby landmark
    val registrationNumber: String = "", // Business registration certificate
    val operatingLicense: String = "", // Operating licence
    val operatingDays: String = "Monday - Saturday", // Operating days
    val openingTime: String = "06:30 AM", // Opening time
    val closingTime: String = "08:00 PM", // Closing time
    val numberOfBays: Int = 4,
    val experienceYears: Int = 2,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val reviewNotes: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0L,
    val reviewedBy: String = ""
)

object OwnerApplicationValidator {

    /** Validates full car wash owner application form requirements. */
    fun validateFullApplication(
        facilityName: String,
        facilityContactNumber: String,
        city: String,
        district: String,
        physicalAddress: String,
        operatingDays: String,
        openingTime: String,
        closingTime: String
    ): String? {
        if (facilityName.isBlank()) return "Facility name is required."
        if (facilityContactNumber.isBlank()) return "Facility contact number is required."
        if (city.isBlank()) return "City is required."
        if (district.isBlank()) return "District is required."
        if (physicalAddress.isBlank()) return "Physical address / area is required."
        if (operatingDays.isBlank()) return "Operating days are required."
        if (openingTime.isBlank()) return "Opening time is required."
        if (closingTime.isBlank()) return "Closing time is required."
        return null
    }

    /** Legacy validator overload for dialog submission. */
    fun validate(
        businessName: String,
        businessAddress: String,
        city: String,
        numberOfBays: String,
        experienceYears: String
    ): String? {
        if (businessName.isBlank()) return "Please enter your facility name."
        if (businessAddress.isBlank()) return "Please enter the physical address."
        if (city.isBlank()) return "Please enter the city."
        return null
    }
}
