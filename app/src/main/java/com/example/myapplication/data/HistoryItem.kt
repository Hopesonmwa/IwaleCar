package com.example.myapplication.data

enum class WashStatus {
    COMPLETED,
    REFUNDED,
    ACTIVE
}

data class HistoryItem(
    val id: String,
    val stationName: String,
    val bayDetails: String,
    val dateText: String,
    val durationText: String,
    val paymentProvider: String,
    val amountFormatted: String,
    val status: WashStatus,
    val receiptRef: String = "IW-8924",
    val litersDispensed: String = "180 Liters"
)
