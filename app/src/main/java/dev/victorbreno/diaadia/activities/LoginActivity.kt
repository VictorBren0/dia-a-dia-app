package dev.victorbreno.diaadia.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.utils.Validations

class LoginActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.editTextEmail)
        passwordInput = findViewById(R.id.editTextPassword)
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun signIn(view: View) {
        if (!Validations.validateUserInputs(this, emailInput, passwordInput)) return

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.message_login_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException,
                        is FirebaseAuthInvalidUserException -> getString(R.string.error_email_not_valid)
                        else -> task.exception?.localizedMessage ?: getString(R.string.message_user_not_authenticated)
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun forgotPassword(view: View) {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        if (email.isBlank()) {
            emailInput.error = getString(R.string.message_invalid_email_for_reset)
            return
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.message_forgot_password_success), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.message_invalid_email_for_reset), Toast.LENGTH_LONG).show()
            }
    }

    fun goToRegisterPage(view: View) {
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }
}