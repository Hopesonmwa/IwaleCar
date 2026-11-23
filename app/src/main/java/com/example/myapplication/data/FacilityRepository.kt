package com.example.myapplication.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.ui.BayConfig
import com.example.myapplication.ui.FacilityItemData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar

data class FacilityData(
    val id: String,
    val name: String,
    val area: String,
    val address: String,
    val city: String,
    val phone: String,
    val bayCount: Int,
    val openingTime: String,
    val closingTime: String,
    val description: String,
    val status: String, // "Approved", "Pending Approval", "Rejected"
    val bays: List<BayConfig> = emptyList(),
    val ownerId: String = "owner@gmail.com",
    val badgeText: String = "High Pressure Jet",
    val badgeType: BadgeType = BadgeType.HIGH_PRESSURE_JET,
    val imageUrl: String = "https://images.unsplash.com/photo-1520340356584-f9917d1eea6f?auto=format&fit=crop&w=800&q=80",
    val operatingDays: List<Int> = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )
)

object FacilityRepository {

    private const val APPROVED_FACILITIES_COLLECTION = "approvedFacilities"
    private const val REJECTED_FACILITIES_COLLECTION = "rejectedFacilities"
    private const val FACILITIES_COLLECTION = "facilities"
    private const val BAYS_COLLECTION = "bays"

    private val db: FirebaseFirestore get() = FirebaseServices.db

    fun parseOperatingDaysToCalendarList(daysStr: String): List<Int> {
        return when {
            daysStr.contains("Except Saturday", ignoreCase = true) -> listOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
            daysStr.contains("Friday", ignoreCase = true) && !daysStr.contains("Saturday", ignoreCase = true) && !daysStr.contains("Sunday", ignoreCase = true) -> listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
            daysStr.contains("Saturday", ignoreCase = true) && !daysStr.contains("Sunday", ignoreCase = true) -> listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
            daysStr.contains("Sunday", ignoreCase = true) -> listOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
            else -> listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        }
    }

    // Facility repository holds only registered facilities from Firestore
    private val facilities = mutableStateListOf<FacilityData>()

    fun resetToDefaultData() {
        facilities.clear()
        _selectedFacilityId.value = null
    }

    private val _selectedFacilityId = mutableStateOf<String?>(null)

    val selectedFacilityId: String? get() = _selectedFacilityId.value

    fun setSelectedFacilityId(id: String) {
        _selectedFacilityId.value = id
    }

    /**
     * Asynchronously retrieves facilities from Firestore (`approvedFacilities`, `rejectedFacilities`, `facilities`).
     */
    suspend fun fetchApprovedFacilitiesFromFirestore(): Result<List<FacilityData>> = withContext(Dispatchers.IO) {
        try {
            val fetchedFacilities = mutableListOf<FacilityData>()

            // 1) Query approvedFacilities collection
            try {
                val approvedSnapshot = db.collection(APPROVED_FACILITIES_COLLECTION).get().await()
                for (doc in approvedSnapshot.documents) {
                    val fac = parseDocToFacility(doc.id, doc.data ?: emptyMap())
                    if (fac != null) fetchedFacilities.add(fac)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2) Query rejectedFacilities collection
            try {
                val rejectedSnapshot = db.collection(REJECTED_FACILITIES_COLLECTION).get().await()
                for (doc in rejectedSnapshot.documents) {
                    val fac = parseDocToFacility(doc.id, doc.data ?: emptyMap())
                    if (fac != null && fetchedFacilities.none { it.id == fac.id }) {
                        fetchedFacilities.add(fac)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3) Query general facilities collection
            try {
                val snapshot = db.collection(FACILITIES_COLLECTION).get().await()
                for (doc in snapshot.documents) {
                    val fac = parseDocToFacility(doc.id, doc.data ?: emptyMap())
                    if (fac != null && fetchedFacilities.none { it.id == fac.id }) {
                        fetchedFacilities.add(fac)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (fetchedFacilities.isNotEmpty()) {
                facilities.clear()
                facilities.addAll(fetchedFacilities)
            }

            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun parseDocToFacility(docId: String, data: Map<String, Any?>): FacilityData? {
        val facilityId = docId
        val name = data["name"] as? String ?: return null
        val area = data["area"] as? String ?: data["city"] as? String ?: "Lilongwe"
        val address = data["address"] as? String ?: ""
        val city = data["city"] as? String ?: area
        val phone = data["phone"] as? String ?: ""
        val openingTime = data["openingTime"] as? String ?: "06:30 AM"
        val closingTime = data["closingTime"] as? String ?: "08:00 PM"
        val description = data["description"] as? String ?: ""
        val status = data["status"] as? String ?: "Approved"
        val ownerId = data["ownerId"] as? String ?: ""
        val badgeText = data["badgeText"] as? String ?: "High Pressure Jet"
        val imageUrl = data["imageUrl"] as? String ?: "https://images.unsplash.com/photo-1520340356584-f9917d1eea6f?auto=format&fit=crop&w=800&q=80"

        val rawDays = data["operatingDays"] as? List<*>
        val operatingDaysStr = data["operatingDaysStr"] as? String ?: data["operatingDays"] as? String
        val operatingDays = rawDays?.mapNotNull { (it as? Long)?.toInt() }
            ?: parseOperatingDaysToCalendarList(operatingDaysStr ?: "Monday - Saturday")

        // Fetch bays subcollection & top-level bays collection for this facility
        val baysList = try {
            val baysSnapshot = db.collection(FACILITIES_COLLECTION)
                .document(facilityId)
                .collection("bays")
                .get()
                .await()

            if (baysSnapshot.isEmpty) {
                val topBaysSnapshot = db.collection(BAYS_COLLECTION)
                    .whereEqualTo("facilityId", facilityId)
                    .get()
                    .await()

                topBaysSnapshot.documents.map { bayDoc ->
                    BayConfig(
                        id = bayDoc.id,
                        bayNumber = bayDoc.getString("bayNumber") ?: "BAY 01",
                        serviceTitle = bayDoc.getString("serviceTitle") ?: "Foam Wash",
                        featuresDescription = bayDoc.getString("featuresDescription") ?: "High pressure rinse",
                        priceText = bayDoc.getString("priceText") ?: "MWK 5,000",
                        priceValue = bayDoc.getLong("priceValue")?.toInt() ?: 5000,
                        durationText = bayDoc.getString("durationText") ?: "30 mins",
                        status = bayDoc.getString("status") ?: "Available"
                    )
                }
            } else {
                baysSnapshot.documents.map { bayDoc ->
                    BayConfig(
                        id = bayDoc.id,
                        bayNumber = bayDoc.getString("bayNumber") ?: "BAY 01",
                        serviceTitle = bayDoc.getString("serviceTitle") ?: "Foam Wash",
                        featuresDescription = bayDoc.getString("featuresDescription") ?: "High pressure rinse",
                        priceText = bayDoc.getString("priceText") ?: "MWK 5,000",
                        priceValue = bayDoc.getLong("priceValue")?.toInt() ?: 5000,
                        durationText = bayDoc.getString("durationText") ?: "30 mins",
                        status = bayDoc.getString("status") ?: "Available"
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        return FacilityData(
            id = facilityId,
            name = name,
            area = area,
            address = address,
            city = city,
            phone = phone,
            bayCount = baysList.size,
            openingTime = openingTime,
            closingTime = closingTime,
            description = description,
            status = status,
            bays = baysList,
            ownerId = ownerId,
            badgeText = badgeText,
            imageUrl = imageUrl,
            operatingDays = operatingDays
        )
    }

    fun getAllFacilities(): List<FacilityData> {
        return facilities
    }

    fun getApprovedFacilities(): List<FacilityData> {
        return facilities.filter { it.status == "Approved" }
    }

    /**
     * Requirement 1: Only return approved facilities that have AT LEAST 1 configured bay.
     */
    fun getApprovedCarWashFacilities(): List<CarWashFacility> {
        return getApprovedFacilities()
            .filter { it.bays.isNotEmpty() || it.bayCount > 0 }
            .map { it.toCarWashFacility() }
    }

    fun getFacilityById(id: String): FacilityData? {
        return facilities.find { it.id == id } ?: facilities.firstOrNull()
    }

    fun getFacilitiesForOwner(ownerEmail: String): List<FacilityData> {
        val emailToMatch = ownerEmail.trim().lowercase()
        val ownerFacilities = facilities.filter { it.ownerId.trim().lowercase() == emailToMatch }
        if (ownerFacilities.isNotEmpty()) {
            return ownerFacilities
        }

        val currentUser = UserRepository.findUser(ownerEmail)
        val ownerName = currentUser?.fullName ?: "My Car Wash"
        val ownerLocation = currentUser?.primaryLocation ?: "Lilongwe"
        val facilityName = if (ownerName.endsWith("s", ignoreCase = true)) "${ownerName}' Car Wash" else "${ownerName}'s Car Wash"
        val newFacility = FacilityData(
            id = "fac_${System.currentTimeMillis()}",
            name = facilityName,
            area = ownerLocation,
            address = "Main Location, $ownerLocation",
            city = ownerLocation,
            phone = currentUser?.phoneNumber ?: "999000111",
            bayCount = 0,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "High-pressure wash station with dedicated bays",
            status = "Approved",
            bays = emptyList(),
            ownerId = ownerEmail.trim()
        )
        facilities.add(newFacility)
        return listOf(newFacility)
    }

    fun getSelectedFacilityForOwner(ownerEmail: String): FacilityData? {
        val ownerFacilities = getFacilitiesForOwner(ownerEmail)
        val currentId = selectedFacilityId
        val match = if (currentId != null) {
            ownerFacilities.find { it.id == currentId }
        } else null

        val selected = match ?: ownerFacilities.firstOrNull()
        if (selected != null) {
            setSelectedFacilityId(selected.id)
        }
        return selected
    }

    fun isFacilitySetupComplete(facilityId: String): Boolean {
        val facility = facilities.find { it.id == facilityId }
        return facility != null && facility.bays.isNotEmpty()
    }

    fun saveFacilityData(facility: FacilityData) {
        val existingIndex = facilities.indexOfFirst { it.id == facility.id }
        if (existingIndex >= 0) {
            facilities[existingIndex] = facility
        } else {
            facilities.add(facility)
        }
    }

    fun saveApprovedFacilityToFirestore(facility: FacilityData) {
        saveFacilityData(facility)
        try {
            val facilityMap = hashMapOf<String, Any>(
                "id" to facility.id,
                "name" to facility.name,
                "area" to facility.area,
                "address" to facility.address,
                "city" to facility.city,
                "phone" to facility.phone,
                "bayCount" to facility.bays.size,
                "openingTime" to facility.openingTime,
                "closingTime" to facility.closingTime,
                "description" to facility.description,
                "status" to "Approved",
                "ownerId" to facility.ownerId,
                "badgeText" to facility.badgeText,
                "imageUrl" to facility.imageUrl,
                "operatingDays" to facility.operatingDays
            )
            db.collection(APPROVED_FACILITIES_COLLECTION).document(facility.id).set(facilityMap)
            db.collection(FACILITIES_COLLECTION).document(facility.id).set(facilityMap)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun saveRejectedFacilityToFirestore(facility: FacilityData) {
        saveFacilityData(facility.copy(status = "Rejected"))
        try {
            val facilityMap = hashMapOf<String, Any>(
                "id" to facility.id,
                "name" to facility.name,
                "area" to facility.area,
                "address" to facility.address,
                "city" to facility.city,
                "phone" to facility.phone,
                "bayCount" to facility.bays.size,
                "openingTime" to facility.openingTime,
                "closingTime" to facility.closingTime,
                "description" to facility.description,
                "status" to "Rejected",
                "ownerId" to facility.ownerId,
                "badgeText" to facility.badgeText,
                "imageUrl" to facility.imageUrl,
                "operatingDays" to facility.operatingDays
            )
            db.collection(REJECTED_FACILITIES_COLLECTION).document(facility.id).set(facilityMap)
            db.collection(FACILITIES_COLLECTION).document(facility.id).set(facilityMap)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun saveFacility(item: FacilityItemData, ownerEmail: String = "owner@gmail.com") {
        val existingIndex = facilities.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            val current = facilities[existingIndex]
            val updated = current.copy(
                name = item.name,
                address = item.address,
                city = item.city,
                phone = item.phone,
                bayCount = current.bays.size,
                openingTime = item.openingTime,
                closingTime = item.closingTime,
                description = item.description,
                status = item.status
            )
            facilities[existingIndex] = updated
            if (updated.status == "Approved") {
                saveApprovedFacilityToFirestore(updated)
            } else if (updated.status == "Rejected") {
                saveRejectedFacilityToFirestore(updated)
            }
        } else {
            val newFacility = FacilityData(
                id = item.id,
                name = item.name,
                area = item.city,
                address = item.address,
                city = item.city,
                phone = item.phone,
                bayCount = 0,
                openingTime = item.openingTime,
                closingTime = item.closingTime,
                description = item.description,
                status = item.status,
                bays = emptyList(),
                ownerId = ownerEmail
            )
            facilities.add(newFacility)
            setSelectedFacilityId(newFacility.id)
            if (newFacility.status == "Approved") {
                saveApprovedFacilityToFirestore(newFacility)
            } else if (newFacility.status == "Rejected") {
                saveRejectedFacilityToFirestore(newFacility)
            }
        }
    }

    /**
     * Requirement 2: Record bay details in a dedicated Firestore collection "bays".
     */
    fun updateBaysForFacility(facilityId: String, bays: List<BayConfig>) {
        val existingIndex = facilities.indexOfFirst { it.id == facilityId }
        if (existingIndex >= 0) {
            val current = facilities[existingIndex]
            val updatedFacility = current.copy(
                bays = bays,
                bayCount = bays.size
            )
            facilities[existingIndex] = updatedFacility

            // Record bay details in dedicated Firestore collection "bays" and subcollection
            try {
                bays.forEach { bay ->
                    val bayMap = hashMapOf<String, Any>(
                        "bayId" to bay.id,
                        "facilityId" to facilityId,
                        "ownerId" to current.ownerId,
                        "bayNumber" to bay.bayNumber,
                        "serviceTitle" to bay.serviceTitle,
                        "priceText" to bay.priceText,
                        "priceValue" to bay.priceValue,
                        "durationText" to bay.durationText,
                        "featuresDescription" to bay.featuresDescription,
                        "status" to bay.status,
                        "timestampMs" to System.currentTimeMillis()
                    )
                    db.collection(BAYS_COLLECTION).document(bay.id).set(bayMap)
                    db.collection(FACILITIES_COLLECTION).document(facilityId).collection("bays").document(bay.id).set(bayMap)
                }

                if (updatedFacility.status == "Approved") {
                    saveApprovedFacilityToFirestore(updatedFacility)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

fun FacilityData.toCarWashFacility(): CarWashFacility {
    val startingPriceValue = bays.minOfOrNull { it.priceValue } ?: 5000
    val startingPriceFormatted = "MWK " + NumberFormat.getNumberInstance().format(startingPriceValue)
    val startingDuration = bays.firstOrNull()?.durationText ?: "30 mins"
    return CarWashFacility(
        id = this.id,
        name = this.name,
        area = this.area,
        location = this.address,
        price = startingPriceFormatted,
        duration = "/ ${startingDuration.replace(" mins", "m")}",
        badgeText = this.badgeText,
        badgeType = this.badgeType,
        imageUrl = this.imageUrl
    )
}

fun FacilityData.toCarWashTiers(): List<CarWashTier> {
    if (bays.isEmpty()) {
        return listOf(
            CarWashTier(
                id = "def_1",
                title = "Standard Wash",
                description = "Full exterior wash & spot rinse",
                price = 5000,
                formattedPrice = "MWK 5,000"
            )
        )
    }
    return bays.map { bay ->
        CarWashTier(
            id = bay.id,
            title = "${bay.durationText} - ${bay.serviceTitle}",
            description = bay.featuresDescription,
            price = bay.priceValue,
            formattedPrice = bay.priceText,
            isMostPopular = bay.status == "Available"
        )
    }
}

fun FacilityData.toFacilityItemData(): FacilityItemData {
    return FacilityItemData(
        id = this.id,
        name = this.name,
        address = this.address,
        city = this.city,
        phone = this.phone,
        bayCount = this.bays.size,
        openingTime = this.openingTime,
        closingTime = this.closingTime,
        description = this.description,
        status = this.status
    )
}
