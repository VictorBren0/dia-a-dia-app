package dev.victorbreno.diaadia.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.services.AnalyticsService
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.LocalStorageService
import java.io.ByteArrayOutputStream

class SettingsActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private val firebaseStorage = FirebaseStorage.getInstance()

    private lateinit var emailText: TextView
    private lateinit var nameInput: TextInputEditText
    private lateinit var imageAvatar: CircleImageView
    private lateinit var frameAvatar: FrameLayout
    private lateinit var currentProfile: DiaryProfile

    private var coverPhotoBase64: String = ""

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            setAvatarBitmap(bitmap)
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
                setAvatarBitmap(bitmap)
            }
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
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.profile_editor_title)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        emailText = findViewById(R.id.textProfileEmailValue)
        nameInput = findViewById(R.id.editTextName)
        imageAvatar = findViewById(R.id.imageAvatar)
        frameAvatar = findViewById(R.id.frameAvatar)
        currentProfile = DiaryProfile()

        frameAvatar.setOnClickListener { showPhotoPickerDialog() }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser == null) {
            goToLogin()
            return
        }
        loadProfile()
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

    private fun showPhotoPickerDialog() {
        val options = arrayOf(
            getString(R.string.reflection_photo_camera),
            getString(R.string.reflection_photo_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_change_photo))
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

    private fun setAvatarBitmap(bitmap: Bitmap) {
        val size = 400
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size * bitmap.height / bitmap.width, true)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        coverPhotoBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

        imageAvatar.setImageBitmap(scaled)
    }

    private fun loadAvatarFromBase64(base64: String) {
        if (base64.isBlank()) return
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                imageAvatar.setImageBitmap(bitmap)
                coverPhotoBase64 = base64
            }
        } catch (_: Exception) { }
    }

    fun saveProfile(view: View) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.message_user_not_authenticated), Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        view.isEnabled = false

        val name = nameInput.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            nameInput.error = getString(R.string.error_name_required)
            view.isEnabled = true
            return
        }

        if (coverPhotoBase64.isNotBlank()) {
            uploadPhotoAndSaveProfile(view, name, currentUser.uid)
        } else {
            saveProfileToFirebase(view, name, currentUser.uid, currentProfile.photoUrl)
        }
    }

    private fun uploadPhotoAndSaveProfile(view: View, name: String, uid: String) {
        val imageBytes = Base64.decode(coverPhotoBase64, Base64.DEFAULT)
        val storageRef = firebaseStorage.reference
            .child("profile_photos")
            .child("$uid.jpg")

        storageRef.putBytes(imageBytes)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveProfileToFirebase(view, name, uid, downloadUri.toString())
                }.addOnFailureListener {
                    // Storage URL failed, save with base64 only
                    saveProfileToFirebase(view, name, uid, "")
                }
            }
            .addOnFailureListener {
                // Upload failed, save profile without storage URL
                saveProfileToFirebase(view, name, uid, "")
            }
    }

    private fun saveProfileToFirebase(view: View, name: String, uid: String, photoUrl: String) {
        val currentUser = firebaseAuth.currentUser ?: return

        val profile = currentProfile.copy(
            uid = uid,
            displayName = name,
            email = currentUser.email.orEmpty(),
            photoUrl = photoUrl,
            coverPhotoBase64 = coverPhotoBase64
        )

        val usersPath = getString(R.string.firebase_database_users_path)
        firebaseDatabase.child(usersPath).child(uid).setValue(profile)
            .addOnSuccessListener {
                view.isEnabled = true
                LocalStorageService.saveProfile(this, profile)
                AnalyticsService.init(this)
                AnalyticsService.logProfileUpdated()
                if (coverPhotoBase64.isNotBlank()) AnalyticsService.logProfilePhotoChanged()
                Toast.makeText(this, getString(R.string.message_profile_saved), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                view.isEnabled = true
                Toast.makeText(this, getString(R.string.message_profile_save_error), Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfile() {
        val currentUser = firebaseAuth.currentUser ?: return
        emailText.text = currentUser.email.orEmpty()

        val usersPath = getString(R.string.firebase_database_users_path)
        firebaseDatabase.child(usersPath).child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.getValue(DiaryProfile::class.java)
                if (profile != null) {
                    currentProfile = profile
                    LocalStorageService.saveProfile(this, profile)
                    loadAvatarFromBase64(profile.coverPhotoBase64)
                }
                nameInput.setText(profile?.displayName ?: currentUser.displayName.orEmpty())
            }
            .addOnFailureListener {
                val cachedProfile = LocalStorageService.getProfile(this, currentUser.uid)
                if (cachedProfile != null) {
                    currentProfile = cachedProfile
                    emailText.text = cachedProfile.email.ifBlank { currentUser.email.orEmpty() }
                    nameInput.setText(cachedProfile.displayName)
                    loadAvatarFromBase64(cachedProfile.coverPhotoBase64)
                    Toast.makeText(this, getString(R.string.message_profile_loaded_from_device), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.message_profile_load_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
