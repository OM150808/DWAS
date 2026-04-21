package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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
        val ageAutoComplete = findViewById<AutoCompleteTextView>(R.id.ageAutoComplete)
        val genderAutoComplete = findViewById<AutoCompleteTextView>(R.id.genderAutoComplete)
        val countryCodeAutoComplete = findViewById<AutoCompleteTextView>(R.id.countryCodeAutoComplete)
        val phoneEditText = findViewById<EditText>(R.id.phoneEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val employeeRadio = findViewById<RadioButton>(R.id.employeeRadio)
        val supervisorIdEditText = findViewById<EditText>(R.id.supervisorIdEditText)
        val signupButton = findViewById<MaterialButton>(R.id.signupButton)
        val roleRadioGroup = findViewById<RadioGroup>(R.id.roleRadioGroup)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        // Populate Gender Dropdown
        val genders = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        genderAutoComplete.setAdapter(genderAdapter)

        // Populate Age Dropdown (20 to 80)
        val ages = (20..80).map { it.toString() }.toTypedArray()
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ages)
        ageAutoComplete.setAdapter(ageAdapter)

        // Populate Country Codes
        val countryCodes = arrayOf("+91 (India)", "+1 (USA)", "+44 (UK)", "+971 (UAE)", "+61 (Australia)")
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countryCodes)
        countryCodeAutoComplete.setAdapter(countryAdapter)

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
            val ageStr = ageAutoComplete.text.toString().trim()
            val gender = genderAutoComplete.text.toString().trim()
            val countryCodeRaw = countryCodeAutoComplete.text.toString()
            val countryCode = if (countryCodeRaw.contains(" ")) countryCodeRaw.split(" ")[0] else countryCodeRaw
            val phone = countryCode + phoneEditText.text.toString().trim()
            val supervisorId = supervisorIdEditText.text.toString().trim()
            val isEmployee = employeeRadio.isChecked
            val role = if (isEmployee) "employee" else "supervisor"

            // Validation logic
            if (firstNameRaw.isEmpty() || lastNameRaw.isEmpty() || email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                Toast.makeText(this, "Password requires: 6+ chars, 2+ digits, 1+ special char", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (isEmployee && supervisorId.isEmpty()) {
                Toast.makeText(this, "Supervisor ID is required for Employees", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signupButton.isEnabled = false

            // Convert names to Sentence Case
            val firstName = firstNameRaw.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
            val lastName = lastNameRaw.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }

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
        if (password.length < 6) return false
        val digits = password.count { it.isDigit() }
        val special = password.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        return digits >= 2 && special >= 1
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