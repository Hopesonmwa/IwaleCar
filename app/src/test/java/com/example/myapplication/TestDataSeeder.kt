package com.example.myapplication

import com.example.myapplication.data.BookingRecord
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityData
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.BayConfig

object TestDataSeeder {

    fun seedAll() {
        UserRepository.resetToDefaultData()
        FacilityRepository.resetToDefaultData()
        BookingRepository.resetToDefaultData()

        // Seeded Accounts
        UserRepository.registerUser(
            UserAccount(
                fullName = "Test Customer",
                phoneNumber = "991234567",
                email = "customer.test@gmail.com",
                password = "Wash@2026",
                role = "User",
                primaryLocation = "Lilongwe"
            )
        )

        UserRepository.registerUser(
            UserAccount(
                fullName = "Test Owner",
                phoneNumber = "998888777",
                email = "owner.test@gmail.com",
                password = "Owner@2026",
                role = "Car Wash Owner",
                primaryLocation = "Lilongwe"
            )
        )

        UserRepository.registerUser(
            UserAccount(
                fullName = "Test Admin",
                phoneNumber = "997777666",
                email = "admin.test@gmail.com",
                password = "Admin@2026",
                role = "Administrator",
                primaryLocation = "Lilongwe HQ"
            )
        )

        UserRepository.registerUser(
            UserAccount(
                fullName = "Owner A",
                phoneNumber = "991111222",
                email = "owner.a@gmail.com",
                password = "OwnerA@2026",
                role = "Car Wash Owner",
                primaryLocation = "Lilongwe"
            )
        )

        UserRepository.registerUser(
            UserAccount(
                fullName = "Owner B",
                phoneNumber = "993333444",
                email = "owner.b@gmail.com",
                password = "OwnerB@2026",
                role = "Car Wash Owner",
                primaryLocation = "Blantyre"
            )
        )

        UserRepository.registerUser(
            UserAccount(
                fullName = "New Owner",
                phoneNumber = "995555666",
                email = "owner.new@gmail.com",
                password = "OwnerNew@2026",
                role = "Car Wash Owner",
                primaryLocation = "Zomba"
            )
        )

        // Seeded Facilities
        val cityShineBays = listOf(
            BayConfig(
                id = "cs_1",
                bayNumber = "BAY 01",
                serviceTitle = "Standard Wash",
                priceText = "MWK 5,000",
                priceValue = 5000,
                durationText = "30 mins",
                featuresDescription = "High-pressure wash & foam",
                status = "Available"
            ),
            BayConfig(
                id = "cs_2",
                bayNumber = "BAY 02",
                serviceTitle = "Deep Clean",
                priceText = "MWK 10,000",
                priceValue = 10000,
                durationText = "40 mins",
                featuresDescription = "Full exterior wax & vacuum",
                status = "Available"
            )
        )

        val cityShine = FacilityData(
            id = "cs_facility_1",
            name = "City Shine",
            area = "Lilongwe",
            address = "Plot 12, Area 3, Lilongwe",
            city = "Lilongwe",
            phone = "991234567",
            bayCount = 2,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "Express washing bays in Lilongwe",
            status = "Approved",
            ownerId = "owner.test@gmail.com",
            bays = cityShineBays
        )

        val pendingWash = FacilityData(
            id = "pending_wash_1",
            name = "Pending Wash",
            area = "Lilongwe",
            address = "Area 25 Industrial",
            city = "Lilongwe",
            phone = "998888777",
            bayCount = 1,
            openingTime = "07:00 AM",
            closingTime = "06:00 PM",
            description = "Awaiting drainage clearance certificate",
            status = "Pending Approval",
            ownerId = "owner.a@gmail.com",
            bays = emptyList()
        )

        val blantyreSpa = FacilityData(
            id = "blantyre_spa_1",
            name = "Blantyre Auto Spa",
            area = "Blantyre",
            address = "Masauko Chipembere Hwy, Blantyre",
            city = "Blantyre",
            phone = "888654321",
            bayCount = 2,
            openingTime = "07:00 AM",
            closingTime = "07:30 PM",
            description = "Automated Foam Bays • Blantyre Express",
            status = "Approved",
            ownerId = "owner.b@gmail.com",
            bays = cityShineBays
        )

        val rejectedWash = FacilityData(
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
            ownerId = "owner.a@gmail.com",
            bays = emptyList()
        )

        FacilityRepository.saveFacilityData(cityShine)
        FacilityRepository.saveFacilityData(pendingWash)
        FacilityRepository.saveFacilityData(blantyreSpa)
        FacilityRepository.saveFacilityData(rejectedWash)

        // Seeded Booking Record for Owner Test
        BookingRepository.addBookingRecord(
            BookingRecord(
                id = "BK-SEED-01",
                bookingReference = "IW-9999",
                facilityId = "cs_facility_1",
                bayId = "cs_1",
                ownerId = "owner.test@gmail.com",
                customerName = "Test Customer",
                customerEmail = "customer.test@gmail.com",
                customerPhone = "991234567",
                bayTitle = "BAY 01 — Standard Wash",
                status = "In Progress",
                timeSlotText = "14:00 – 14:30 CAT",
                slotDetailText = "Active Slot",
                amountValue = 5000,
                amountFormatted = "MWK 5,000",
                paymentMethod = "Airtel Money",
                paymentStatus = "SUCCESSFUL",
                startTimeMs = System.currentTimeMillis() - 10 * 60 * 1000L,
                endTimeMs = System.currentTimeMillis() + 20 * 60 * 1000L,
                timestampMs = System.currentTimeMillis()
            )
        )
    }
}
