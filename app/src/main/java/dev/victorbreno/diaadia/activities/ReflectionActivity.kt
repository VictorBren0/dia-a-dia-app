package dev.victorbreno.diaadia.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReflectionActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()
    private lateinit var reflectionInput: TextInputEditText

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
        toolbar.title = "Criar Reflexão"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reflectionInput = findViewById(R.id.editTextReflection)
        loadExistingReflection()
    }

    private fun loadExistingReflection() {
        val currentUser = firebaseAuth.currentUser ?: return
        val usersPath = getString(R.string.firebase_database_users_path)
        firebaseDatabase.child(usersPath).child(currentUser.uid).child("dailyReflection").get()
            .addOnSuccessListener {
                val existing = it.getValue(String::class.java)
                if (!existing.isNullOrBlank()) {
                    reflectionInput.setText(existing)
                }
            }
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

    fun saveReflection(view: View) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.message_user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val reflectionText = reflectionInput.text?.toString()?.trim() ?: ""
        if (reflectionText.isEmpty()) {
            Toast.makeText(this, "A reflexão não pode estar vazia.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val dateString = dateFormat.format(Date())

        val usersPath = getString(R.string.firebase_database_users_path)
        val userRef = firebaseDatabase.child(usersPath).child(currentUser.uid)

        val updates = mapOf(
            "dailyReflection" to reflectionText,
            "reflectionDate" to dateString
        )

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Reflexão salva com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar a reflexão.", Toast.LENGTH_SHORT).show()
            }
    }
}