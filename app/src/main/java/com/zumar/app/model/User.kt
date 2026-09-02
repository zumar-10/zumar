package com.zumar.app.model

data class User(
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val phone: String,
    val email: String,
    val hashedPassword: String,
    val address: String,
    val state: String,
    val dob: String,
    val gender: String,
    val hashedPin: String,
    var balance: Double = 0.0,
    var transactions: List<Transaction> = emptyList(),
    var beneficiaries: List<Beneficiary> = emptyList(),
    var notifications: NotificationPrefs = NotificationPrefs()
)
