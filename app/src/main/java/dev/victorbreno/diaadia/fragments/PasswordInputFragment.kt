package dev.victorbreno.diaadia.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R

class PasswordInputFragment : Fragment() {

    private lateinit var editTextPassword: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editTextPassword = view.findViewById(R.id.editTextPassword)

        val hint = arguments?.getString(ARG_HINT)
        if (!hint.isNullOrBlank()) {
            editTextPassword.hint = hint
        }
    }

    fun getPassword(): String = editTextPassword.text?.toString().orEmpty()

    fun getEditText(): TextInputEditText = editTextPassword

    fun setError(error: String?) {
        editTextPassword.error = error
    }

    companion object {
        private const val ARG_HINT = "hint"

        fun newInstance(hint: String? = null): PasswordInputFragment {
            return PasswordInputFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HINT, hint)
                }
            }
        }
    }
}
