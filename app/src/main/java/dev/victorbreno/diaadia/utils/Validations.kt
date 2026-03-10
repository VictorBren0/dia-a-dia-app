package dev.victorbreno.diaadia.utils

import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R

class Validations {
    companion object {
        @JvmStatic
        fun validateUserInputs(
            context: Context,
            emailInput: TextInputEditText,
            passwordInput: TextInputEditText,
            passwordConfirmInput: TextInputEditText? = null,
            nameInput: TextInputEditText? = null,
        ) : Boolean {
            if (nameInput != null) {
                if (nameInput.text.isNullOrEmpty() || nameInput.text.isNullOrBlank()) {
                    nameInput.error = context.getString(R.string.error_name_required)
                    return false
                }
            }

            val emailText = emailInput.text
            if (emailText.isNullOrEmpty() || emailText.isNullOrBlank()) {
                emailInput.error = context.getString(R.string.error_email_required)
                return false
            }

            val patternEmail = Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}\$")
            if (!patternEmail.matches(emailText.toString())) {
                emailInput.error = context.getString(R.string.error_email_not_valid)
                return false
            }
            if (passwordInput.text.isNullOrEmpty() || passwordInput.text.isNullOrBlank()) {
                passwordInput.error = context.getString(R.string.error_password_required)
                return false
            }

            if (passwordInput.text!!.length < 8) {
                passwordInput.error = context.getString(R.string.error_password_should_be_at_least_6_characters)
                return false
            }

            if (passwordConfirmInput != null) {
                if (passwordConfirmInput.text.isNullOrEmpty() || passwordConfirmInput.text.isNullOrBlank()) {
                    passwordConfirmInput.error = context.getString(R.string.error_password_confirm_required)
                    return false
                }
                if (passwordConfirmInput.text.toString() != passwordInput.text.toString()) {
                    passwordConfirmInput.error = context.getString(R.string.error_password_should_be_equal)
                    return false
                }
            }


            return true
        }
    }
}