package com.example.tstproj

import java.util.Date
import java.util.UUID

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val coordinates: Coordinates,
    var text: String,
    var relatedDate: Date,
    val creationDate: Date,
    var editDate: Date,
    val references: List<String> = emptyList(), // List of other Note IDs
    val imageReference: String? = null,
    var linkedNotes: MutableList<String>? = null,
    var icon: Int? = null
)
