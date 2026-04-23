package com.example.dwas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TimeLogsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_logs)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val rvTimeLogs = findViewById<RecyclerView>(R.id.rvTimeLogs)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        val dummyLogs = listOf(
            TimeLog("Pranav Kumar", "09:00 AM", "06:00 PM", "9h 00m", "23 April, 2026"),
            TimeLog("Amit Sharma", "08:30 AM", "05:45 PM", "9h 15m", "23 April, 2026"),
            TimeLog("Suresh Raina", "10:15 AM", "07:15 PM", "9h 00m", "23 April, 2026"),
            TimeLog("Rohit Verma", "09:10 AM", "06:20 PM", "9h 10m", "23 April, 2026"),
            TimeLog("Deepak Singh", "08:55 AM", "05:30 PM", "8h 35m", "23 April, 2026")
        )

        if (dummyLogs.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvTimeLogs.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvTimeLogs.visibility = View.VISIBLE
            rvTimeLogs.layoutManager = LinearLayoutManager(this)
            rvTimeLogs.adapter = TimeLogsAdapter(dummyLogs)
        }
    }

    data class TimeLog(
        val name: String,
        val checkIn: String,
        val checkOut: String,
        val totalHours: String,
        val date: String
    )

    class TimeLogsAdapter(private val logs: List<TimeLog>) :
        RecyclerView.Adapter<TimeLogsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvEmpName)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvIn: TextView = view.findViewById(R.id.tvCheckIn)
            val tvOut: TextView = view.findViewById(R.id.tvCheckOut)
            val tvTotal: TextView = view.findViewById(R.id.tvTotalHours)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_log, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = logs[position]
            holder.tvName.text = log.name
            holder.tvDate.text = "Date: ${log.date}"
            holder.tvIn.text = log.checkIn
            holder.tvOut.text = log.checkOut
            holder.tvTotal.text = log.totalHours
        }

        override fun getItemCount() = logs.size
    }
}