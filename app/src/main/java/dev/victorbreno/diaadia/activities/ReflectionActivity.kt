package dev.victorbreno.diaadia.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.data.ReflectionEntry
import dev.victorbreno.diaadia.services.AnalyticsService
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.LocalStorageService
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class ReflectionActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()

    private lateinit var reflectionInput: TextInputEditText
    private lateinit var imagePreview: ImageView
    private lateinit var photoPlaceholder: LinearLayout
    private lateinit var framePhoto: FrameLayout
    private lateinit var textLocation: TextView
    private lateinit var btnRefreshLocation: Button

    private var photoBase64: String = ""
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentLocationName: String = ""

    private var editingEntryId: String? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            setPhotoBitmap(bitmap)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                setPhotoBitmap(bitmap)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            textLocation.text = getString(R.string.reflection_location_denied)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(this, getString(R.string.reflection_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reflection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reflectionInput = findViewById(R.id.editTextReflection)
        imagePreview = findViewById(R.id.imagePreview)
        photoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder)
        framePhoto = findViewById(R.id.framePhoto)
        textLocation = findViewById(R.id.textLocation)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)

        framePhoto.setOnClickListener { showPhotoPickerDialog() }
        btnRefreshLocation.setOnClickListener { requestLocationPermission() }

        loadEditData()
        requestLocationPermission()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadEditData() {
        val entryId = intent.getStringExtra("ENTRY_ID")
        if (entryId.isNullOrBlank()) {
            supportActionBar?.title = getString(R.string.reflection_title_create)
            return
        }

        editingEntryId = entryId
        supportActionBar?.title = getString(R.string.reflection_title_edit)

        val entryText = intent.getStringExtra("ENTRY_TEXT").orEmpty()
        val entryPhoto = intent.getStringExtra("ENTRY_PHOTO").orEmpty()
        val entryLat = intent.getDoubleExtra("ENTRY_LATITUDE", 0.0)
        val entryLng = intent.getDoubleExtra("ENTRY_LONGITUDE", 0.0)
        val entryLocation = intent.getStringExtra("ENTRY_LOCATION").orEmpty()

        reflectionInput.setText(entryText)

        if (entryPhoto.isNotBlank()) {
            photoBase64 = entryPhoto
            val bytes = Base64.decode(entryPhoto, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                imagePreview.setImageBitmap(bitmap)
                imagePreview.visibility = View.VISIBLE
                photoPlaceholder.visibility = View.GONE
            }
        }

        if (entryLat != 0.0 || entryLng != 0.0) {
            currentLatitude = entryLat
            currentLongitude = entryLng
            currentLocationName = entryLocation
            textLocation.text = if (entryLocation.isNotBlank()) entryLocation
                else String.format(Locale.US, "%.4f, %.4f", entryLat, entryLng)
        }
    }

    private fun showPhotoPickerDialog() {
        val options = arrayOf(
            getString(R.string.reflection_photo_camera),
            getString(R.string.reflection_photo_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reflection_photo_choose))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setPhotoBitmap(bitmap: Bitmap) {
        val scaled = Bitmap.createScaledBitmap(bitmap, 800, 800 * bitmap.height / bitmap.width, true)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        photoBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

        imagePreview.setImageBitmap(scaled)
        imagePreview.visibility = View.VISIBLE
        photoPlaceholder.visibility = View.GONE
    }

    private fun requestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        textLocation.text = getString(R.string.reflection_location_loading)
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val cancellationToken = CancellationTokenSource()

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    resolveLocationName(location.latitude, location.longitude)
                } else {
                    textLocation.text = getString(R.string.reflection_location_unavailable)
                }
            }
            .addOnFailureListener {
                textLocation.text = getString(R.string.reflection_location_unavailable)
            }
    }

    private fun resolveLocationName(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val parts = listOfNotNull(
                    addr.thoroughfare,
                    addr.subLocality,
                    addr.locality,
                    addr.adminArea
                )
                currentLocationName = if (parts.isNotEmpty()) parts.joinToString(", ")
                    else String.format(Locale.US, "%.4f, %.4f", lat, lng)
            } else {
                currentLocationName = String.format(Locale.US, "%.4f, %.4f", lat, lng)
            }
        } catch (_: Exception) {
            currentLocationName = String.format(Locale.US, "%.4f, %.4f", lat, lng)
        }
        textLocation.text = currentLocationName
    }

    fun saveReflection(view: View) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.message_user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        view.isEnabled = false

        val reflectionText = reflectionInput.text?.toString()?.trim() ?: ""
        if (reflectionText.isEmpty()) {
            Toast.makeText(this, getString(R.string.reflection_error_empty_text), Toast.LENGTH_SHORT).show()
            view.isEnabled = true
            return
        }

        if (photoBase64.isEmpty()) {
            Toast.makeText(this, getString(R.string.reflection_error_no_photo), Toast.LENGTH_SHORT).show()
            view.isEnabled = true
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
        val dateString = dateFormat.format(Date())
        val timestamp = System.currentTimeMillis()

        val usersPath = getString(R.string.firebase_database_users_path)
        val userRef = firebaseDatabase.child(usersPath).child(currentUser.uid)

        val isEditing = !editingEntryId.isNullOrBlank()
        val reflectionId: String
        if (isEditing) {
            reflectionId = editingEntryId!!
        } else {
            val newRef = userRef.child("reflections").push()
            reflectionId = newRef.key ?: run {
                Toast.makeText(this, getString(R.string.reflection_error_generate_id), Toast.LENGTH_SHORT).show()
                view.isEnabled = true
                return
            }
        }

        val reflectionEntry = ReflectionEntry(
            id = reflectionId,
            text = reflectionText,
            photoBase64 = photoBase64,
            latitude = currentLatitude,
            longitude = currentLongitude,
            locationName = currentLocationName,
            createdAt = if (isEditing) intent.getLongExtra("ENTRY_CREATED_AT", timestamp) else timestamp,
            formattedDate = if (isEditing) intent.getStringExtra("ENTRY_FORMATTED_DATE") ?: dateString else dateString
        )

        LocalStorageService.saveReflection(this, currentUser.uid, reflectionEntry)
        val cachedProfile = LocalStorageService.getProfile(this, currentUser.uid) ?: DiaryProfile(
            uid = currentUser.uid,
            displayName = currentUser.displayName.orEmpty(),
            email = currentUser.email.orEmpty()
        )
        LocalStorageService.saveProfile(
            this,
            cachedProfile.copy(
                dailyReflection = reflectionText,
                reflectionDate = dateString
            )
        )

        val entryMap = mapOf(
            "id" to reflectionEntry.id,
            "text" to reflectionEntry.text,
            "photoBase64" to reflectionEntry.photoBase64,
            "latitude" to reflectionEntry.latitude,
            "longitude" to reflectionEntry.longitude,
            "locationName" to reflectionEntry.locationName,
            "createdAt" to reflectionEntry.createdAt,
            "formattedDate" to reflectionEntry.formattedDate
        )

        val updates = mapOf<String, Any>(
            "reflections/$reflectionId" to entryMap,
            "dailyReflection" to reflectionText,
            "reflectionDate" to dateString
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                AnalyticsService.init(this)
                if (isEditing) AnalyticsService.logReflectionEdited()
                else AnalyticsService.logReflectionCreated()
                val msg = if (isEditing) getString(R.string.reflection_updated_success)
                    else getString(R.string.reflection_saved_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                view.isEnabled = true
                Toast.makeText(this, getString(R.string.message_reflection_saved_locally), Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
