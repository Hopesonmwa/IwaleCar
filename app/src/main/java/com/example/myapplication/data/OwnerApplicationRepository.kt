package com.example.myapplication.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Holds car wash owner applications.
 *
 * - Applications are cached in an observable in-memory list (same pattern as the other repositories).
 * - When a Firebase user is signed in, every change is also persisted to Firestore at
 *   `ownerApplications/{applicantUid}`. If that write fails, the operation fails and the local
 *   cache is NOT changed, so the UI never claims something was saved when it was not.
 * - When no Firebase session exists (local/offline demo mode and JVM unit tests) it works in memory only.
 */
object OwnerApplicationRepository {

    private const val COLLECTION = "ownerApplications"
    const val OWNER_ROLE = "Car Wash Owner"

    private val applications = mutableStateListOf<OwnerApplication>()

    private fun currentFirebaseUid(): String? = try {
        FirebaseServices.auth.currentUser?.uid
    } catch (e: Throwable) {
        null
    }

    private fun key(email: String) = email.trim().lowercase()

    // ---------- Reads (observable from Compose) ----------

    fun getAll(): List<OwnerApplication> = applications.sortedByDescending { it.submittedAt }

    fun getByStatus(status: ApplicationStatus): List<OwnerApplication> =
        getAll().filter { it.status == status }

    fun pendingCount(): Int = applications.count { it.status == ApplicationStatus.PENDING }

    fun getApplicationFor(email: String): OwnerApplication? =
        applications.firstOrNull { key(it.applicantEmail) == key(email) }

    fun resetToDefaultData() {
        applications.clear()
    }

    private fun upsertLocal(app: OwnerApplication) {
        val index = applications.indexOfFirst { key(it.applicantEmail) == key(app.applicantEmail) }
        if (index >= 0) applications[index] = app else applications.add(app)
    }

    // ---------- Applicant actions ----------

    /** Submits the detailed car wash owner application. */
    suspend fun submitFullApplication(
        applicant: UserAccount,
        facilityName: String,
        facilityContactNumber: String,
        shortDescription: String,
        city: String,
        district: String,
        physicalAddress: String,
        landmark: String,
        registrationNumber: String,
        operatingLicense: String,
        operatingDays: String,
        openingTime: String,
        closingTime: String
    ): Result<OwnerApplication> = withContext(Dispatchers.IO) {
        OwnerApplicationValidator.validateFullApplication(
            facilityName = facilityName,
            facilityContactNumber = facilityContactNumber,
            city = city,
            district = district,
            physicalAddress = physicalAddress,
            operatingDays = operatingDays,
            openingTime = openingTime,
            closingTime = closingTime
        )?.let { return@withContext Result.failure(IllegalArgumentException(it)) }

        val existing = getApplicationFor(applicant.email)
        if (existing != null && existing.status != ApplicationStatus.REJECTED) {
            return@withContext Result.failure(
                IllegalStateException(
                    if (existing.status == ApplicationStatus.APPROVED) "Your application has already been approved."
                    else "You already have an application under review."
                )
            )
        }

        val uid = currentFirebaseUid()
        val application = OwnerApplication(
            applicantUid = uid ?: "",
            applicantEmail = applicant.email.trim(),
            applicantName = applicant.fullName,
            phoneNumber = applicant.phoneNumber.ifBlank { facilityContactNumber },
            businessName = facilityName.trim(),
            facilityContactNumber = facilityContactNumber.trim(),
            description = shortDescription.trim(),
            photoCount = 2,
            city = city.trim(),
            district = district.trim(),
            businessAddress = physicalAddress.trim(),
            landmark = landmark.trim(),
            registrationNumber = registrationNumber.trim(),
            operatingLicense = operatingLicense.trim(),
            operatingDays = operatingDays.trim(),
            openingTime = openingTime.trim(),
            closingTime = closingTime.trim(),
            numberOfBays = 4,
            experienceYears = 2,
            status = ApplicationStatus.PENDING,
            submittedAt = System.currentTimeMillis()
        )

        if (uid != null) {
            try {
                FirebaseServices.db.collection(COLLECTION).document(uid).set(
                    hashMapOf(
                        "applicantUid" to application.applicantUid,
                        "applicantEmail" to application.applicantEmail,
                        "applicantName" to application.applicantName,
                        "phoneNumber" to application.phoneNumber,
                        "businessName" to application.businessName,
                        "facilityContactNumber" to application.facilityContactNumber,
                        "description" to application.description,
                        "photoCount" to application.photoCount,
                        "city" to application.city,
                        "district" to application.district,
                        "businessAddress" to application.businessAddress,
                        "landmark" to application.landmark,
                        "registrationNumber" to application.registrationNumber,
                        "operatingLicense" to application.operatingLicense,
                        "operatingDays" to application.operatingDays,
                        "openingTime" to application.openingTime,
                        "closingTime" to application.closingTime,
                        "numberOfBays" to application.numberOfBays,
                        "experienceYears" to application.experienceYears,
                        "status" to ApplicationStatus.PENDING.name,
                        "submittedAt" to application.submittedAt
                    )
                ).await()
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Could not submit your application. " + (e.localizedMessage ?: "Please try again."))
                )
            }
        }

        upsertLocal(application)

        // Trigger Admin Notification
        try {
            AdminNotificationRepository.createNotification(
                category = "APPLICATIONS",
                title = "New Owner Application Submitted",
                body = "Application for ${application.businessName} submitted by ${applicant.fullName}.",
                chipText = application.city
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Result.success(application)
    }

    /** Legacy submit function. */
    suspend fun submit(
        applicant: UserAccount,
        businessName: String,
        registrationNumber: String,
        businessAddress: String,
        city: String,
        numberOfBays: String,
        experienceYears: String,
        description: String
    ): Result<OwnerApplication> {
        return submitFullApplication(
            applicant = applicant,
            facilityName = businessName,
            facilityContactNumber = applicant.phoneNumber,
            shortDescription = description,
            city = city,
            district = "$city Urban",
            physicalAddress = businessAddress,
            landmark = "",
            registrationNumber = registrationNumber,
            operatingLicense = "",
            operatingDays = "Monday - Saturday",
            openingTime = "06:30 AM",
            closingTime = "08:00 PM"
        )
    }

    /** Pulls the signed-in applicant's own application. */
    suspend fun refreshOwn(email: String) = withContext(Dispatchers.IO) {
        val uid = currentFirebaseUid() ?: return@withContext
        try {
            val doc = FirebaseServices.db.collection(COLLECTION).document(uid).get().await()
            if (doc.exists()) upsertLocal(fromDoc(doc.id, doc.data ?: emptyMap(), email))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------- Admin actions ----------

    /** Admin: loads all applications from Firestore into the local list. */
    suspend fun refreshAll(): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentFirebaseUid() == null) return@withContext Result.success(Unit)
        try {
            val snapshot = FirebaseServices.db.collection(COLLECTION).get().await()
            snapshot.documents.forEach { doc ->
                upsertLocal(fromDoc(doc.id, doc.data ?: emptyMap(), ""))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not load applications. " + (e.localizedMessage ?: "")))
        }
    }

    suspend fun approve(applicantEmail: String, reviewerEmail: String): Result<OwnerApplication> =
        review(applicantEmail, reviewerEmail, ApplicationStatus.APPROVED, "")

    suspend fun reject(applicantEmail: String, reviewerEmail: String, notes: String): Result<OwnerApplication> {
        if (notes.isBlank()) {
            return Result.failure(IllegalArgumentException("Please provide a reason so the applicant can correct and resubmit."))
        }
        return review(applicantEmail, reviewerEmail, ApplicationStatus.REJECTED, notes.trim())
    }

    private suspend fun review(
        applicantEmail: String,
        reviewerEmail: String,
        newStatus: ApplicationStatus,
        notes: String
    ): Result<OwnerApplication> = withContext(Dispatchers.IO) {
        val current = getApplicationFor(applicantEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("Application not found."))
        if (current.status != ApplicationStatus.PENDING) {
            return@withContext Result.failure(IllegalStateException("This application has already been reviewed."))
        }

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = newStatus,
            reviewNotes = notes,
            reviewedAt = now,
            reviewedBy = reviewerEmail
        )

        // Persist remotely only when an admin Firebase session exists and the application has a uid.
        if (currentFirebaseUid() != null && current.applicantUid.isNotBlank()) {
            try {
                val db = FirebaseServices.db
                // 1) Application status first.
                db.collection(COLLECTION).document(current.applicantUid).update(
                    hashMapOf<String, Any>(
                        "status" to newStatus.name,
                        "reviewNotes" to notes,
                        "reviewedAt" to now,
                        "reviewedBy" to reviewerEmail
                    )
                ).await()
                // 2) Promote the user's role.
                if (newStatus == ApplicationStatus.APPROVED) {
                    db.collection("users").document(current.applicantUid)
                        .update("role", OWNER_ROLE).await()
                }
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception(
                        "Could not save the decision. Make sure you are signed in with the seeded " +
                                "administrator account. " + (e.localizedMessage ?: "")
                    )
                )
            }
        }

        upsertLocal(updated)
        if (newStatus == ApplicationStatus.APPROVED) {
            UserRepository.findUser(applicantEmail)?.let {
                UserRepository.registerUser(it.copy(role = OWNER_ROLE))
            }

            val facilityId = "fac_${current.applicantUid.ifBlank { System.currentTimeMillis().toString() }}"
            val approvedFacility = FacilityData(
                id = facilityId,
                name = current.businessName,
                area = current.city,
                address = current.businessAddress,
                city = current.city,
                phone = current.phoneNumber,
                bayCount = 0,
                openingTime = current.openingTime,
                closingTime = current.closingTime,
                description = current.description.ifBlank { "Approved Car Wash Facility" },
                status = "Approved",
                bays = emptyList(),
                ownerId = current.applicantEmail,
                operatingDays = FacilityRepository.parseOperatingDaysToCalendarList(current.operatingDays)
            )
            FacilityRepository.saveApprovedFacilityToFirestore(approvedFacility)
        } else if (newStatus == ApplicationStatus.REJECTED) {
            val facilityId = "fac_${current.applicantUid.ifBlank { System.currentTimeMillis().toString() }}"
            val rejectedFacility = FacilityData(
                id = facilityId,
                name = current.businessName,
                area = current.city,
                address = current.businessAddress,
                city = current.city,
                phone = current.phoneNumber,
                bayCount = current.numberOfBays,
                openingTime = current.openingTime,
                closingTime = current.closingTime,
                description = notes.ifBlank { "Application rejected by admin" },
                status = "Rejected",
                ownerId = current.applicantEmail,
                operatingDays = FacilityRepository.parseOperatingDaysToCalendarList(current.operatingDays)
            )
            FacilityRepository.saveRejectedFacilityToFirestore(rejectedFacility)
        }
        Result.success(updated)
    }

    private fun fromDoc(docId: String, data: Map<String, Any?>, fallbackEmail: String): OwnerApplication {
        fun str(k: String) = data[k] as? String ?: ""
        fun num(k: String) = (data[k] as? Number)?.toLong() ?: 0L
        return OwnerApplication(
            applicantUid = str("applicantUid").ifBlank { docId },
            applicantEmail = str("applicantEmail").ifBlank { fallbackEmail },
            applicantName = str("applicantName"),
            phoneNumber = str("phoneNumber"),
            businessName = str("businessName"),
            facilityContactNumber = str("facilityContactNumber").ifBlank { str("phoneNumber") },
            registrationNumber = str("registrationNumber"),
            operatingLicense = str("operatingLicense"),
            businessAddress = str("businessAddress"),
            landmark = str("landmark"),
            city = str("city"),
            district = str("district"),
            operatingDays = str("operatingDays").ifBlank { "Monday - Saturday" },
            openingTime = str("openingTime").ifBlank { "06:30 AM" },
            closingTime = str("closingTime").ifBlank { "08:00 PM" },
            photoCount = num("photoCount").toInt().coerceAtLeast(2),
            numberOfBays = num("numberOfBays").toInt(),
            experienceYears = num("experienceYears").toInt(),
            description = str("description"),
            status = runCatching { ApplicationStatus.valueOf(str("status")) }.getOrDefault(ApplicationStatus.PENDING),
            reviewNotes = str("reviewNotes"),
            submittedAt = num("submittedAt"),
            reviewedAt = num("reviewedAt"),
            reviewedBy = str("reviewedBy")
        )
    }
}
