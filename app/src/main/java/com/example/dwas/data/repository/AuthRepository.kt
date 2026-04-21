package com.example.dwas.data.repository

import com.example.dwas.data.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun signInWithCredential(credential: AuthCredential) =
        auth.signInWithCredential(credential).await()

    suspend fun signUp(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    suspend fun saveUser(user: User) =
        db.collection("users").document(user.uid).set(user).await()

    suspend fun getUser(uid: String) =
        db.collection("users").document(uid).get().await().toObject(User::class.java)

    suspend fun getUserByEmail(email: String): User? =
        db.collection("users").whereEqualTo("email", email).get().await()
            .toObjects(User::class.java).firstOrNull()

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser

    suspend fun sendPasswordResetEmail(email: String) =
        auth.sendPasswordResetEmail(email).await()

    suspend fun getSupervisorEmail(supervisorId: String): String? {
        val supervisor = db.collection("users").document(supervisorId).get().await().toObject(User::class.java)
        return supervisor?.email
    }
}