package dev.victorbreno.diaadia.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.GoogleAuthHelper
import dev.victorbreno.diaadia.services.LocalStorageService
import dev.victorbreno.diaadia.utils.Validations

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data == null) {
            Toast.makeText(this, getString(R.string.message_google_sign_in_cancelled), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.message_google_sign_in_error), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener {
                    val user = firebaseAuth.currentUser
                    if (user == null) {
                        Toast.makeText(this, getString(R.string.message_google_sign_in_error), Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    GoogleAuthHelper.ensureUserProfile(this, user) { profileSaved ->
                        runOnUiThread {
                            if (profileSaved) {
                                LocalStorageService.saveUserSession(
                                    this,
                                    user.uid,
                                    user.displayName.orEmpty(),
                                    user.email.orEmpty()
                                )
                                Toast.makeText(this, getString(R.string.message_google_sign_in_success), Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, getString(R.string.message_register_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.message_google_sign_in_error), Toast.LENGTH_SHORT).show()
                }
        } catch (exception: ApiException) {
            Toast.makeText(this, getString(R.string.message_google_sign_in_error), Toast.LENGTH_SHORT).show()
        }
    }

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
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            LocalStorageService.saveUserSession(
                this,
                currentUser.uid,
                currentUser.displayName.orEmpty(),
                currentUser.email.orEmpty()
            )
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun signIn(view: View) {
        if (!Validations.validateUserInputs(this, emailInput, passwordInput)) return

        view.isEnabled = false

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        LocalStorageService.saveUserSession(
                            this,
                            currentUser.uid,
                            currentUser.displayName.orEmpty(),
                            currentUser.email.orEmpty()
                        )
                    }
                    Toast.makeText(this, getString(R.string.message_login_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    view.isEnabled = true
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
        view.isEnabled = false
        val email = emailInput.text?.toString()?.trim().orEmpty()
        if (email.isBlank()) {
            emailInput.error = getString(R.string.message_invalid_email_for_reset)
            view.isEnabled = true
            return
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                view.isEnabled = true
                Toast.makeText(this, getString(R.string.message_forgot_password_success), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                view.isEnabled = true
                Toast.makeText(this, getString(R.string.message_invalid_email_for_reset), Toast.LENGTH_LONG).show()
            }
    }

    fun goToRegisterPage(view: View) {
        view.isEnabled = false
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }

    fun signInWithGoogle(view: View) {
        val googleClient = GoogleAuthHelper.getGoogleSignInClient(this)
        if (googleClient == null) {
            Toast.makeText(this, getString(R.string.message_google_config_missing), Toast.LENGTH_LONG).show()
            return
        }

        view.isEnabled = false
        googleSignInLauncher.launch(googleClient.signInIntent)
        view.postDelayed({ view.isEnabled = true }, 1200)
    }
}