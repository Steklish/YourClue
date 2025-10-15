package com.example.tstproj

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tstproj.databinding.ActivityCalendarSelectFiltersBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CalendarSelectFiltersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarSelectFiltersBinding
    private lateinit var storageHandler: JsonStorage
    private var allNotes: MutableList<Note> = mutableListOf()
    private lateinit var noteAdapter: NoteAdapter
    private var dataChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarSelectFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageHandler = LocalStorageHandler(this)
        loadNotes()
        noteAdapter = NoteAdapter(allNotes.toMutableList(), {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("noteId", it.id)
            startActivity(intent)
        }, {
            deleteNote(it)
        })
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.selectDateRangeButton.setOnClickListener {
            // Get default dates from extras or use current month/today
            val defaultStartDate = if(intent.hasExtra("lastSelectedStartDate")) {
                intent.getLongExtra("lastSelectedStartDate", 0)
            } else {
                MaterialDatePicker.thisMonthInUtcMilliseconds()
            }
            val defaultEndDate = if(intent.hasExtra("lastSelectedEndDate")) {
                intent.getLongExtra("lastSelectedEndDate", 0)
            } else {
                MaterialDatePicker.todayInUtcMilliseconds()
            }
            
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.CustomCalendar)
                .setTitleText("Select dates")
                .setSelection(Pair(defaultStartDate, defaultEndDate))
                .build()

            dateRangePicker.addOnPositiveButtonClickListener { range ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate = Date(range.first)
                val endDate = Date(range.second)
                binding.selectedDateRange.text = "Selected range: ${sdf.format(startDate)} to ${sdf.format(endDate)}"
                filterNotesByDate(startDate, endDate)
                
                // Pass the selected dates back to MainActivity
                val resultIntent = Intent()
                resultIntent.putExtra("startDate", startDate.time)
                resultIntent.putExtra("endDate", endDate.time)
                setResult(Activity.RESULT_OK, resultIntent)
            }

            dateRangePicker.show(supportFragmentManager, "dateRangePicker")
        }
        
        binding.clearFiltersButton.setOnClickListener {
            // Clear the date filters by passing null values back to MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("startDate", 0L) // 0 indicates null/empty
            resultIntent.putExtra("endDate", 0L)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Close the activity to return to main
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (dataChanged) {
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    private fun loadNotes() {
        val type = object : TypeToken<List<Note>>() {}.type
        val loadedNotes: List<Note>? = storageHandler.readJsonFromFile("notes.json", type)
        loadedNotes?.let {
            allNotes = it.toMutableList()
            allNotes.forEach { note ->
                if (note.linkedNotes == null) {
                    note.linkedNotes = mutableListOf()
                }
            }
        }
    }

    private fun deleteNote(note: Note) {
        allNotes.remove(note)
        allNotes.forEach { otherNote ->
            otherNote.linkedNotes?.remove(note.id)
        }
        storageHandler.writeJsonToFile("notes.json", allNotes)
        noteAdapter.updateNotes(allNotes)
        dataChanged = true
    }

    private fun filterNotesByDate(startDate: Date, endDate: Date) {
        val filteredNotes = allNotes.filter {
            !it.relatedDate.before(startDate) && !it.relatedDate.after(endDate)
        }
        noteAdapter.updateNotes(filteredNotes)
    }
}