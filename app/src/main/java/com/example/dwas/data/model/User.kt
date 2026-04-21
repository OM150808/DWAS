package com.example.dwas.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val supervisorId: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"
}