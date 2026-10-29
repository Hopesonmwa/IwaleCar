package com.example.myapplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthRepository {

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()

    fun register(
        email: String,
        password: String,
        onResult: (FirebaseUser?, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {
                onResult(auth.currentUser, null)
            } else {
                onResult(
                    null,
                    task.exception?.localizedMessage
                        ?: "Registration failed."
                )
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onResult: (FirebaseUser?, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {
                onResult(auth.currentUser, null)
            } else {
                onResult(
                    null,
                    task.exception?.localizedMessage
                        ?: "Login failed."
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUser(): FirebaseUser? {
        return auth.currentUser
    }
}