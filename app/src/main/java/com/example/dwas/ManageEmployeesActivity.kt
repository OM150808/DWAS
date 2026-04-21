package com.example.dwas

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dwas.databinding.ActivityManageEmployeesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageEmployeesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageEmployeesBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: EmployeeAdapter
    private val employeeList = mutableListOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadEmployees()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnAddMember.setOnClickListener {
            val email = binding.etEmployeeEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                addEmployeeByEmail(email)
            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter(employeeList) { employee ->
            removeEmployee(employee)
        }
        binding.rvEmployees.layoutManager = LinearLayoutManager(this)
        binding.rvEmployees.adapter = adapter
    }

    private fun loadEmployees() {
        val supervisorId = auth.currentUser?.uid ?: return
        db.collection("users")
            .whereEqualTo("supervisorId", supervisorId)
            .whereEqualTo("role", "employee")
            .get()
            .addOnSuccessListener { documents ->
                employeeList.clear()
                for (doc in documents) {
                    val emp = doc.toObject(Employee::class.java).copy(uid = doc.id)
                    employeeList.add(emp)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun addEmployeeByEmail(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("role", "employee")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val supervisorId = auth.currentUser?.uid ?: ""
                    
                    db.collection("users").document(doc.id)
                        .update("supervisorId", supervisorId)
                        .addOnSuccessListener {
                            val newEmp = doc.toObject(Employee::class.java)!!.copy(uid = doc.id)
                            adapter.addEmployee(newEmp)
                            binding.etEmployeeEmail.text?.clear()
                            Toast.makeText(this, "Employee added successfully", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Employee not found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun removeEmployee(employee: Employee) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${employee.fullName} from your team?")
            .setPositiveButton("Remove") { _, _ ->
                db.collection("users").document(employee.uid)
                    .update("supervisorId", "")
                    .addOnSuccessListener {
                        adapter.removeEmployee(employee)
                        Toast.makeText(this, "Employee removed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}