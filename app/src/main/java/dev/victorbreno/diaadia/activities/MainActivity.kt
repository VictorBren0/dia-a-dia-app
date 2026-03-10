package dev.victorbreno.diaadia.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private lateinit var greetingText: TextView
    private lateinit var dateText: TextView
    private lateinit var reflectionText: TextView
    private lateinit var reflectionDateText: TextView
    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.home_title)
        setSupportActionBar(toolbar)

        greetingText = findViewById(R.id.textGreeting)
        dateText = findViewById(R.id.textDate)
        reflectionText = findViewById(R.id.textReflectionValue)
        reflectionDateText = findViewById(R.id.textReflectionDate)

        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        dateText.text = dateFormat.format(Date())
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser == null) {
            goToLogin()
            return
        }

        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser != null) {
            loadProfile()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profileMenu -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.logoutMenu -> {
                firebaseAuth.signOut()
                goToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun goToReflectionEditor(view: View) {
        startActivity(Intent(this, ReflectionActivity::class.java))
    }

    fun toggleReflection(view: View) {
        if (isExpanded) {
            reflectionText.maxLines = 4
            isExpanded = false
        } else {
            reflectionText.maxLines = Integer.MAX_VALUE
            isExpanded = true
        }
    }

    private fun loadProfile() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.message_user_not_authenticated), Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        val usersPath = getString(R.string.firebase_database_users_path)
        firebaseDatabase.child(usersPath).child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.getValue(DiaryProfile::class.java)
                val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName
                    ?: getString(R.string.home_empty_name)

                greetingText.text = getString(R.string.home_greeting, displayName)
                reflectionText.text = profile?.dailyReflection?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.env_default_reflection)
                
                val dateStr = profile?.reflectionDate?.takeIf { !it.isNullOrBlank() }
                if (dateStr != null) {
                    reflectionDateText.text = "Reflexão do dia $dateStr"
                } else {
                    reflectionDateText.text = getString(R.string.home_reflection_title)
                }
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