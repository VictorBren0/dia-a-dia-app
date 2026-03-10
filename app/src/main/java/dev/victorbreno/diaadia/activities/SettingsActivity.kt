package dev.victorbreno.diaadia.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.services.FirebaseConfiguration

class SettingsActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private lateinit var emailText: TextView
    private lateinit var nameInput: TextInputEditText
    private lateinit var currentProfile: DiaryProfile

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
        currentProfile = DiaryProfile()
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

    fun saveProfile(view: View) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.message_user_not_authenticated), Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        val name = nameInput.text?.toString()?.trim().orEmpty()

        if (name.isBlank()) {
            nameInput.error = getString(R.string.error_name_required)
            return
        }

        val profile = currentProfile.copy(
            uid = currentUser.uid,
            displayName = name,
            email = currentUser.email.orEmpty()
        )

        val usersPath = getString(R.string.firebase_database_users_path)
        firebaseDatabase.child(usersPath).child(currentUser.uid).setValue(profile)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.message_profile_saved), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
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
                }
                nameInput.setText(profile?.displayName ?: currentUser.displayName.orEmpty())
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.message_profile_load_error), Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}