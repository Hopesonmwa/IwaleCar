package com.example.myapplication.data

object ValidationUtils {

    fun isPhoneValid(phone: String): Boolean {
        val trimmed = phone.trim()
        return trimmed.length == 9 && trimmed.all { it.isDigit() }
    }

    fun isEmailValid(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return trimmed.contains("@") && trimmed.indexOf("@") > 0 && trimmed.indexOf("@") < trimmed.length - 1
    }

    fun isEmailFormatValid(email: String): Boolean {
        return email.trim().contains("@")
    }

    fun isPasswordComplex(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() }
    }
}
