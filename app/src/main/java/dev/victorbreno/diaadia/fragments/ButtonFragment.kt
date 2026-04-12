package dev.victorbreno.diaadia.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dev.victorbreno.diaadia.R

class ButtonFragment : Fragment() {

    private lateinit var button: Button
    private var clickListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_button, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button = view.findViewById(R.id.fragmentButton)

        val text = arguments?.getString(ARG_TEXT).orEmpty()
        button.text = text

        button.setOnClickListener { clickListener?.invoke() }
    }

    fun setOnClickListener(listener: () -> Unit) {
        clickListener = listener
        if (::button.isInitialized) {
            button.setOnClickListener { listener() }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (::button.isInitialized) {
            button.isEnabled = enabled
        }
    }

    fun setText(text: String) {
        if (::button.isInitialized) {
            button.text = text
        }
    }

    companion object {
        private const val ARG_TEXT = "text"

        fun newInstance(text: String): ButtonFragment {
            return ButtonFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
        }
    }
}
