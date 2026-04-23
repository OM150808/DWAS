package com.example.dwas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EmployeeDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentImageBitmap: Bitmap? = null
    private var currentLocation: Location? = null
    private var currentTimestamp: String? = null
    private var googleMap: GoogleMap? = null
    private var mapView: MapView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            openCamera()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                currentImageBitmap = imageBitmap
                currentTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
                getLastLocationAndUpdateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        contentContainer = findViewById(R.id.content_container)
        bottomNav = findViewById(R.id.bottomNavigation)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupBottomNavigation()
        loadHomeView()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_employee_home -> {
                    loadHomeView()
                    true
                }
                R.id.nav_employee_tasks -> {
                    loadTasksView()
                    true
                }
                R.id.nav_employee_history -> {
                    loadHistoryView()
                    true
                }
                R.id.nav_employee_profile -> {
                    loadProfileView()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadHomeView() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_employee_home, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(view)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val btnProfile = view.findViewById<ImageButton>(R.id.btnProfile)

        // Initialize MapView
        mapView = view.findViewById(R.id.mapPreview)
        mapView?.onCreate(null)
        mapView?.getMapAsync(this)

        // Time-based greeting
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        tvWelcome.text = when (hour) {
            in 0..11 -> "Good Morning,"
            in 12..15 -> "Good Afternoon,"
            in 16..20 -> "Good Evening,"
            else -> "Good Night,"
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnProfile.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_employee_profile
        }

        view.findViewById<View>(R.id.btnCheckIn).setOnClickListener {
            Toast.makeText(this, "Checking in...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.cardCamera).setOnClickListener {
            checkPermissionsAndOpen()
        }

        view.findViewById<View>(R.id.btnRetake).setOnClickListener {
            checkPermissionsAndOpen()
        }

        view.findViewById<View>(R.id.btnUploadImage).setOnClickListener {
            if (currentImageBitmap != null) {
                Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
                // Upload logic here
            }
        }

        view.findViewById<View>(R.id.btnViewTasks).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_employee_tasks
        }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            tvUserName.text = document.getString("firstName") ?: "Employee"
        }

        // Restore UI State if data exists
        if (currentImageBitmap != null) {
            updateHomeUIWithCapturedData(view)
        }
    }

    private fun updateHomeUIWithCapturedData(rootView: View) {
        val ivCaptured = rootView.findViewById<ImageView>(R.id.ivCapturedImage)
        val tvCoords = rootView.findViewById<TextView>(R.id.tvLocationCoords)
        val tvTime = rootView.findViewById<TextView>(R.id.tvTimestamp)
        val layoutActions = rootView.findViewById<View>(R.id.layoutPostCaptureActions)
        val ivCameraIcon = rootView.findViewById<ImageView>(R.id.ivCameraAction)
        val tvCameraHint = rootView.findViewById<TextView>(R.id.tvCameraHint)

        ivCaptured.setImageBitmap(currentImageBitmap)
        ivCaptured.visibility = View.VISIBLE
        ivCameraIcon.visibility = View.GONE
        tvCameraHint.visibility = View.GONE
        
        currentTimestamp?.let {
            tvTime.text = "Captured at: $it"
            tvTime.visibility = View.VISIBLE
        }
        
        currentLocation?.let {
            tvCoords.text = "Lat: ${String.format("%.4f", it.latitude)}, Lng: ${String.format("%.4f", it.longitude)}"
            tvCoords.visibility = View.VISIBLE
            updateMapWithLocation()
        }
        
        layoutActions.visibility = View.VISIBLE
    }

    private fun loadTasksView() {
        cleanupMap()
        val view = View(this)
        view.setBackgroundColor(getColor(R.color.smarthome_bg))
        contentContainer.removeAllViews()
        contentContainer.addView(view)
        Toast.makeText(this, "Tasks Screen", Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryView() {
        cleanupMap()
        val view = View(this)
        view.setBackgroundColor(getColor(R.color.smarthome_bg))
        contentContainer.removeAllViews()
        contentContainer.addView(view)
        Toast.makeText(this, "History Screen", Toast.LENGTH_SHORT).show()
    }

    private fun loadProfileView() {
        cleanupMap()
        val view = LayoutInflater.from(this).inflate(R.layout.layout_employee_profile, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(view)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvAge = view.findViewById<TextView>(R.id.tvProfileAge)
        val tvGender = view.findViewById<TextView>(R.id.tvProfileGender)
        val tvPhone = view.findViewById<TextView>(R.id.tvProfilePhone)

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                tvName.text = "Name: ${doc.getString("firstName")} ${doc.getString("lastName")}"
                tvEmail.text = "Email: ${doc.getString("email")}"
                tvAge.text = "Age: ${doc.getString("age")}"
                tvGender.text = "Gender: ${doc.getString("gender")}"
                tvPhone.text = "Phone: ${doc.getString("phone")}"
            }
        }
    }

    private fun cleanupMap() {
        mapView?.onPause()
        mapView?.onDestroy()
        mapView = null
        googleMap = null
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        updateMapWithLocation()
    }

    private fun updateMapWithLocation() {
        val loc = currentLocation ?: return
        val map = googleMap ?: return
        val latLng = LatLng(loc.latitude, loc.longitude)
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title("Captured Location"))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        mapView?.visibility = View.VISIBLE
    }

    private fun getLastLocationAndUpdateUI() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    val view = contentContainer.getChildAt(0)
                    if (view != null && view.findViewById<View>(R.id.cardCamera) != null) {
                        updateHomeUIWithCapturedData(view)
                    }
                } else {
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermissionsAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { super.onPause(); mapView?.onPause() }
    override fun onDestroy() { super.onDestroy(); cleanupMap() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
}
