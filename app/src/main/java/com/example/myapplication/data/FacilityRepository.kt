package com.example.myapplication.data

import androidx.compose.runtime.mutableStateListOf
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

    private val db: FirebaseFirestore get() = FirebaseServices.db

    private val defaultArea4Bays = listOf(
        BayConfig(id = "1_1", bayNumber = "BAY 01", serviceTitle = "Foam Cannon Express", priceText = "MWK 5,000", priceValue = 5000, durationText = "10 mins", featuresDescription = "Foam lance, wheel blast & spot rinse", status = "Available"),
        BayConfig(id = "1_2", bayNumber = "BAY 02", serviceTitle = "High-Pressure Wash & Foam", priceText = "MWK 10,000", priceValue = 10000, durationText = "20 mins", featuresDescription = "High-pressure jet, active foam & wax", status = "Available"),
        BayConfig(id = "1_3", bayNumber = "BAY 03", serviceTitle = "Heavy Duty Underbody", priceText = "MWK 15,000", priceValue = 15000, durationText = "30 mins", featuresDescription = "Underbody rinse, engine wash & vacuum", status = "Available"),
        BayConfig(id = "1_4", bayNumber = "BAY 04", serviceTitle = "Spot-Free Rinse", priceText = "MWK 20,000", priceValue = 20000, durationText = "40 mins", featuresDescription = "Deionized spot-free rinse & air dry", status = "Available")
    )

    private val defaultGinneryBays = listOf(
        BayConfig(id = "4_1", bayNumber = "BAY 01", serviceTitle = "Express Wash", priceText = "MWK 6,000", priceValue = 6000, durationText = "15 mins", featuresDescription = "Foam wash & high pressure rinse", status = "Available"),
        BayConfig(id = "4_2", bayNumber = "BAY 02", serviceTitle = "Foam & Vacuum", priceText = "MWK 12,000", priceValue = 12000, durationText = "30 mins", featuresDescription = "Ceramic foam & interior vacuum", status = "Available")
    )

    private val initialFacilities = listOf(
        FacilityData(
            id = "1",
            name = "Area 4 Express Station",
            area = "Lilongwe",
            address = "Plot 18, Area 4, Lilongwe",
            city = "Lilongwe",
            phone = "999123456",
            bayCount = 4,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "High-Pressure Washing Bays • 06:30 – 20:00 CAT",
            status = "Approved",
            ownerId = "owner@gmail.com",
            bays = defaultArea4Bays,
            operatingDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        ),
        FacilityData(
            id = "2",
            name = "Area 3 Sparkle Hub",
            area = "Lilongwe",
            address = "Off Chilambula Road, Area 3, Lilongwe",
            city = "Lilongwe",
            phone = "999654321",
            bayCount = 3,
            openingTime = "07:00 AM",
            closingTime = "07:00 PM",
            description = "Foam Cannon & Ceramic Rinse Bays",
            status = "Approved",
            ownerId = "owner@gmail.com",
            bays = defaultArea4Bays.take(3),
            operatingDays = listOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        ),
        FacilityData(
            id = "3",
            name = "City Centre Automated Wash",
            area = "Lilongwe",
            address = "Presidential Way, City Centre, Lilongwe",
            city = "Lilongwe",
            phone = "999777888",
            bayCount = 2,
            openingTime = "06:00 AM",
            closingTime = "09:00 PM",
            description = "Touchless Tunnel & Underbody Rinse",
            status = "Approved",
            ownerId = "owner@gmail.com",
            bays = defaultArea4Bays.take(2),
            operatingDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
        ),
        FacilityData(
            id = "4",
            name = "Ginnery Corner Auto Spa",
            area = "Blantyre",
            address = "Masauko Chipembere Hwy, Blantyre",
            city = "Blantyre",
            phone = "888654321",
            bayCount = 2,
            openingTime = "07:00 AM",
            closingTime = "07:30 PM",
            description = "Automated Foam Bays • Blantyre Express",
            status = "Approved",
            ownerId = "carwash@gmail.com",
            bays = defaultGinneryBays,
            operatingDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        ),
        FacilityData(
            id = "5",
            name = "Zomba Central Wash",
            area = "Zomba",
            address = "M1 Road, Opposite University, Zomba",
            city = "Zomba",
            phone = "888111333",
            bayCount = 2,
            openingTime = "07:00 AM",
            closingTime = "06:00 PM",
            description = "Express Self-Service Bays",
            status = "Approved",
            ownerId = "owner@gmail.com",
            bays = defaultArea4Bays.take(2)
        ),
        FacilityData(
            id = "6",
            name = "Mzuzu Katoto Express",
            area = "Mzuzu",
            address = "Katoto Main Street, Mzuzu",
            city = "Mzuzu",
            phone = "888222444",
            bayCount = 2,
            openingTime = "07:00 AM",
            closingTime = "06:00 PM",
            description = "High Pressure & Vacuum Bays",
            status = "Approved",
            ownerId = "carwash@gmail.com",
            bays = defaultGinneryBays
        ),
        FacilityData(
            id = "7",
            name = "Kanengo Industrial Wash",
            area = "Lilongwe",
            address = "M1 Road, Kanengo Light Industrial Area, Lilongwe",
            city = "Lilongwe",
            phone = "999888777",
            bayCount = 0,
            openingTime = "08:00 AM",
            closingTime = "06:00 PM",
            description = "Incomplete business registration certificate (MBRS) and missing environmental waste clearance.",
            status = "Rejected",
            ownerId = "carwash@gmail.com",
            bays = emptyList()
        )
    )

    private val facilities = mutableStateListOf<FacilityData>().apply { addAll(initialFacilities) }

    fun resetToDefaultData() {
        facilities.clear()
        facilities.addAll(initialFacilities)
        selectedFacilityId = null
    }

    private var selectedFacilityId: String? = null

    fun setSelectedFacilityId(id: String) {
        selectedFacilityId = id
    }

    /**
     * Asynchronously retrieves approved facilities and their washing bays from Firestore `facilities/{facilityId}` and `facilities/{facilityId}/bays`.
     */
    suspend fun fetchApprovedFacilitiesFromFirestore(): Result<List<FacilityData>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("facilities")
                .whereEqualTo("status", "Approved")
                .get()
                .await()

            val fetchedFacilities = mutableListOf<FacilityData>()

            for (doc in snapshot.documents) {
                val facilityId = doc.id
                val name = doc.getString("name") ?: ""
                val area = doc.getString("area") ?: doc.getString("city") ?: "Lilongwe"
                val address = doc.getString("address") ?: ""
                val city = doc.getString("city") ?: area
                val phone = doc.getString("phone") ?: ""
                val openingTime = doc.getString("openingTime") ?: "06:30 AM"
                val closingTime = doc.getString("closingTime") ?: "08:00 PM"
                val description = doc.getString("description") ?: ""
                val status = doc.getString("status") ?: "Approved"
                val ownerId = doc.getString("ownerId") ?: ""
                val badgeText = doc.getString("badgeText") ?: "High Pressure Jet"
                val imageUrl = doc.getString("imageUrl") ?: "https://images.unsplash.com/photo-1520340356584-f9917d1eea6f?auto=format&fit=crop&w=800&q=80"

                val rawDays = doc.get("operatingDays") as? List<*>
                val operatingDays = rawDays?.mapNotNull { (it as? Long)?.toInt() } ?: listOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
                )

                // Fetch bays subcollection for this facility
                val baysSnapshot = db.collection("facilities")
                    .document(facilityId)
                    .collection("bays")
                    .get()
                    .await()

                val baysList = baysSnapshot.documents.map { bayDoc ->
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

                fetchedFacilities.add(
                    FacilityData(
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
                )
            }

            if (fetchedFacilities.isNotEmpty()) {
                facilities.clear()
                facilities.addAll(fetchedFacilities)
            }

            Result.success(facilities)
        } catch (e: Exception) {
            // Return failure on real network/permission exceptions without overwriting local cache
            Result.failure(e)
        }
    }

    fun getAllFacilities(): List<FacilityData> {
        return facilities
    }

    fun getApprovedFacilities(): List<FacilityData> {
        return facilities.filter { it.status == "Approved" }
    }

    fun getApprovedCarWashFacilities(): List<CarWashFacility> {
        return getApprovedFacilities().map { it.toCarWashFacility() }
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
            selectedFacilityId = selected.id
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

    fun saveFacility(item: FacilityItemData, ownerEmail: String = "owner@gmail.com") {
        val existingIndex = facilities.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            val current = facilities[existingIndex]
            facilities[existingIndex] = current.copy(
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
            selectedFacilityId = newFacility.id
        }
    }

    fun updateBaysForFacility(facilityId: String, bays: List<BayConfig>) {
        val existingIndex = facilities.indexOfFirst { it.id == facilityId }
        if (existingIndex >= 0) {
            val current = facilities[existingIndex]
            facilities[existingIndex] = current.copy(
                bays = bays,
                bayCount = bays.size
            )
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
