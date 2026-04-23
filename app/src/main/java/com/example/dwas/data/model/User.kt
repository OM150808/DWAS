package com.example.dwas.data.model

import com.google.firebase.firestore.PropertyName

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
    val supervisorId: String? = null,
    
    // New fields for Supervisor Profile
    val employeeId: String = "",
    val department: String = "",
    val teamSize: Int = 0,
    val project: String = "",
    val shift: String = "",
    val location: String = "",
    val joiningDate: String = "",
    val status: String = "Active",
    val profileImageUrl: String? = null
) {
    @get:PropertyName("fullName")
    val fullName: String
        get() = "$firstName $lastName"
}