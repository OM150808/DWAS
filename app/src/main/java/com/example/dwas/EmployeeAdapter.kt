package com.example.dwas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmployeeAdapter(
    private val employees: MutableList<Employee>,
    private val onRemoveClick: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvEmployeeName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmployeeEmail)
        val btnRemove: View = view.findViewById(R.id.btnRemoveEmployee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employees[position]
        holder.tvName.text = employee.fullName
        holder.tvEmail.text = employee.email
        holder.btnRemove.setOnClickListener { onRemoveClick(employee) }
    }

    override fun getItemCount() = employees.size

    fun removeEmployee(employee: Employee) {
        val position = employees.indexOf(employee)
        if (position != -1) {
            employees.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addEmployee(employee: Employee) {
        employees.add(employee)
        notifyItemInserted(employees.size - 1)
    }
}