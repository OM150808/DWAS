package com.example.dwas

data class Employee(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "",
    val supervisorId: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName"
}