package com.example.myapplication.data

data class CarWashTier(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val formattedPrice: String,
    val isMostPopular: Boolean = false
)
