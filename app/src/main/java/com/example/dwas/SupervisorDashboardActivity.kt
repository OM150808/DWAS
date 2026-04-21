package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SupervisorDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDashboard()

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.btnProfile).setOnClickListener {
            val profileLayout = findViewById<android.view.View>(R.id.layoutProfileDetails)
            if (profileLayout.visibility == android.view.View.VISIBLE) {
                profileLayout.visibility = android.view.View.GONE
            } else {
                profileLayout.visibility = android.view.View.VISIBLE
                fetchProfileData()
            }
        }

        findViewById<android.view.View>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewEmployees).setOnClickListener {
            startActivity(Intent(this, ManageEmployeesActivity::class.java))
        }

        findViewById<Button>(R.id.btnLaunchCamera).setOnClickListener {
            // TODO: Implement GPS Camera logic
            Toast.makeText(this, "Launching GPS Camera...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnReviewApprovals).setOnClickListener {
            // TODO: Navigate to Approve Time Entries activity
            Toast.makeText(this, "Opening Review Approvals", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDashboard() {
        val currentSupervisorId = auth.currentUser?.uid ?: return

        // Fetch User Data for Welcome Message
        db.collection("users").document(currentSupervisorId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "Supervisor"
                findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $name"
            }

        // Fetch Total Employees
        db.collection("users")
            .whereEqualTo("role", "employee")
            .whereEqualTo("supervisorId", currentSupervisorId)
            .get()
            .addOnSuccessListener { documents ->
                findViewById<TextView>(R.id.tvTotalEmployees).text = documents.size().toString()
            }
        
        // Note: Pending Approvals and Approved Today counts would typically come from a 'time_entries' collection
        // For now, these are placeholder values in the UI (8 and 16)
    }

    private fun fetchProfileData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
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
    }
}