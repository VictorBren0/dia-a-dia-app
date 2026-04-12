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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.fragments.ButtonFragment
import dev.victorbreno.diaadia.fragments.EmailInputFragment
import dev.victorbreno.diaadia.fragments.PasswordInputFragment
import dev.victorbreno.diaadia.services.AnalyticsService
import dev.victorbreno.diaadia.services.FirebaseConfiguration
import dev.victorbreno.diaadia.services.GoogleAuthHelper
import dev.victorbreno.diaadia.services.LocalStorageService
import dev.victorbreno.diaadia.utils.Validations

@Suppress("DEPRECATION")
class RegisterActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseConfiguration.getFirebaseAuth()
    private val firebaseDatabase = FirebaseConfiguration.getFirebaseDatabase()

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailFragment: EmailInputFragment
    private lateinit var passwordFragment: PasswordInputFragment
    private lateinit var passwordConfirmFragment: PasswordInputFragment
    private lateinit var buttonFragment: ButtonFragment

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
                                val cachedProfile = DiaryProfile(
                                    uid = user.uid,
                                    displayName = user.displayName.orEmpty(),
                                    email = user.email.orEmpty(),
                                    todayFocus = getString(R.string.env_default_focus),
                                    dailyReflection = getString(R.string.env_default_reflection)
                                )
                                LocalStorageService.saveProfile(this, cachedProfile)
                                AnalyticsService.init(this)
                                AnalyticsService.logSignUp("google")
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
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameInput = findViewById(R.id.editTextName)
        emailFragment = supportFragmentManager.findFragmentById(R.id.fragmentEmail) as EmailInputFragment
        passwordFragment = supportFragmentManager.findFragmentById(R.id.fragmentPassword) as PasswordInputFragment
        passwordConfirmFragment = supportFragmentManager.findFragmentById(R.id.fragmentPasswordConfirm) as PasswordInputFragment

        buttonFragment = supportFragmentManager.findFragmentById(R.id.fragmentButtonSignUp) as ButtonFragment
        buttonFragment.setText(getString(R.string.register))
        buttonFragment.setOnClickListener { signUp() }

        // Definir hint diferente para o campo de confirmação de senha
        passwordConfirmFragment.view?.post {
            passwordConfirmFragment.getEditText().hint = getString(R.string.password_confirm)
        }
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

    private fun setLoadingState(isLoading: Boolean) {
        buttonFragment.setEnabled(!isLoading)
        buttonFragment.setText(if (isLoading) "Carregando..." else getString(R.string.register))
    }

    private fun signUp() {
        val emailInput = emailFragment.getEditText()
        val passwordInput = passwordFragment.getEditText()
        val passwordConfirmInput = passwordConfirmFragment.getEditText()

        if (!Validations.validateUserInputs(this, emailInput, passwordInput, passwordConfirmInput, nameInput)) return

        setLoadingState(true)

        val email = emailFragment.getEmail()
        val password = passwordFragment.getPassword()
        val displayName = nameInput.text.toString().trim()
        val usersPath = getString(R.string.firebase_database_users_path)

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser ?: task.result?.user
                    if (user == null) {
                        setLoadingState(false)
                        return@addOnCompleteListener
                    }
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    user.updateProfile(profileUpdates).addOnCompleteListener updateProfileListener@{ updateProfileTask ->
                        if (!updateProfileTask.isSuccessful) {
                            setLoadingState(false)
                            Toast.makeText(this@RegisterActivity, getString(R.string.message_register_error), Toast.LENGTH_SHORT).show()
                            return@updateProfileListener
                        }

                        val profile = DiaryProfile(
                            uid = user.uid,
                            displayName = displayName,
                            email = email,
                            todayFocus = getString(R.string.env_default_focus),
                            dailyReflection = getString(R.string.env_default_reflection)
                        )

                        firebaseDatabase.child(usersPath).child(user.uid).setValue(profile)
                            .addOnCompleteListener { dbTask ->
                                setLoadingState(false)
                                if (dbTask.isSuccessful) {
                                    LocalStorageService.saveProfile(this@RegisterActivity, profile)
                                    AnalyticsService.init(this@RegisterActivity)
                                    AnalyticsService.logSignUp("email")
                                    Toast.makeText(this@RegisterActivity, getString(R.string.message_register_success), Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this@RegisterActivity, getString(R.string.message_register_error), Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    setLoadingState(false)
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> getString(R.string.error_password_should_be_at_least_6_characters)
                        is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_email_not_valid)
                        is FirebaseAuthUserCollisionException -> "Usuário já cadastrado."
                        else -> task.exception?.localizedMessage ?: getString(R.string.message_register_error)
                    }
                    Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun goToLoginPage(view: View) {
        view.isEnabled = false
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun signUpWithGoogle(view: View) {
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
