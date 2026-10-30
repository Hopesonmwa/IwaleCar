package com.example.myapplication.data

import com.google.firebase.firestore.FirebaseFirestore

object UserProfileRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        primaryLocation: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        val userProfile = hashMapOf(
            "fullName" to fullName,
            "email" to email.trim(),
            "phoneNumber" to phoneNumber,
            "role" to "Customer",
            "primaryLocation" to primaryLocation
        )

        db.collection("users")
            .document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    false,
                    exception.localizedMessage
                        ?: "Failed to save user profile."
                )
            }
    }
}