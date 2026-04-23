package com.example.dwas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvNotifications: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        rvNotifications = findViewById(R.id.rvNotifications)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupRecyclerView()
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        rvNotifications.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchNotifications() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("login_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val notifications = mutableListOf<NotificationItem>()
                for (doc in documents) {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val device = doc.getString("device") ?: "Unknown Device"
                    val location = doc.getString("location") ?: "Unknown Location"
                    
                    val date = Date(timestamp)
                    val format = SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault())
                    
                    notifications.add(NotificationItem(
                        "Login Detected",
                        "Device: $device\nLocation: $location",
                        format.format(date)
                    ))
                }
                rvNotifications.adapter = NotificationAdapter(notifications)
            }
    }

    data class NotificationItem(val title: String, val details: String, val time: String)

    class NotificationAdapter(private val items: List<NotificationItem>) : 
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val details: TextView = view.findViewById(R.id.tvDetails)
            val time: TextView = view.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.details.text = item.details
            holder.time.text = item.time
        }

        override fun getItemCount() = items.size
    }
}