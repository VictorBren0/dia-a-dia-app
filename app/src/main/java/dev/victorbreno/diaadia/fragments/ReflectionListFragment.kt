package dev.victorbreno.diaadia.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.activities.ReflectionActivity
import dev.victorbreno.diaadia.adapters.ReflectionAdapter
import dev.victorbreno.diaadia.data.ReflectionEntry

class ReflectionListFragment : Fragment() {

    private lateinit var recyclerReflections: RecyclerView
    private lateinit var textEmptyState: TextView
    private lateinit var reflectionAdapter: ReflectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reflection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerReflections = view.findViewById(R.id.recyclerReflections)
        textEmptyState = view.findViewById(R.id.textEmptyState)

        reflectionAdapter = ReflectionAdapter { entry -> openReflectionForEdit(entry) }
        recyclerReflections.layoutManager = LinearLayoutManager(requireContext())
        recyclerReflections.adapter = reflectionAdapter
    }

    fun updateEntries(entries: List<ReflectionEntry>) {
        if (!::reflectionAdapter.isInitialized) return

        if (entries.isEmpty()) {
            recyclerReflections.visibility = View.GONE
            textEmptyState.visibility = View.VISIBLE
        } else {
            recyclerReflections.visibility = View.VISIBLE
            textEmptyState.visibility = View.GONE
            reflectionAdapter.updateEntries(entries)
        }
    }

    private fun openReflectionForEdit(entry: ReflectionEntry) {
        val intent = Intent(requireContext(), ReflectionActivity::class.java).apply {
            putExtra("ENTRY_ID", entry.id)
            putExtra("ENTRY_TEXT", entry.text)
            putExtra("ENTRY_PHOTO", entry.photoBase64)
            putExtra("ENTRY_LATITUDE", entry.latitude)
            putExtra("ENTRY_LONGITUDE", entry.longitude)
            putExtra("ENTRY_LOCATION", entry.locationName)
            putExtra("ENTRY_CREATED_AT", entry.createdAt)
            putExtra("ENTRY_FORMATTED_DATE", entry.formattedDate)
        }
        startActivity(intent)
    }
}
