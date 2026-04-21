package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployeeDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDashboard()

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        findViewById<View>(R.id.btnProfile).setOnClickListener {
            toggleProfileSection()
        }

        findViewById<Button>(R.id.btnCheckIn).setOnClickListener {
            // TODO: Implement Check-in logic
            Toast.makeText(this, "Checking In...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnViewTasks).setOnClickListener {
            // TODO: Navigate to Tasks activity
            Toast.makeText(this, "Opening Tasks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDashboard() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Fetch User Data for Welcome Message
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: "Employee"
                findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $firstName"
            }

        // Placeholder stats (In a real app, fetch these from Firestore)
        findViewById<TextView>(R.id.tvHoursToday).text = "0.0"
        findViewById<TextView>(R.id.tvTotalTasks).text = "0"
        findViewById<TextView>(R.id.tvPerformance).text = "N/A"
    }

    private fun toggleProfileSection() {
        val profileSection = findViewById<LinearLayout>(R.id.layoutProfileDetails)
        if (profileSection.visibility == View.VISIBLE) {
            profileSection.visibility = View.GONE
        } else {
            profileSection.visibility = View.VISIBLE
            fetchUserProfile()
        }
    }

    private fun fetchUserProfile() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: "--"
                    val lastName = document.getString("lastName") ?: "--"
                    val email = document.getString("email") ?: "--"
                    val age = document.getLong("age")?.toString() ?: "--"
                    val gender = document.getString("gender") ?: "--"
                    val phone = document.getString("phoneNumber") ?: "--"

                    findViewById<TextView>(R.id.tvProfileName).text = "Name: $firstName $lastName"
                    findViewById<TextView>(R.id.tvProfileEmail).text = "Email: $email"
                    findViewById<TextView>(R.id.tvProfileAge).text = "Age: $age"
                    findViewById<TextView>(R.id.tvProfileGender).text = "Gender: $gender"
                    findViewById<TextView>(R.id.tvProfilePhone).text = "Phone: $phone"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }
}