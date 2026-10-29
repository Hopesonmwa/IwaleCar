package com.example.myapplication.data

object UserRepository {
    private val initialAccounts = listOf(
        UserAccount(
            fullName = "Default User",
            phoneNumber = "992929988",
            email = "user@gmail.com",
            password = "1234",
            role = "User",
            primaryLocation = "Lilongwe"
        ),
        UserAccount(
            fullName = "Chisomo Gondwe",
            phoneNumber = "999123456",
            email = "owner@gmail.com",
            password = "owner@123",
            role = "Car Wash Owner",
            primaryLocation = "Lilongwe"
        ),
        UserAccount(
            fullName = "Car Wash Owner",
            phoneNumber = "999000111",
            email = "carwash@gmail.com",
            password = "1234",
            role = "Car Wash Owner",
            primaryLocation = "Lilongwe"
        ),
        UserAccount(
            fullName = "Chisomo Banda",
            phoneNumber = "999234567",
            email = "admin@gmail.com",
            password = "1234",
            role = "Administrator",
            primaryLocation = "Lilongwe HQ"
        ),
        UserAccount(
            fullName = "Chisomo Banda",
            phoneNumber = "999234567",
            email = "chisomo.banda@iwalecar.mw",
            password = "1234",
            role = "Administrator",
            primaryLocation = "Lilongwe HQ"
        )
    )

    private val accounts = mutableListOf<UserAccount>().apply { addAll(initialAccounts) }
    private var currentUser: UserAccount? = null

    fun resetToDefaultData() {
        accounts.clear()
        accounts.addAll(initialAccounts)
        currentUser = null
    }

    fun setCurrentUser(user: UserAccount?) {
        currentUser = user
    }

    fun getCurrentUser(): UserAccount {
        return currentUser ?: accounts.first { it.role == "Car Wash Owner" }
    }

    fun registerUser(user: UserAccount) {
        val existingIndex = accounts.indexOfFirst { it.email.equals(user.email, ignoreCase = true) }
        if (existingIndex >= 0) {
            accounts[existingIndex] = user
        } else {
            accounts.add(user)
        }
    }

    fun validateCredentials(email: String, password: String): Boolean {
        return accounts.any {
            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
        }
    }

    fun findUser(email: String): UserAccount? {
        return accounts.find { it.email.equals(email.trim(), ignoreCase = true) }
    }
}
