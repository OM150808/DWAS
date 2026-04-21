package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val firstNameEditText = findViewById<EditText>(R.id.firstNameEditText)
        val lastNameEditText = findViewById<EditText>(R.id.lastNameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val ageEditText = findViewById<EditText>(R.id.ageEditText)
        val genderEditText = findViewById<EditText>(R.id.genderEditText)
        val phoneEditText = findViewById<EditText>(R.id.phoneEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val employeeRadio = findViewById<RadioButton>(R.id.employeeRadio)
        val supervisorIdEditText = findViewById<EditText>(R.id.supervisorIdEditText)
        val signupButton = findViewById<MaterialButton>(R.id.signupButton)
        val roleRadioGroup = findViewById<RadioGroup>(R.id.roleRadioGroup)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.employeeRadio) {
                supervisorIdEditText.visibility = View.VISIBLE
            } else {
                supervisorIdEditText.visibility = View.GONE
            }
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val firstNameRaw = firstNameEditText.text.toString().trim()
            val lastNameRaw = lastNameEditText.text.toString().trim()
            val ageStr = ageEditText.text.toString().trim()
            val gender = genderEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val supervisorId = supervisorIdEditText.text.toString().trim()
            val isEmployee = employeeRadio.isChecked
            val role = if (isEmployee) "employee" else "supervisor"

            // Convert names to Sentence Case
            val firstName = firstNameRaw.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            val lastName = lastNameRaw.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            // Validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Specific Password Requirement Validation
            if (!isPasswordValid(password)) {
                Toast.makeText(this, "Password must contain at least 6 letters, 2 numbers, and a special character", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (isEmployee && supervisorId.isEmpty()) {
                Toast.makeText(this, "Supervisor ID is required for Employees", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double-click
            signupButton.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            saveUserToFirestore(uid, email, role, supervisorId, firstName, lastName, ageStr.toIntOrNull() ?: 0, gender, phone)
                        }
                    } else {
                        signupButton.isEnabled = true
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val letterCount = password.count { it.isLetter() }
        val digitCount = password.count { it.isDigit() }
        val specialCount = password.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        return letterCount >= 6 && digitCount >= 2 && specialCount >= 1
    }

    private fun saveUserToFirestore(uid: String, email: String, role: String, supervisorId: String, first: String, last: String, age: Int, gender: String, phone: String) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "firstName" to first,
            "lastName" to last,
            "age" to age,
            "gender" to gender,
            "phoneNumber" to phone
        )
        if (role == "employee") {
            userMap["supervisorId"] = supervisorId
        }

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                redirectBasedOnRole(role)
            }
            .addOnFailureListener {
                findViewById<View>(R.id.signupButton)?.isEnabled = true
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