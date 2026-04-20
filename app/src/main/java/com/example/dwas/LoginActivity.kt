package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 7. SESSION MANAGEMENT - Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.uid)
            return
        }

        setContentView(R.layout.activity_login)

        // Binding Views
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        // 1. AUTHENTICATION SYSTEM - Login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // 5. VALIDATIONS
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            checkUserRoleAndRedirect(uid)
                        }
                    } else {
                        // 8. ERROR HANDLING
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Navigation to Signup
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        // 6. NAVIGATION - Fetch role from Firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "supervisor") {
                        // Supervisor Dashboard
                        val intent = Intent(this, SupervisorDashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Employee Dashboard
                        val intent = Intent(this, EmployeeDashboardActivity::class.java)
                        startActivity(intent)
                    }
                    finish()
                } else {
                    Toast.makeText(this, "User data not found in database", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
    }
}