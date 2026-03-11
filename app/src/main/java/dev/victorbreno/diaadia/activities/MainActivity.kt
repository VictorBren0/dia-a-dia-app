package dev.victorbreno.diaadia.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.data.ReflectionEntry
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.GoogleAuthHelper
import dev.victorbreno.diaadia.services.LocalStorageService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private lateinit var greetingText: TextView
    private lateinit var dateText: TextView
    private lateinit var reflectionListLayout: LinearLayout
    private lateinit var reflectionTitleText: TextView

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
        reflectionListLayout = findViewById(R.id.layoutReflectionList)
        reflectionTitleText = findViewById(R.id.textReflectionDate)
        reflectionTitleText.text = getString(R.string.home_reflections_title)

        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
        dateText.text = dateFormat.format(Date())
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        LocalStorageService.saveUserSession(
            this,
            currentUser.uid,
            currentUser.displayName.orEmpty(),
            currentUser.email.orEmpty()
        )
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
                GoogleAuthHelper.signOut(this)
                LocalStorageService.clearUserSession(this)
                goToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun goToReflectionEditor(view: View) {
        view.isEnabled = false
        startActivity(Intent(this, ReflectionActivity::class.java))
        view.post { view.isEnabled = true }
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
                val resolvedProfile = profile ?: DiaryProfile(
                    uid = currentUser.uid,
                    displayName = currentUser.displayName.orEmpty(),
                    email = currentUser.email.orEmpty(),
                    todayFocus = getString(R.string.env_default_focus),
                    dailyReflection = getString(R.string.env_default_reflection)
                )
                val displayName = resolvedProfile.displayName.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName
                    ?: getString(R.string.home_empty_name)
                val remoteReflectionEntries = snapshot.child("reflections").children
                    .mapNotNull { child ->
                        val entry = child.getValue(ReflectionEntry::class.java) ?: return@mapNotNull null
                        val text = entry.text.ifBlank { child.child("text").getValue(String::class.java).orEmpty() }
                        if (text.isBlank()) {
                            return@mapNotNull null
                        }

                        entry.copy(
                            id = entry.id.ifBlank { child.key.orEmpty() },
                            text = text,
                            createdAt = entry.createdAt.takeIf { it > 0L }
                                ?: child.child("createdAt").getValue(Long::class.java)
                                ?: 0L,
                            formattedDate = entry.formattedDate.ifBlank {
                                child.child("formattedDate").getValue(String::class.java).orEmpty()
                            }
                        )
                    }
                val cachedReflectionEntries = LocalStorageService.getReflections(this, currentUser.uid)
                val reflectionEntries = (remoteReflectionEntries + cachedReflectionEntries)
                    .distinctBy { entry -> entry.id.ifBlank { "${entry.createdAt}-${entry.text}" } }
                    .sortedByDescending { it.createdAt }

                LocalStorageService.saveProfile(this, resolvedProfile)
                LocalStorageService.saveReflections(this, currentUser.uid, reflectionEntries)

                greetingText.text = getString(R.string.home_greeting, displayName)
                if (reflectionEntries.isNotEmpty()) {
                    showReflectionEntries(reflectionEntries)
                } else {
                    val legacyText = resolvedProfile.dailyReflection.takeIf { it.isNotBlank() }
                    val legacyDate = resolvedProfile.reflectionDate.takeIf { it.isNotBlank() }.orEmpty()
                    showLegacyReflection(legacyText, legacyDate)
                }
            }
            .addOnFailureListener {
                val cachedProfile = LocalStorageService.getProfile(this, currentUser.uid)
                val cachedReflections = LocalStorageService.getReflections(this, currentUser.uid)

                if (cachedProfile != null || cachedReflections.isNotEmpty()) {
                    val displayName = cachedProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.displayName
                        ?: getString(R.string.home_empty_name)

                    greetingText.text = getString(R.string.home_greeting, displayName)
                    if (cachedReflections.isNotEmpty()) {
                        showReflectionEntries(cachedReflections.sortedByDescending { entry -> entry.createdAt })
                    } else {
                        showLegacyReflection(
                            cachedProfile?.dailyReflection,
                            cachedProfile?.reflectionDate.orEmpty()
                        )
                    }

                    Toast.makeText(this, getString(R.string.message_profile_loaded_from_device), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.message_profile_load_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showReflectionEntries(entries: List<ReflectionEntry>) {
        reflectionListLayout.removeAllViews()
        entries.forEachIndexed { index, entry ->
            val itemView = createReflectionItem(entry.formattedDate, entry.text)
            reflectionListLayout.addView(itemView)

            if (index < entries.lastIndex) {
                reflectionListLayout.addView(createReflectionDivider())
            }
        }
    }

    private fun showLegacyReflection(text: String?, date: String) {
        reflectionListLayout.removeAllViews()
        val content = text ?: getString(R.string.env_default_reflection)
        reflectionListLayout.addView(createReflectionItem(date, content))
    }

    private fun createReflectionItem(date: String, content: String): LinearLayout {
        val context = this
        val container = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        if (date.isNotBlank()) {
            val dateView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = date
                textSize = 12f
                setTextColor(getColor(R.color.journalMuted))
            }
            container.addView(dateView)
        }

        val contentView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { params ->
                if (date.isNotBlank()) {
                    params.topMargin = dpToPx(6)
                }
            }
            text = content
            textSize = 15f
            setLineSpacing(dpToPx(6).toFloat(), 1f)
            setTextColor(getColor(R.color.journalInk))
            typeface = android.graphics.Typeface.SERIF
        }
        container.addView(contentView)

        return container
    }

    private fun createReflectionDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).also { params ->
                params.topMargin = dpToPx(14)
                params.bottomMargin = dpToPx(14)
            }
            setBackgroundColor(getColor(R.color.journalDivider))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}