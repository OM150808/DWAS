package com.example.dwas

import android.os.Bundle
import android.view.View
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
        setupListeners()
        loadEmployees()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Add Member Toggles
        binding.rgAddType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAddByEmail -> {
                    binding.tilAddMember.hint = "Enter Employee Email"
                    binding.tilAddMember.setStartIconDrawable(android.R.drawable.ic_dialog_email)
                    binding.etAddMember.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
                R.id.rbAddById -> {
                    binding.tilAddMember.hint = "Enter Employee ID"
                    binding.tilAddMember.setStartIconDrawable(R.drawable.ic_person)
                    binding.etAddMember.inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
            }
            binding.etAddMember.text?.clear()
        }

        // Remove Member Toggles
        binding.rgRemoveType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRemoveByEmail -> {
                    binding.tilRemoveMember.hint = "Enter Employee Email"
                    binding.tilRemoveMember.setStartIconDrawable(android.R.drawable.ic_dialog_email)
                    binding.etRemoveMember.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
                R.id.rbRemoveById -> {
                    binding.tilRemoveMember.hint = "Enter Employee ID"
                    binding.tilRemoveMember.setStartIconDrawable(R.drawable.ic_person)
                    binding.etRemoveMember.inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
            }
            binding.etRemoveMember.text?.clear()
        }

        binding.btnAddMember.setOnClickListener {
            val input = binding.etAddMember.text.toString().trim()
            if (input.isEmpty()) {
                val error = if (binding.rbAddByEmail.isChecked) "Please enter an email" else "Employee ID required"
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.rbAddByEmail.isChecked) {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                    updateEmployeeSupervisor(input, "email")
                } else {
                    Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show()
                }
            } else {
                updateEmployeeSupervisor(input, "employeeId")
            }
        }

        binding.btnRemoveMember.setOnClickListener {
            val input = binding.etRemoveMember.text.toString().trim()
            if (input.isEmpty()) {
                val error = if (binding.rbRemoveByEmail.isChecked) "Enter email to remove" else "Employee ID required"
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.rbRemoveByEmail.isChecked) {
                removeEmployeeByField(input, "email")
            } else {
                removeEmployeeByField(input, "employeeId")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter(employeeList) { employee ->
            showRemoveConfirmationDialog(employee)
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
                updateUIState()
            }
    }

    private fun updateUIState() {
        adapter.notifyDataSetChanged()
        if (employeeList.isEmpty()) {
            binding.rvEmployees.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvEmployees.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun updateEmployeeSupervisor(value: String, field: String) {
        db.collection("users")
            .whereEqualTo(field, value)
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
                            if (!employeeList.any { it.uid == newEmp.uid }) {
                                employeeList.add(newEmp)
                                updateUIState()
                            }
                            binding.etAddMember.text?.clear()
                            Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Employee not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding member", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeEmployeeByField(value: String, field: String) {
        val supervisorId = auth.currentUser?.uid ?: ""
        db.collection("users")
            .whereEqualTo(field, value)
            .whereEqualTo("supervisorId", supervisorId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val employee = doc.toObject(Employee::class.java)?.copy(uid = doc.id)
                    if (employee != null) {
                        showRemoveConfirmationDialog(employee)
                        binding.etRemoveMember.text?.clear()
                    }
                } else {
                    Toast.makeText(this, "Member not found in your team", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showRemoveConfirmationDialog(employee: Employee) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${employee.fullName} from your team?")
            .setPositiveButton("Remove") { _, _ ->
                db.collection("users").document(employee.uid)
                    .update("supervisorId", "")
                    .addOnSuccessListener {
                        employeeList.removeAll { it.uid == employee.uid }
                        updateUIState()
                        Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to remove member", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}