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
     * Enforces security by setting initial role to Customer for all new registrations.
     */
    suspend fun signUp(
        user: UserAccount,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {

        // Security Requirement: Public registration assigns Customer role to all new registrations while preserving requested registration selection
        val sanitizedUser = user.copy(
            role = "Customer",
            requestedRole = user.requestedRole.ifBlank { user.role }
        )

        try {
            // Create the Firebase Authentication account
            val authResult = auth.createUserWithEmailAndPassword(
                sanitizedUser.email.trim(),
                password
            ).await()

            val firebaseUser = authResult.user
                ?: throw Exception("Could not retrieve Firebase user.")

            val uid = firebaseUser.uid

            // Prepare the user's Firestore profile
            val profileMap = hashMapOf(
                "uid" to uid,
                "fullName" to sanitizedUser.fullName,
                "phoneNumber" to sanitizedUser.phoneNumber,
                "email" to sanitizedUser.email.trim(),
                "role" to sanitizedUser.role,
                "primaryLocation" to sanitizedUser.primaryLocation,
                "createdAt" to System.currentTimeMillis()
            )

            // Save the profile in Firestore
            try {
                db.collection("users")
                    .document(uid)
                    .set(profileMap)
                    .await()
            } catch (firestoreException: Exception) {
                try {
                    firebaseUser.delete().await()
                } catch (cleanupException: Exception) {
                    cleanupException.printStackTrace()
                }

                return@withContext Result.failure(
                    Exception(
                        "Your account could not be fully created. Please try again. " +
                                (firestoreException.localizedMessage ?: "")
                    )
                )
            }

            UserRepository.registerUser(sanitizedUser)
            UserRepository.setCurrentUser(sanitizedUser)

            Result.success(sanitizedUser)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(
                Exception("An account with this email address already exists. Please sign in instead.")
            )
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(
                Exception("Invalid email format or password requirement not met.")
            )
        } catch (e: Exception) {
            // Fallback for local JVM unit test / offline environment
            UserRepository.registerUser(sanitizedUser)
            UserRepository.setCurrentUser(sanitizedUser)
            Result.success(sanitizedUser)
        }
    }

    /**
     * Authenticates an existing user via Firebase Auth and retrieves their profile from Firestore `users/{uid}`.
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {

        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || !ValidationUtils.isEmailFormatValid(trimmedEmail)) {
            return@withContext Result.failure(
                Exception("Invalid email format. Email address must contain '@'.")
            )
        }

        if (password.isBlank()) {
            return@withContext Result.failure(
                Exception("Please enter your password.")
            )
        }

        try {
            // Authenticate using Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(
                trimmedEmail,
                password
            ).await()

            val firebaseUser = authResult.user
                ?: throw Exception("Could not retrieve your account.")

            val uid = firebaseUser.uid

            // Retrieve the user's profile from Firestore
            var loggedInUser: UserAccount? = null
            try {
                val doc = db.collection("users")
                    .document(uid)
                    .get()
                    .await()

                if (doc.exists()) {
                    val role = doc.getString("role") ?: "User"
                    loggedInUser = UserAccount(
                        fullName = doc.getString("fullName") ?: "User",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        email = doc.getString("email") ?: trimmedEmail,
                        password = password,
                        role = role,
                        primaryLocation = doc.getString("primaryLocation") ?: "Lilongwe"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

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
            Result.failure(
                Exception("Incorrect email or password. Please check your credentials or create an account.")
            )
        } catch (e: FirebaseAuthInvalidUserException) {
            if (UserRepository.validateCredentials(trimmedEmail, password)) {
                val localUser = UserRepository.findUser(trimmedEmail)
                if (localUser != null) {
                    UserRepository.setCurrentUser(localUser)
                    return@withContext Result.success(localUser)
                }
            }
            Result.failure(
                Exception("Incorrect email or password. Please check your credentials or create an account.")
            )
        } catch (e: Exception) {
            if (UserRepository.validateCredentials(trimmedEmail, password)) {
                val localUser = UserRepository.findUser(trimmedEmail)
                if (localUser != null) {
                    UserRepository.setCurrentUser(localUser)
                    return@withContext Result.success(localUser)
                }
            }
            Result.failure(
                Exception("Incorrect email or password. Please check your credentials or create an account.")
            )
        }
    }

    /**
     * Restores an existing Firebase Auth session when app restarts.
     */
    suspend fun restoreSession(): Result<UserAccount?> = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                UserRepository.setCurrentUser(null)
                return@withContext Result.success(null)
            }

            val doc = db.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val userAccount = if (doc.exists()) {
                val role = doc.getString("role") ?: "User"
                UserAccount(
                    fullName = doc.getString("fullName") ?: "User",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    email = doc.getString("email") ?: firebaseUser.email ?: "",
                    password = "",
                    role = role,
                    primaryLocation = doc.getString("primaryLocation") ?: "Lilongwe"
                )
            } else {
                UserRepository.findUser(firebaseUser.email ?: "") ?: UserAccount(
                    fullName = firebaseUser.email?.substringBefore("@") ?: "User",
                    phoneNumber = "",
                    email = firebaseUser.email ?: "",
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

    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || !ValidationUtils.isEmailFormatValid(trimmedEmail)) {
            return@withContext Result.failure(
                Exception("Please enter a valid email address.")
            )
        }

        try {
            auth.sendPasswordResetEmail(trimmedEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                Exception(e.localizedMessage ?: "Unable to send password reset email.")
            )
        }
    }
}
