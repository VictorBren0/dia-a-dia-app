package dev.victorbreno.diaadia.services

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.DiaryProfile

object GoogleAuthHelper {
    private fun getWebClientId(context: Context): String? {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

        if (resourceId == 0) {
            return null
        }

        val value = context.getString(resourceId)
        return value.takeIf { it.isNotBlank() }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient? {
        val webClientId = getWebClientId(context) ?: return null
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, googleSignInOptions)
    }

    fun signOut(context: Context) {
        getGoogleSignInClient(context)?.signOut()
    }

    fun ensureUserProfile(
        context: Context,
        user: FirebaseUser,
        onComplete: (Boolean) -> Unit
    ) {
        val usersPath = context.getString(R.string.firebase_database_users_path)
        val userRef = FirebaseConfiguration.getFirebaseDatabase().child(usersPath).child(user.uid)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onComplete(true)
                    return@addOnSuccessListener
                }

                val profile = DiaryProfile(
                    uid = user.uid,
                    displayName = user.displayName.orEmpty(),
                    email = user.email.orEmpty(),
                    todayFocus = context.getString(R.string.env_default_focus),
                    dailyReflection = context.getString(R.string.env_default_reflection)
                )

                userRef.setValue(profile)
                    .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}