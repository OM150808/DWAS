package com.example.dwas.data.repository

import com.example.dwas.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun signUp(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    suspend fun saveUser(user: User) =
        db.collection("users").document(user.uid).set(user).await()

    suspend fun getUser(uid: String) =
        db.collection("users").document(uid).get().await().toObject(User::class.java)

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser
}