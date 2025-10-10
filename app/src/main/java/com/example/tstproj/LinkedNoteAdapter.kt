package com.example.tstproj

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LinkedNoteAdapter(
    private var linkedNotes: MutableList<Note>,
    private val unlinkAction: (Note) -> Unit
) : RecyclerView.Adapter<LinkedNoteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val linkedNoteText: TextView = view.findViewById(R.id.linked_note_text)
        val unlinkButton: Button = view.findViewById(R.id.unlink_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_linked_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val linkedNote = linkedNotes[position]
        holder.linkedNoteText.text = linkedNote.text
        holder.unlinkButton.setOnClickListener { unlinkAction(linkedNote) }
    }

    override fun getItemCount() = linkedNotes.size

    fun updateLinkedNotes(newLinkedNotes: List<Note>) {
        this.linkedNotes.clear()
        this.linkedNotes.addAll(newLinkedNotes)
        notifyDataSetChanged()
    }
}