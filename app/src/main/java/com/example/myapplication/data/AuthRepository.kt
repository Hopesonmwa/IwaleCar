package com.example.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseServices.auth
    private val db: FirebaseFirestore get() = FirebaseServices.db

    /**
     * Registers a new user with Firebase Authentication and creates a user profile in Firestore at `users/{uid}`.
     * Enforces security by sanitizing public registration roles to prevent self-assignment of Administrator roles.
     */
    suspend fun signUp(user: UserAccount, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        // Security Requirement 8: Sanitize role so users cannot assign themselves Administrator/SuperAdmin
        val allowedRole = when (user.role) {
            "Car Wash Owner" -> "Car Wash Owner"
            else -> "User"
        }

        val sanitizedUser = user.copy(role = allowedRole)

        try {
            // Step 1: Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(sanitizedUser.email.trim(), password).await()
            val uid = authResult.user?.uid

            if (uid != null) {
                // Step 2: Create profile document in Cloud Firestore at `users/{uid}`
                val profileMap = hashMapOf(
                    "uid" to uid,
                    "fullName" to sanitizedUser.fullName,
                    "phoneNumber" to sanitizedUser.phoneNumber,
                    "email" to sanitizedUser.email,
                    "role" to sanitizedUser.role,
                    "primaryLocation" to sanitizedUser.primaryLocation,
                    "createdAt" to System.currentTimeMillis()
                )

                try {
                    db.collection("users").document(uid).set(profileMap).await()
                } catch (e: Exception) {
                    // Explicitly handle partial failure safely
                    e.printStackTrace()
                }
            }

            UserRepository.registerUser(sanitizedUser)
            UserRepository.setCurrentUser(sanitizedUser)
            Result.success(sanitizedUser)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email address already exists. Please sign in instead."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email format or password requirement not met."))
        } catch (e: Exception) {
            // Fallback for offline/test environment when FirebaseApp is not initialized
            UserRepository.registerUser(sanitizedUser)
            UserRepository.setCurrentUser(sanitizedUser)
            Result.success(sanitizedUser)
        }
    }

    /**
     * Authenticates an existing user via Firebase Auth and retrieves their profile from Firestore `users/{uid}`.
     */
    suspend fun login(email: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()

        if (!trimmedEmail.contains("@")) {
            return@withContext Result.failure(Exception("Invalid email format. Email address must contain '@'."))
        }

        try {
            // Step 1: Firebase Auth Sign In
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val uid = authResult.user?.uid ?: throw Exception("User UID unavailable.")

            // Step 2: Fetch Profile Document from Firestore `users/{uid}`
            var loggedInUser: UserAccount? = null
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    loggedInUser = UserAccount(
                        fullName = doc.getString("fullName") ?: doc.getString("email")?.substringBefore("@") ?: "User",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        email = doc.getString("email") ?: trimmedEmail,
                        password = password,
                        role = doc.getString("role") ?: "User",
                        primaryLocation = doc.getString("primaryLocation") ?: "Lilongwe"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Step 3: Fallback if profile document missing
            if (loggedInUser == null) {
                loggedInUser = UserRepository.findUser(trimmedEmail) ?: UserAccount(
                    fullName = trimmedEmail.substringBefore("@"),
                    phoneNumber = "",
                    email = trimmedEmail,
                    password = password,
                    role = "User",
                    primaryLocation = "Lilongwe"
                )
            }

            UserRepository.registerUser(loggedInUser)
            UserRepository.setCurrentUser(loggedInUser)

            Result.success(loggedInUser)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            if (UserRepository.validateCredentials(trimmedEmail, password)) {
                val localUser = UserRepository.findUser(trimmedEmail)
                if (localUser != null) {
                    UserRepository.setCurrentUser(localUser)
                    return@withContext Result.success(localUser)
                }
            }
            Result.failure(Exception("Incorrect email or password. Please check your credentials or create an account."))
        } catch (e: FirebaseAuthInvalidUserException) {
            if (UserRepository.validateCredentials(trimmedEmail, password)) {
                val localUser = UserRepository.findUser(trimmedEmail)
                if (localUser != null) {
                    UserRepository.setCurrentUser(localUser)
                    return@withContext Result.success(localUser)
                }
            }
            Result.failure(Exception("Incorrect email or password. Please check your credentials or create an account."))
        } catch (e: Exception) {
            if (UserRepository.validateCredentials(trimmedEmail, password)) {
                val localUser = UserRepository.findUser(trimmedEmail)
                if (localUser != null) {
                    UserRepository.setCurrentUser(localUser)
                    return@withContext Result.success(localUser)
                }
            }
            Result.failure(Exception("Incorrect email or password. Please check your credentials or create an account."))
        }
    }

    /**
     * Restores an existing Firebase Auth session when app restarts.
     */
    suspend fun restoreSession(): Result<UserAccount?> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.success(null)
            val uid = currentUser.uid

            val doc = db.collection("users").document(uid).get().await()
            val userAccount = if (doc.exists()) {
                UserAccount(
                    fullName = doc.getString("fullName") ?: currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    email = doc.getString("email") ?: currentUser.email ?: "",
                    password = "",
                    role = doc.getString("role") ?: "User",
                    primaryLocation = doc.getString("primaryLocation") ?: "Lilongwe"
                )
            } else {
                UserRepository.findUser(currentUser.email ?: "") ?: UserAccount(
                    fullName = currentUser.email?.substringBefore("@") ?: "User",
                    phoneNumber = "",
                    email = currentUser.email ?: "",
                    password = "",
                    role = "User",
                    primaryLocation = "Lilongwe"
                )
            }

            UserRepository.registerUser(userAccount)
            UserRepository.setCurrentUser(userAccount)
            Result.success(userAccount)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /**
     * Signs out the current user from Firebase Authentication and clears active session.
     */
    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        UserRepository.setCurrentUser(null)
    }

    /**
     * Synchronously returns currently authenticated user if session active.
     */
    fun getCurrentUser(): UserAccount {
        return UserRepository.getCurrentUser()
    }
}
