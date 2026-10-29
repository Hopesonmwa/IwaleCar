package com.example.myapplication.data

data class CarWashFacility(
    val id: String,
    val name: String,
    val area: String,
    val location: String,
    val price: String,
    val duration: String,
    val badgeText: String,
    val badgeType: BadgeType,
    val imageUrl: String,
    val isPrimaryAction: Boolean = true
)

enum class BadgeType {
    HIGH_PRESSURE_JET,
    SUPER_FOAM
}

data class AreaFilter(
    val id: String,
    val name: String,
    val hasIcon: Boolean = false
)
