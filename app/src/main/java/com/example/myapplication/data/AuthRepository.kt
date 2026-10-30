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
    suspend fun signUp(
        user: UserAccount,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {


        // Public registrations are always assigned the Customer role.
        val sanitizedUser = user.copy(role = "Customer")

        try {
            // 2. Create the Firebase Authentication account
            val authResult = auth.createUserWithEmailAndPassword(
                sanitizedUser.email.trim(),
                password
            ).await()

            val firebaseUser = authResult.user
                ?: throw Exception("Could not retrieve Firebase user.")

            val uid = firebaseUser.uid

            // 3. Prepare the user's Firestore profile
            val profileMap = hashMapOf(
                "uid" to uid,
                "fullName" to sanitizedUser.fullName,
                "phoneNumber" to sanitizedUser.phoneNumber,
                "email" to sanitizedUser.email.trim(),
                "role" to sanitizedUser.role,
                "primaryLocation" to sanitizedUser.primaryLocation,
                "createdAt" to System.currentTimeMillis()
            )

            // 4. Save the profile in Firestore
            try {
                db.collection("users")
                    .document(uid)
                    .set(profileMap)
                    .await()

            } catch (firestoreException: Exception) {

                // 5. Attempt to remove the incomplete Auth account
                try {
                    firebaseUser.delete().await()
                } catch (cleanupException: Exception) {
                    cleanupException.printStackTrace()
                }

                return@withContext Result.failure(
                    Exception(
                        "Your account could not be fully created. " +
                                "Please try again. " +
                                (firestoreException.localizedMessage ?: "")
                    )
                )
            }

            // 6. Update the existing local application session
            UserRepository.registerUser(sanitizedUser)
            UserRepository.setCurrentUser(sanitizedUser)

            // 7. Report successful registration
            Result.success(sanitizedUser)

        } catch (e: FirebaseAuthUserCollisionException) {

            Result.failure(
                Exception("An account with this email already exists. Please sign in instead.")
            )

        } catch (e: FirebaseAuthInvalidCredentialsException) {

            Result.failure(
                Exception("Invalid email format or password requirement not met.")
            )

        } catch (e: Exception) {

            Result.failure(
                Exception(
                    e.localizedMessage ?: "Registration failed. Please try again."
                )
            )
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

        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(trimmedEmail).matches()
        ) {
            return@withContext Result.failure(
                Exception("Please enter a valid email address.")
            )
        }

        if (password.isBlank()) {
            return@withContext Result.failure(
                Exception("Please enter your password.")
            )
        }

        try {
            // 1. Authenticate using Firebase Authentication.
            val authResult = auth.signInWithEmailAndPassword(
                trimmedEmail,
                password
            ).await()

            val firebaseUser = authResult.user
                ?: throw Exception("Could not retrieve your account.")

            val uid = firebaseUser.uid

            // 2. Retrieve the user's profile from Firestore.
            val doc = db.collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                throw Exception(
                    "Your account profile could not be found. Please contact support."
                )
            }

            // 3. Build the application user model.
            val role = doc.getString("role")
                ?: throw Exception("Your account role is missing.")

            val loggedInUser = UserAccount(
                fullName = doc.getString("fullName")
                    ?: "User",
                phoneNumber = doc.getString("phoneNumber")
                    ?: "",
                email = doc.getString("email")
                    ?: trimmedEmail,
                password = "",
                role = role,
                primaryLocation = doc.getString("primaryLocation")
                    ?: "Lilongwe"
            )

            // 4. Update the temporary local session cache.
            UserRepository.registerUser(loggedInUser)
            UserRepository.setCurrentUser(loggedInUser)

            Result.success(loggedInUser)

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(
                Exception("Incorrect email or password. Please try again.")
            )

        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(
                Exception("No account was found for this email address.")
            )

        } catch (e: Exception) {
            Result.failure(
                Exception(
                    e.localizedMessage
                        ?: "Login failed. Please check your connection and try again."
                )
            )
        }
    }

    /**
     * Restores an existing Firebase Auth session when app restarts.
     */

    suspend fun restoreSession(): Result<UserAccount?> =
        withContext(Dispatchers.IO) {

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

                if (!doc.exists()) {
                    return@withContext Result.failure(
                        Exception("Your account profile could not be found.")
                    )
                }

                val role = doc.getString("role")
                    ?: throw Exception("Your account role is missing.")

                val userAccount = UserAccount(
                    fullName = doc.getString("fullName")
                        ?: "User",
                    phoneNumber = doc.getString("phoneNumber")
                        ?: "",
                    email = doc.getString("email")
                        ?: firebaseUser.email
                        ?: "",
                    password = "",
                    role = role,
                    primaryLocation = doc.getString("primaryLocation")
                        ?: "Lilongwe"
                )

                UserRepository.registerUser(userAccount)
                UserRepository.setCurrentUser(userAccount)

                Result.success(userAccount)

            } catch (e: Exception) {
                Result.failure(
                    Exception(
                        e.localizedMessage
                            ?: "Unable to restore your session."
                    )
                )
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
    suspend fun resetPassword(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {

            val trimmedEmail = email.trim()

            if (trimmedEmail.isBlank() ||
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(trimmedEmail).matches()
            ) {
                return@withContext Result.failure(
                    Exception("Please enter a valid email address.")
                )
            }

            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()

                Result.success(Unit)

            } catch (e: Exception) {
                Result.failure(
                    Exception(
                        e.localizedMessage
                            ?: "Unable to send password reset email."
                    )
                )
            }
        }
   }
