package com.example.dwas

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dwas.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            getLastLocationAndNotify()
        } else {
            Toast.makeText(this, "Permissions are required for all features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        createNotificationChannel()
        checkAndRequestPermissions()
        setupUI()
        fetchUserData()
    }

    private fun setupUI() {
        binding.btnProfile.setOnClickListener {
            if (binding.layoutProfileDetails.visibility == View.VISIBLE) {
                binding.layoutProfileDetails.visibility = View.GONE
            } else {
                binding.layoutProfileDetails.visibility = View.VISIBLE
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: "User"
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val age = document.getLong("age")?.toString() ?: "--"
                    val gender = document.getString("gender") ?: "--"
                    val phone = document.getString("phoneNumber") ?: "--"
                    
                    val timeGreeting = getTimeBasedGreeting()
                    binding.tvGreeting.text = "Hi $firstName,\n$timeGreeting"
                    
                    // Populate Profile View
                    binding.tvProfileName.text = "Name: $firstName $lastName"
                    binding.tvProfileEmail.text = "Email: $email"
                    binding.tvProfileAge.text = "Age: $age"
                    binding.tvProfileGender.text = "Gender: $gender"
                    binding.tvProfilePhone.text = "Phone: $phone"
                }
            }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..23 -> "Good Evening"
            else -> "Good Evening"
        }
    }

    private fun getLastLocationAndNotify() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val locationString = if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.get(0)?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                } catch (e: Exception) {
                    "${location.latitude}, ${location.longitude}"
                }
            } else {
                "Unknown Location"
            }
            
            showLoginNotification(locationString)
        }
    }

    private fun showLoginNotification(location: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceName = Build.MODEL

        val builder = NotificationCompat.Builder(this, "LOGIN_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Login Alert")
            .setContentText("Logged in at $currentTime on $deviceName ($location)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(1, builder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Login Notifications"
            val descriptionText = "Notifications for account login events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("LOGIN_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}