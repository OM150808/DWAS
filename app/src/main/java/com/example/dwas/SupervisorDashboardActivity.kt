package com.example.dwas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dwas.R
import com.example.dwas.databinding.LayoutProfileItemBinding
import com.example.dwas.databinding.LayoutSupervisorProfileBinding
import com.example.dwas.ui.ProfileUiState
import com.example.dwas.ui.ProfileViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SupervisorDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var contentContainer: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest: LocationRequest? = null

    private lateinit var profileViewModel: ProfileViewModel

    private var currentImageBitmap: Bitmap? = null
    private var currentLocation: Location? = null
    private var currentAddress: String = ""
    private var currentFullAddress: String = ""
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
            if (imageBitmap != null && currentLocation != null) {
                processCapturedImage(imageBitmap)
            } else if (currentLocation == null) {
                Toast.makeText(this, "Waiting for high-accuracy GPS...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        contentContainer = findViewById(R.id.content_container)
        bottomNav = findViewById(R.id.bottomNavigation)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupLocationUpdates()

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        setupBottomNavigation()
        loadHomeView()
    }

    private fun setupLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                    updateAddress(location)
                    updateMapWithLocation()
                }
            }
        }
    }

    private fun updateAddress(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                currentAddress = "${addr.locality ?: ""}, ${addr.adminArea ?: ""}, ${addr.countryName ?: ""}".trimStart(',', ' ')
                currentFullAddress = addr.getAddressLine(0) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processCapturedImage(originalBitmap: Bitmap) {
        val timestamp = SimpleDateFormat("EEEE, dd/MM/yyyy hh:mm a 'GMT' Z", Locale.getDefault()).format(Date())
        currentTimestamp = timestamp

        // Use the map snapshot if available
        googleMap?.snapshot { snapshot ->
            val finalBitmap = drawOverlayOnImage(originalBitmap, currentLocation!!, currentAddress, currentFullAddress, timestamp, snapshot)
            currentImageBitmap = finalBitmap
            
            val view = contentContainer.getChildAt(0)
            if (view != null) {
                updateHomeUIWithCapturedData(view)
            }
        }
    }

    private fun drawOverlayOnImage(
        original: Bitmap,
        loc: Location,
        address: String,
        fullAddress: String,
        time: String,
        mapSnapshot: Bitmap?
    ): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = canvas.width
        val height = canvas.height

        // 1. Semi-transparent overlay background at bottom
        val overlayHeight = height * 0.25f
        val paint = Paint().apply {
            color = Color.parseColor("#80000000") // Semi-transparent black
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - overlayHeight, width.toFloat(), height.toFloat(), paint)

        // 2. Map Thumbnail (Bottom Left)
        if (mapSnapshot != null) {
            val thumbSize = (overlayHeight * 0.8f).toInt()
            val margin = (overlayHeight * 0.1f)
            val scaledMap = Bitmap.createScaledBitmap(mapSnapshot, thumbSize, thumbSize, true)
            
            // Draw rounded map thumbnail
            val rect = RectF(margin, height - thumbSize - margin, margin + thumbSize, height - margin)
            val path = Path().apply { addRoundRect(rect, 20f, 20f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(scaledMap, margin, height - thumbSize - margin, null)
            canvas.restore()
            
            // Text starting position after map
            val textStartX = margin + thumbSize + margin
            drawInfoText(canvas, textStartX, height - thumbSize - margin, width - margin, loc, address, fullAddress, time)
        } else {
            drawInfoText(canvas, 20f, height - overlayHeight + 20f, width - 20f, loc, address, fullAddress, time)
        }

        return result
    }

    private fun drawInfoText(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        loc: Location,
        address: String,
        fullAddress: String,
        time: String
    ) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = canvas.height * 0.025f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = canvas.height * 0.020f
            isAntiAlias = true
        }

        var currentY = startY + textPaint.textSize
        
        // 📍 Address
        canvas.drawText("📍 $address", startX, currentY, textPaint)
        currentY += textPaint.textSize * 1.2f

        // 📌 Full Address
        val wrappedAddress = wrapText(fullAddress, subTextPaint, (maxWidth - startX).toInt())
        for (line in wrappedAddress) {
            canvas.drawText(line, startX, currentY, subTextPaint)
            currentY += subTextPaint.textSize * 1.1f
        }

        // 🌍 Coordinates
        val coords = "Lat ${String.format("%.6f", loc.latitude)}° Long ${String.format("%.6f", loc.longitude)}°"
        canvas.drawText("🌍 $coords", startX, currentY, subTextPaint)
        currentY += subTextPaint.textSize * 1.2f

        // 🕒 Date & Time
        canvas.drawText("🕒 $time", startX, currentY, subTextPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_supervisor_home -> {
                    loadHomeView()
                    true
                }
                R.id.nav_supervisor_manage -> {
                    loadManageView()
                    true
                }
                R.id.nav_supervisor_approvals -> {
                    loadApprovalsView()
                    true
                }
                R.id.nav_supervisor_profile -> {
                    loadProfileView()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadHomeView() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_supervisor_home, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(view)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvTotalEmployees = view.findViewById<TextView>(R.id.tvTotalEmployees)
        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val btnProfile = view.findViewById<ImageButton>(R.id.btnProfile)

        // Initialize MapView
        mapView = view.findViewById(R.id.mapPreview)
        mapView?.onCreate(null)
        mapView?.getMapAsync(this)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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

        btnProfile.setOnClickListener { bottomNav.selectedItemId = R.id.nav_supervisor_profile }

        view.findViewById<View>(R.id.btnQuickManage).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_supervisor_manage
        }

        view.findViewById<View>(R.id.cardCamera).setOnClickListener {
            if (currentImageBitmap == null) {
                checkPermissionsAndOpen()
            }
        }

        view.findViewById<View>(R.id.btnRetake).setOnClickListener {
            checkPermissionsAndOpen()
        }

        view.findViewById<View>(R.id.btnUploadImage).setOnClickListener {
            if (currentImageBitmap != null && currentLocation != null) {
                uploadCapturedImage(view)
            } else if (currentLocation == null) {
                Toast.makeText(this, "Waiting for high-accuracy GPS...", Toast.LENGTH_SHORT).show()
                startLocationUpdates()
            }
        }

        view.findViewById<View>(R.id.btnTimeLogs).setOnClickListener {
            startActivity(Intent(this, TimeLogsActivity::class.java))
        }

        view.findViewById<View>(R.id.btnTeamStats).setOnClickListener {
            startActivity(Intent(this, TeamStatsActivity::class.java))
        }

        val currentSupervisorId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentSupervisorId).get().addOnSuccessListener { document ->
            tvUserName.text = document.getString("firstName") ?: "Supervisor"
        }

        db.collection("users")
            .whereEqualTo("role", "employee")
            .whereEqualTo("supervisorId", currentSupervisorId)
            .get()
            .addOnSuccessListener { documents ->
                tvTotalEmployees.text = documents.size().toString()
            }
            
        // Restore UI State if data exists
        if (currentImageBitmap != null) {
            updateHomeUIWithCapturedData(view)
        }
    }

    private fun uploadCapturedImage(rootView: View) {
        val progressBar = rootView.findViewById<ProgressBar>(R.id.progressBarCapture)
        val btnUpload = rootView.findViewById<View>(R.id.btnUploadImage)
        
        progressBar?.visibility = View.VISIBLE
        btnUpload?.isEnabled = false
        
        // Placeholder for upload logic (e.g., Firestore or Storage)
        val baos = ByteArrayOutputStream()
        currentImageBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageData = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        
        val reportData = hashMapOf(
            "supervisorId" to (auth.currentUser?.uid ?: ""),
            "timestamp" to (currentTimestamp ?: ""),
            "latitude" to (currentLocation?.latitude ?: 0.0),
            "longitude" to (currentLocation?.longitude ?: 0.0),
            "imageData" to imageData
        )
        
        db.collection("reports").add(reportData)
            .addOnSuccessListener {
                progressBar?.visibility = View.GONE
                btnUpload?.isEnabled = true
                Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                resetCaptureState(rootView)
            }
            .addOnFailureListener {
                progressBar?.visibility = View.GONE
                btnUpload?.isEnabled = true
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetCaptureState(rootView: View) {
        currentImageBitmap = null
        currentLocation = null
        currentTimestamp = null
        
        val ivCaptured = rootView.findViewById<ImageView>(R.id.ivCapturedImage)
        val tvCoords = rootView.findViewById<TextView>(R.id.tvLocationCoords)
        val tvTime = rootView.findViewById<TextView>(R.id.tvTimestamp)
        val layoutActions = rootView.findViewById<View>(R.id.layoutPostCaptureActions)
        val ivCameraIcon = rootView.findViewById<ImageView>(R.id.ivCameraAction)
        val tvCameraHint = rootView.findViewById<TextView>(R.id.tvCameraHint)
        val map = rootView.findViewById<MapView>(R.id.mapPreview)

        ivCaptured.visibility = View.GONE
        tvCoords.visibility = View.GONE
        tvTime.visibility = View.GONE
        layoutActions.visibility = View.GONE
        map.visibility = View.GONE
        
        ivCameraIcon.visibility = View.VISIBLE
        tvCameraHint.visibility = View.VISIBLE
    }

    private fun updateHomeUIWithCapturedData(rootView: View) {
        val ivCaptured = rootView.findViewById<ImageView>(R.id.ivCapturedImage)
        val tvCoords = rootView.findViewById<TextView>(R.id.tvLocationCoords)
        val tvTime = rootView.findViewById<TextView>(R.id.tvTimestamp)
        val layoutActions = rootView.findViewById<View>(R.id.layoutPostCaptureActions)
        val ivCameraIcon = rootView.findViewById<ImageView>(R.id.ivCameraAction)
        val tvCameraHint = rootView.findViewById<TextView>(R.id.tvCameraHint)
        val btnUpload = rootView.findViewById<View>(R.id.btnUploadImage)

        ivCaptured.setImageBitmap(currentImageBitmap)
        ivCaptured.visibility = View.VISIBLE
        ivCameraIcon.visibility = View.GONE
        tvCameraHint.visibility = View.GONE
        
        currentTimestamp?.let {
            tvTime.text = it
            tvTime.visibility = View.VISIBLE
        }
        
        if (currentLocation != null) {
            currentLocation?.let {
                tvCoords.text = "📍 $currentAddress\nLat: ${String.format("%.6f", it.latitude)}, Lng: ${String.format("%.6f", it.longitude)}"
                tvCoords.visibility = View.VISIBLE
                updateMapWithLocation()
            }
            btnUpload.isEnabled = true
        } else {
            tvCoords.text = "Fetching high-accuracy GPS..."
            tvCoords.visibility = View.VISIBLE
            btnUpload.isEnabled = false
        }
        
        layoutActions.visibility = View.VISIBLE
    }

    private fun loadManageView() {
        cleanupMap()
        val view = LayoutInflater.from(this).inflate(R.layout.activity_manage_employees, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(view)

        val etEmail = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAddMember)
        val tilAddMember = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilAddMember)
        val rgAddType = view.findViewById<RadioGroup>(R.id.rgAddType)
        val btnAdd = view.findViewById<View>(R.id.btnAddMember)

        rgAddType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAddByEmail -> {
                    tilAddMember.hint = "Enter Employee Email"
                    tilAddMember.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_email)
                    etEmail.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
                R.id.rbAddById -> {
                    tilAddMember.hint = "Enter Employee ID"
                    tilAddMember.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_id)
                    etEmail.inputType = InputType.TYPE_CLASS_TEXT
                }
            }
        }

        val etRemove = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRemoveMember)
        val tilRemoveMember = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilRemoveMember)
        val rgRemoveType = view.findViewById<RadioGroup>(R.id.rgRemoveType)
        val btnRemove = view.findViewById<View>(R.id.btnRemoveMember)

        rgRemoveType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRemoveByEmail -> {
                    tilRemoveMember.hint = "Enter Employee Email"
                    tilRemoveMember.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_email)
                    etRemove.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
                R.id.rbRemoveById -> {
                    tilRemoveMember.hint = "Enter Employee ID"
                    tilRemoveMember.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_id)
                    etRemove.inputType = InputType.TYPE_CLASS_TEXT
                }
            }
        }

        val rvEmployees = view.findViewById<RecyclerView>(R.id.rvEmployees)

        val employeeList = mutableListOf<Employee>()
        val adapter = EmployeeAdapter(employeeList) { employee ->
            db.collection("users").document(employee.uid).update("supervisorId", "")
                .addOnSuccessListener {
                    employeeList.remove(employee)
                    rvEmployees.adapter?.notifyDataSetChanged()
                }
        }
        rvEmployees.layoutManager = LinearLayoutManager(this)
        rvEmployees.adapter = adapter

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

        btnAdd.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                db.collection("users").whereEqualTo("email", email).whereEqualTo("role", "employee").get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val doc = documents.documents[0]
                            db.collection("users").document(doc.id).update("supervisorId", supervisorId)
                                .addOnSuccessListener {
                                    val newEmp = doc.toObject(Employee::class.java)!!.copy(uid = doc.id)
                                    employeeList.add(newEmp)
                                    adapter.notifyDataSetChanged()
                                    etEmail.text?.clear()
                                }
                        }
                    }
            }
        }
    }

    private fun loadApprovalsView() {
        cleanupMap()
        val view = LayoutInflater.from(this).inflate(R.layout.layout_supervisor_approvals, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(view)
    }

    private fun loadProfileView() {
        cleanupMap()
        val binding = LayoutSupervisorProfileBinding.inflate(layoutInflater, contentContainer, false)
        contentContainer.removeAllViews()
        contentContainer.addView(binding.root)

        val viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ProfileUiState.Loading -> {
                        binding.loadingOverlay.visibility = View.VISIBLE
                    }
                    is ProfileUiState.Success -> {
                        binding.loadingOverlay.visibility = View.GONE
                        val user = state.user
                        
                        binding.tvFullName.text = user.fullName
                        binding.tvRole.text = user.role.ifEmpty { "Supervisor" }
                        binding.chipStatus.text = user.status
                        
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@SupervisorDashboardActivity)
                                .load(user.profileImageUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(binding.ivProfileImage)
                        }

                        // Personal Info
                        setupProfileItem(binding.itemEmail, R.drawable.ic_email, "Email", user.email)
                        setupProfileItem(binding.itemPhone, R.drawable.ic_phone, "Phone Number", user.phoneNumber ?: "N/A")
                        setupProfileItem(binding.itemAge, R.drawable.ic_person, "Age", user.age?.toString() ?: "N/A")
                        setupProfileItem(binding.itemGender, R.drawable.ic_person, "Gender", user.gender ?: "N/A")

                        // Work Info
                        setupProfileItem(binding.itemEmployeeId, R.drawable.ic_badge, "Employee ID", user.employeeId.ifEmpty { "N/A" })
                        setupProfileItem(binding.itemDepartment, R.drawable.ic_work, "Department", user.department.ifEmpty { "N/A" })
                        setupProfileItem(binding.itemShift, R.drawable.ic_time, "Shift Timing", user.shift.ifEmpty { "N/A" })
                        setupProfileItem(binding.itemLocation, R.drawable.ic_location, "Office Location", user.location.ifEmpty { "N/A" })
                        setupProfileItem(binding.itemJoiningDate, R.drawable.ic_calendar, "Joining Date", user.joiningDate.ifEmpty { "N/A" })

                        // Team Info
                        setupProfileItem(binding.itemTeamSize, R.drawable.ic_group, "Team Size", user.teamSize.toString())
                        setupProfileItem(binding.itemProject, R.drawable.ic_assignment, "Assigned Team/Project", user.project.ifEmpty { "N/A" })
                    }
                    is ProfileUiState.Error -> {
                        binding.loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@SupervisorDashboardActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is ProfileUiState.Empty -> {
                        binding.loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@SupervisorDashboardActivity, "Profile not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProfileItem(itemBinding: LayoutProfileItemBinding, iconRes: Int, label: String, value: String) {
        itemBinding.ivIcon.setImageResource(iconRes)
        itemBinding.tvLabel.text = label
        itemBinding.tvValue.text = value
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
        map.addMarker(MarkerOptions().position(latLng).title("Current Location"))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        
        // Ensure MapView is visible but possibly small or hidden if preferred
        mapView?.visibility = View.VISIBLE
    }

    private fun getLastLocationAndUpdateUI() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val view = contentContainer.getChildAt(0)
            val progressBar = view?.findViewById<ProgressBar>(R.id.progressBarCapture)
            progressBar?.visibility = View.VISIBLE

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    progressBar?.visibility = View.GONE
                    if (location != null) {
                        currentLocation = location
                        if (view != null && view.findViewById<View>(R.id.cardCamera) != null) {
                            updateHomeUIWithCapturedData(view)
                        }
                    } else {
                        Toast.makeText(this, "Could not get accurate location. Retrying with last known...", Toast.LENGTH_SHORT).show()
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                currentLocation = lastLoc
                                if (view != null) updateHomeUIWithCapturedData(view)
                            } else {
                                Toast.makeText(this, "Location failed. Please ensure GPS is ON.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    progressBar?.visibility = View.GONE
                    Toast.makeText(this, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationRequest?.let {
                fusedLocationClient.requestLocationUpdates(it, locationCallback, Looper.getMainLooper())
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupMap()
        stopLocationUpdates()
    }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
}