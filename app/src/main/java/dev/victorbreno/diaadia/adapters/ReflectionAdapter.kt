package dev.victorbreno.diaadia.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.victorbreno.diaadia.R
import dev.victorbreno.diaadia.data.ReflectionEntry

class ReflectionAdapter(
    private val entries: MutableList<ReflectionEntry> = mutableListOf(),
    private val onItemClick: (ReflectionEntry) -> Unit
) : RecyclerView.Adapter<ReflectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageReflection: ImageView = view.findViewById(R.id.imageReflection)
        val textContent: TextView = view.findViewById(R.id.textReflectionContent)
        val iconLocation: ImageView = view.findViewById(R.id.iconLocation)
        val textLocation: TextView = view.findViewById(R.id.textReflectionLocation)
        val textDate: TextView = view.findViewById(R.id.textReflectionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reflection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.textContent.text = entry.text
        holder.textDate.text = entry.formattedDate

        if (entry.photoBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(entry.photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    holder.imageReflection.setImageBitmap(bitmap)
                    holder.imageReflection.visibility = View.VISIBLE
                } else {
                    holder.imageReflection.visibility = View.GONE
                }
            } catch (_: Exception) {
                holder.imageReflection.visibility = View.GONE
            }
        } else {
            holder.imageReflection.visibility = View.GONE
        }

        if (entry.locationName.isNotBlank()) {
            holder.iconLocation.visibility = View.VISIBLE
            holder.textLocation.visibility = View.VISIBLE
            holder.textLocation.text = entry.locationName
        } else {
            holder.iconLocation.visibility = View.GONE
            holder.textLocation.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    override fun getItemCount(): Int = entries.size

    fun updateEntries(newEntries: List<ReflectionEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
}
