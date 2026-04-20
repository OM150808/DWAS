package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI References
        val firstNameEditText = findViewById<EditText>(R.id.firstNameEditText)
        val lastNameEditText = findViewById<EditText>(R.id.lastNameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val employeeRadio = findViewById<RadioButton>(R.id.employeeRadio)
        val supervisorRadio = findViewById<RadioButton>(R.id.supervisorRadio)
        val supervisorIdEditText = findViewById<EditText>(R.id.supervisorIdEditText)
        val signupButton = findViewById<Button>(R.id.signupButton)
        val roleRadioGroup = findViewById<RadioGroup>(R.id.roleRadioGroup)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        // 3. SUPERVISOR ID LOGIC - Visibility based on selection
        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.employeeRadio) {
                supervisorIdEditText.visibility = View.VISIBLE
            } else {
                supervisorIdEditText.visibility = View.GONE
            }
        }

        // 1. AUTHENTICATION SYSTEM - Signup logic
        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val supervisorId = supervisorIdEditText.text.toString().trim()
            val isEmployee = employeeRadio.isChecked
            val role = if (isEmployee) "employee" else "supervisor"

            // 5. VALIDATIONS
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isEmployee && supervisorId.isEmpty()) {
                Toast.makeText(this, "Supervisor ID is required for Employees", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            // 4. DATA STORAGE - Firestore
                            saveUserToFirestore(uid, email, role, supervisorId, firstName, lastName)
                        }
                    } else {
                        // 8. ERROR HANDLING
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun saveUserToFirestore(uid: String, email: String, role: String, supervisorId: String, first: String, last: String) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "firstName" to first,
            "lastName" to last
        )
        if (role == "employee") {
            userMap["supervisorId"] = supervisorId
        }

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                // 6. NAVIGATION - Redirect after signup
                redirectBasedOnRole(role)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save user details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectBasedOnRole(role: String) {
        val intent = if (role == "supervisor") {
            Intent(this, SupervisorDashboardActivity::class.java)
        } else {
            Intent(this, EmployeeDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}