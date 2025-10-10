package com.example.tstproj

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private var notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.element_note_preview, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteText.text = note.text
        holder.noteDate.text = dateFormat.format(note.creationDate)

        val color = ColorUtils.getColorForDate(holder.itemView.context, note.relatedDate)
        (holder.colorIndicator.background as? GradientDrawable)?.setColor(color)

        note.icon?.let {
            holder.noteIcon.setImageResource(it)
            holder.noteIcon.visibility = View.VISIBLE
        } ?: run {
            holder.noteIcon.visibility = View.GONE
        }

        holder.deleteButton.setOnClickListener { onDelete(note) }
        holder.itemView.setOnClickListener { onNoteClick(note) }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes.toMutableList()
        notifyDataSetChanged()
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.note_preview_text)
        val colorIndicator: View = itemView.findViewById(R.id.note_color_indicator)
        val noteDate: TextView = itemView.findViewById(R.id.note_preview_date)
        val deleteButton: Button = itemView.findViewById(R.id.delete_note_button)
        val noteIcon: ImageView = itemView.findViewById(R.id.note_icon)
    }
}
