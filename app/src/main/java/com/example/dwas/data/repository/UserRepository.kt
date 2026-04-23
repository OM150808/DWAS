package com.example.dwas.data.repository

import com.example.dwas.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = db.collection("users")

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getUserProfileRealtime(uid: String): Flow<Result<User?>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(Result.success(user))
                } else {
                    trySend(Result.success(null))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun updateProfile(user: User): Result<Unit> = try {
        usersCollection.document(user.uid).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        auth.signOut()
    }
}