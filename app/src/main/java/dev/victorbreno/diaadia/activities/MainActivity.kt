package dev.victorbreno.diaadia.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.api.RetrofitClient
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.data.Quote
import dev.victorbreno.diaadia.data.ReflectionEntry
import dev.victorbreno.diaadia.fragments.ReflectionListFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import dev.victorbreno.diaadia.notifications.ReminderWorker
import dev.victorbreno.diaadia.services.AnalyticsService
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.GoogleAuthHelper
import dev.victorbreno.diaadia.services.LocalStorageService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private lateinit var greetingText: TextView
    private lateinit var dateText: TextView
    private lateinit var reflectionListFragment: ReflectionListFragment
    private lateinit var cardQuote: LinearLayout
    private lateinit var textQuote: TextView
    private lateinit var textQuoteAuthor: TextView
    private lateinit var adViewBanner: AdView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we schedule the worker anyway */ }

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
        cardQuote = findViewById(R.id.cardQuote)
        textQuote = findViewById(R.id.textQuote)
        textQuoteAuthor = findViewById(R.id.textQuoteAuthor)
        reflectionListFragment = supportFragmentManager.findFragmentById(R.id.fragmentReflectionList) as ReflectionListFragment
        adViewBanner = findViewById(R.id.adViewBanner)

        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
        dateText.text = dateFormat.format(Date())

        // Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Firebase Analytics
        AnalyticsService.init(this)
        AnalyticsService.logScreenView("home")

        // AdMob
        MobileAds.initialize(this)
        adViewBanner.loadAd(AdRequest.Builder().build())

        loadQuoteOfTheDay()
        requestNotificationPermission()
        scheduleReminderWorker()
        subscribeToPushNotifications()
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
                            photoBase64 = entry.photoBase64.ifBlank {
                                child.child("photoBase64").getValue(String::class.java).orEmpty()
                            },
                            latitude = entry.latitude.takeIf { it != 0.0 }
                                ?: child.child("latitude").getValue(Double::class.java)
                                ?: 0.0,
                            longitude = entry.longitude.takeIf { it != 0.0 }
                                ?: child.child("longitude").getValue(Double::class.java)
                                ?: 0.0,
                            locationName = entry.locationName.ifBlank {
                                child.child("locationName").getValue(String::class.java).orEmpty()
                            },
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
                reflectionListFragment.updateEntries(reflectionEntries)
            }
            .addOnFailureListener {
                val cachedProfile = LocalStorageService.getProfile(this, currentUser.uid)
                val cachedReflections = LocalStorageService.getReflections(this, currentUser.uid)

                val displayName = cachedProfile?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName
                    ?: getString(R.string.home_empty_name)

                greetingText.text = getString(R.string.home_greeting, displayName)
                reflectionListFragment.updateEntries(cachedReflections.sortedByDescending { entry -> entry.createdAt })

                Toast.makeText(this, getString(R.string.message_profile_loaded_from_device), Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadQuoteOfTheDay() {
        RetrofitClient.quoteApi.getQuoteOfTheDay().enqueue(object : Callback<List<Quote>> {
            override fun onResponse(call: Call<List<Quote>>, response: Response<List<Quote>>) {
                val quote = response.body()?.firstOrNull() ?: return
                if (quote.text.isNotBlank()) {
                    cardQuote.visibility = View.VISIBLE
                    textQuote.text = "\"${quote.text}\""
                    textQuoteAuthor.text = "— ${quote.author}"
                }
            }

            override fun onFailure(call: Call<List<Quote>>, t: Throwable) {
                // Silently fail - quote is optional
            }
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reflection_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun subscribeToPushNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("daily_reflections")
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
