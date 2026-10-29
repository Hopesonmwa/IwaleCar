package com.example.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseServices {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
}