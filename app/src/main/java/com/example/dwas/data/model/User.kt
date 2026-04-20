package com.example.dwas.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val supervisorId: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"
}