package com.example.myapplication.data

data class UserAccount(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val role: String,
    val primaryLocation: String,
    val requestedRole: String = role
)

val UserAccount.greetingName: String
    get() {
        if (email.isNotBlank() && email.contains("@")) {
            return email.substringBefore("@")
        }
        return fullName.split(" ").firstOrNull()?.ifBlank { null } ?: fullName
    }
