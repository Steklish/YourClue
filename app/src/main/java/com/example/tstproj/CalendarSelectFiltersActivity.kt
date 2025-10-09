package com.example.tstproj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.example.tstproj.databinding.ActivityCalendarSelectFiltersBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class CalendarSelectFiltersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarSelectFiltersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarSelectFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.selectDateRangeButton.setOnClickListener {
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.CustomCalendar)
                .setTitleText("Select dates")
                .setSelection(
                    Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            dateRangePicker.addOnPositiveButtonClickListener {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate = sdf.format(Date(it.first))
                val endDate = sdf.format(Date(it.second))
                binding.selectedDateRange.text = "Selected range: $startDate to $endDate"
            }

            dateRangePicker.show(supportFragmentManager, "dateRangePicker")
        }
    }
}
