package dev.victorbreno.diaadia.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import dev.victorbreno.diaadia.R

class EmailInputFragment : Fragment() {

    private lateinit var editTextEmail: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_email_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editTextEmail = view.findViewById(R.id.editTextEmail)
    }

    fun getEmail(): String = editTextEmail.text?.toString()?.trim().orEmpty()

    fun getEditText(): TextInputEditText = editTextEmail

    fun setEmail(email: String) {
        editTextEmail.setText(email)
    }

    fun setError(error: String?) {
        editTextEmail.error = error
    }
}
