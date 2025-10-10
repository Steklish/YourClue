package com.example.tstproj

import android.content.Context
import java.util.Calendar
import java.util.Date

object ColorUtils {
    fun getColorForDate(context: Context, date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday is 1, Saturday is 7

        val colorArray = context.resources.getIntArray(R.array.week_gradient)
        val colorIndex = (dayOfWeek - 1).coerceIn(0, colorArray.size - 1)

        return colorArray[colorIndex]
    }
}
