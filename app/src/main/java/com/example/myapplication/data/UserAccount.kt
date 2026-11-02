package com.example.myapplication.data

data class UserAccount(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val role: String,
    val primaryLocation: String,
    val requestedRole: String = role,
    val firstName: String = fullName.substringBefore(" ").ifBlank { fullName },
    val lastName: String = fullName.substringAfter(" ", "").ifBlank { "" },
    val bio: String = ""
)

val UserAccount.greetingName: String
    get() {
        if (email.isNotBlank() && email.contains("@")) {
            return email.substringBefore("@")
        }
        if (firstName.isNotBlank()) {
            return firstName
        }
        return fullName.split(" ").firstOrNull()?.ifBlank { null } ?: fullName
    }
