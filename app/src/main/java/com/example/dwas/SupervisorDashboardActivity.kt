package com.example.dwas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SupervisorDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: EmployeeAdapter
    private val employeeList = mutableListOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val rvEmployees = findViewById<RecyclerView>(R.id.rvEmployees)
        rvEmployees.layoutManager = LinearLayoutManager(this)
        adapter = EmployeeAdapter(employeeList)
        rvEmployees.adapter = adapter

        fetchEmployees()

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchEmployees() {
        val currentSupervisorId = auth.currentUser?.uid ?: return

        db.collection("users")
            .whereEqualTo("role", "employee")
            .whereEqualTo("supervisorId", currentSupervisorId)
            .get()
            .addOnSuccessListener { documents ->
                employeeList.clear()
                for (document in documents) {
                    val employee = document.toObject(Employee::class.java)
                    employeeList.add(employee)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching employees: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}