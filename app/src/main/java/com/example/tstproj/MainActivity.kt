package com.example.tstproj

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.location.Location
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tstproj.BuildConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.reflect.TypeToken
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.BitmapUtils
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Date

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var maplibreMap: MapLibreMap

    private lateinit var createNoteButton: Button
    private lateinit var popUp: ConstraintLayout
    private lateinit var container: ConstraintLayout
    private lateinit var blurredBackground: ImageView
    var styleUrl: String? = null
    private var selectedLocation: LatLng? = null
    private lateinit var storageHandler: JsonStorage
    private var notes: MutableList<Note> = mutableListOf()
    private var editingNote: Note? = null
    private var linkingNote: Note? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val calendarActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadNotes()
            showMarkersAndLinks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity)

        storageHandler = LocalStorageHandler(this)
        loadNotes()

        createNoteButton = findViewById(R.id.createMarkerButton)
        popUp = findViewById(R.id.popup)
        container = findViewById(R.id.container)
        blurredBackground = findViewById(R.id.blurred_background)

        createNoteButton.isEnabled = false
        createNoteButton.alpha = 0.5f

        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "backdrop"
        styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"
        Log.d("MyAppTag", styleUrl!!)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    popUp.visibility == View.VISIBLE -> {
                        hideEditPopup()
                    }
                    selectedLocation != null -> {
                        removeMarker("temp_marker")
                        selectedLocation = null
                        createNoteButton.isEnabled = false
                        createNoteButton.alpha = 0.5f
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setListeners()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestLocationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = insets.top
            mapView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }
            windowInsets
        }
    }

    fun setListeners() {
        createNoteButton.setOnClickListener {
            editingNote = null
            showEditPopup()
        }
        popUp.findViewById<MaterialButton>(R.id.close_popup_button).setOnClickListener {
            hideEditPopup()
        }
        popUp.findViewById<Button>(R.id.save_note_button).setOnClickListener {
            saveNote()
        }
        popUp.findViewById<MaterialButton>(R.id.related_date_button).setOnClickListener {
            showDatePicker()
        }
        popUp.findViewById<MaterialButton>(R.id.set_icon_button).setOnClickListener {
            showIconPicker()
        }
        container.findViewById<MaterialButton>(R.id.find_self_button).setOnClickListener {
            zoomToSelf()
        }
        container.findViewById<MaterialButton>(R.id.zoom_in_button).setOnClickListener {
            if (::maplibreMap.isInitialized) {
                maplibreMap.animateCamera(CameraUpdateFactory.zoomTo(maplibreMap.zoom + 1f), 200)
            }
        }
        container.findViewById<MaterialButton>(R.id.zoom_out_button).setOnClickListener {
            if (::maplibreMap.isInitialized) {
                maplibreMap.animateCamera(CameraUpdateFactory.zoomTo(maplibreMap.zoom - 1f), 200)
            }
        }
        container.findViewById<MaterialButton>(R.id.open_calendar_button).setOnClickListener {
            val intent = Intent(this, CalendarSelectFiltersActivity::class.java)
            calendarActivityResultLauncher.launch(intent)
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        this.maplibreMap = map
        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "backdrop"
        val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"

        getLocationWithCallback { location ->
            map.setStyle(styleUrl) {
                if (location != null) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(15.0)
                        .build()
                    addMarker(
                        location,
                        0.08f,
                        ContextCompat.getColor(this, R.color.primary_color),
                        "self_marker"
                    )
                } else {
                    somethingWentWrongInfoMessage()
                }
                showMarkersAndLinks()
            }
        }

        map.addOnMapClickListener { point ->
            if (popUp.visibility == View.VISIBLE) return@addOnMapClickListener false

            val screenPoint = map.projection.toScreenLocation(point)
            val noteLayerIds = notes.map { "note-${it.id}-layer" }.toTypedArray()
            val features = map.queryRenderedFeatures(screenPoint, *noteLayerIds)

            if (linkingNote != null) {
                if (features.isNotEmpty()) {
                    val featureId = features[0].getStringProperty("id")
                    if (featureId != null) {
                        val noteId = featureId.substringAfter("note-")
                        val clickedNote = notes.find { it.id == noteId }
                        if (clickedNote != null && clickedNote.id != linkingNote!!.id) {
                            if (linkingNote!!.linkedNotes?.contains(clickedNote.id) == false) {
                                linkingNote!!.linkedNotes?.add(clickedNote.id)
                            }
                            if (clickedNote.linkedNotes?.contains(linkingNote!!.id) == false) {
                                clickedNote.linkedNotes?.add(linkingNote!!.id)
                            }
                            storageHandler.writeJsonToFile("notes.json", notes)
                            drawLinks()
                            Toast.makeText(this, "Notes linked!", Toast.LENGTH_SHORT).show()
                            linkingNote = null
                            return@addOnMapClickListener true
                        }
                    }
                }
                linkingNote = null
                Toast.makeText(this, "Link creation cancelled", Toast.LENGTH_SHORT).show()
                return@addOnMapClickListener false
            }

            if (features.isNotEmpty()) {
                val featureId = features[0].getStringProperty("id")
                if (featureId != null) {
                    val noteId = featureId.substringAfter("note-")
                    val note = notes.find { it.id == noteId }
                    if (note != null) {
                        editingNote = note
                        showEditPopup()
                        return@addOnMapClickListener true
                    }
                }
            }

            selectedLocation = point
            val tempLocation = Location("")
            tempLocation.latitude = point.latitude
            tempLocation.longitude = point.longitude
            addMarker(tempLocation, 0.08f, ContextCompat.getColor(this, R.color.red), "temp_marker")
            createNoteButton.isEnabled = true
            createNoteButton.alpha = 1.0f
            true
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    private fun blurBitmap(context: Context, bitmap: Bitmap): Bitmap {
        val outputBitmap = Bitmap.createBitmap(bitmap)
        val renderScript = RenderScript.create(context)
        val tmpIn = Allocation.createFromBitmap(renderScript, bitmap)
        val tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap)
        val blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        blur.setRadius(20f)
        blur.setInput(tmpIn)
        blur.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)
        renderScript.destroy()
        return outputBitmap
    }

    fun showEditPopup() {
        if (!::maplibreMap.isInitialized) {
            Log.w("MainActivity", "Map is not ready yet, cannot show popup.")
            return
        }

        val noteTextInput = popUp.findViewById<EditText>(R.id.note_text_input)
        val linkButton = popUp.findViewById<Button>(R.id.link_note_button)
        val linkedNotesRecyclerView = popUp.findViewById<RecyclerView>(R.id.linked_notes_recycler_view)
        val noLinkedNotesText = popUp.findViewById<TextView>(R.id.no_linked_notes_text)
        val iconPreview = popUp.findViewById<ImageView>(R.id.icon_preview)

        if (editingNote != null) {
            noteTextInput.setText(editingNote!!.text)
            popUp.findViewById<Button>(R.id.save_note_button).isEnabled = true
            linkButton.visibility = View.VISIBLE
            linkButton.setOnClickListener {
                linkingNote = editingNote
                hideEditPopup()
                Toast.makeText(this, "Tap another note to create a link", Toast.LENGTH_SHORT).show()
            }

            val linkedNotes = editingNote!!.linkedNotes?.mapNotNull { getNoteById(it) } ?: emptyList()
            if (linkedNotes.isNotEmpty()) {
                val adapter = LinkedNoteAdapter(linkedNotes.toMutableList()) { unlinkedNote ->
                    editingNote?.linkedNotes?.remove(unlinkedNote.id)
                    unlinkedNote.linkedNotes?.remove(editingNote!!.id)
                    storageHandler.writeJsonToFile("notes.json", notes)
                    showMarkersAndLinks()
                    showEditPopup() // Refresh the popup
                }
                linkedNotesRecyclerView.adapter = adapter
                linkedNotesRecyclerView.layoutManager = LinearLayoutManager(this)
                linkedNotesRecyclerView.visibility = View.VISIBLE
                noLinkedNotesText.visibility = View.GONE
            } else {
                linkedNotesRecyclerView.visibility = View.GONE
                noLinkedNotesText.visibility = View.VISIBLE
            }

            editingNote!!.icon?.let {
                iconPreview.setImageResource(it)
                iconPreview.visibility = View.VISIBLE
            }
        } else {
            noteTextInput.text.clear()
            popUp.findViewById<Button>(R.id.save_note_button).isEnabled = selectedLocation != null
            linkButton.visibility = View.GONE
            linkedNotesRecyclerView.visibility = View.GONE
            noLinkedNotesText.visibility = View.GONE
            iconPreview.visibility = View.GONE
        }

        maplibreMap.snapshot { snapshotBitmap ->
            val blurredBitmap = blurBitmap(this@MainActivity, snapshotBitmap)
            blurredBackground.setImageBitmap(blurredBitmap)
            blurredBackground.alpha = 0f
            blurredBackground.visibility = View.VISIBLE
            blurredBackground.animate().alpha(1f).setDuration(300).setListener(null)

            popUp.alpha = 0f
            popUp.scaleX = 0.8f
            popUp.scaleY = 0.8f
            popUp.visibility = View.VISIBLE
            popUp.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setListener(null)
        }
    }

    fun hideEditPopup() {
        blurredBackground.animate().alpha(0f).setDuration(100).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                blurredBackground.visibility = View.GONE
                blurredBackground.setImageBitmap(null)
            }
        })

        popUp.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                popUp.visibility = View.GONE
                removeMarker("temp_marker")
                selectedLocation = null
                editingNote = null
                createNoteButton.isEnabled = false
                createNoteButton.alpha = 0.5f
            }
        })
    }

    fun addMarker(position: Location, size: Float, @ColorInt color: Int, sourceId: String, icon: Int? = null) {
        val latitude: Double = position.latitude
        val longitude: Double = position.longitude
        val uniqueIconId = if (icon != null) "marker-icon-$icon" else "marker-icon-$color"

        maplibreMap.getStyle { style ->
            if (style.getImage(uniqueIconId) == null) {
                val drawable = if (icon != null) {
                    val background = ResourcesCompat.getDrawable(resources, R.drawable.icon_background, null)!!
                    val iconDrawable = ResourcesCompat.getDrawable(resources, icon, null)!!
                    val layerDrawable = LayerDrawable(arrayOf(background, iconDrawable))
                    layerDrawable
                } else {
                    val originalDrawable: Drawable = ResourcesCompat.getDrawable(resources, R.drawable.geo_marker, null)!!
                    val wrappedDrawable = DrawableCompat.wrap(originalDrawable).mutate()
                    DrawableCompat.setTint(wrappedDrawable, color)
                    wrappedDrawable
                }
                val bitmap = BitmapUtils.getBitmapFromDrawable(drawable)!!
                style.addImage(uniqueIconId, bitmap)
            }

            val geoJson = """{"type":"FeatureCollection","features":[{"type":"Feature","id":"$sourceId","geometry":{"type":"Point","coordinates":[$longitude,$latitude]},"properties":{"icon":"$uniqueIconId","id":"$sourceId"}}]}"""
            val source = style.getSource(sourceId) as? GeoJsonSource
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, geoJson))
            } else {
                source.setGeoJson(geoJson)
            }

            val layerId = "$sourceId-layer"
            if (style.getLayer(layerId) == null) {
                val symbolLayer = SymbolLayer(layerId, sourceId).withProperties(
                    iconImage(get("icon")),
                    iconSize(size),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM)
                )
                style.addLayer(symbolLayer)
            }
        }
    }

    private fun removeMarker(sourceId: String) {
        maplibreMap.getStyle { style ->
            val layerId = "$sourceId-layer"
            style.getLayer(layerId)?.let { style.removeLayer(it) }
            style.getSource(sourceId)?.let { style.removeSource(it) }
        }
    }

    private fun drawLinks() {
        maplibreMap.getStyle { style ->
            val layersToRemove = style.layers.filter { it.id.startsWith("link-layer-") }
            val sourcesToRemove = style.sources.filter { it.id.startsWith("link-source-") }
            layersToRemove.forEach { style.removeLayer(it) }
            sourcesToRemove.forEach { style.removeSource(it) }

            val drawnLinks = mutableSetOf<String>()

            notes.forEach { note1 ->
                note1.linkedNotes?.forEach { note2Id ->
                    val note2 = notes.find { it.id == note2Id }
                    if (note2 != null) {
                        val linkKey = if (note1.id < note2.id) "${note1.id}-${note2.id}" else "${note2.id}-${note1.id}"
                        if (!drawnLinks.contains(linkKey)) {
                            drawLineBetweenNotes(note1, note2)
                            drawnLinks.add(linkKey)
                        }
                    }
                }
            }
        }
    }

    private fun drawLineBetweenNotes(note1: Note, note2: Note) {
        maplibreMap.getStyle { style ->
            val linkId = "link-${note1.id}-${note2.id}"
            val sourceId = "link-source-$linkId"
            val layerId = "link-layer-$linkId"

            val lineString = LineString.fromLngLats(listOf(
                Point.fromLngLat(note1.coordinates.longitude, note1.coordinates.latitude),
                Point.fromLngLat(note2.coordinates.longitude, note2.coordinates.latitude)
            ))

            val geoJsonSource = GeoJsonSource(sourceId, lineString)
            style.addSource(geoJsonSource)

            val color1 = ColorUtils.getColorForDate(this, note1.relatedDate)
            val color2 = ColorUtils.getColorForDate(this, note2.relatedDate)

            val lineLayer = LineLayer(layerId, sourceId).withProperties(lineWidth(2f))

            if (color1 == color2) {
                lineLayer.setProperties(lineColor(color1))
            } else {
                lineLayer.setProperties(
                    lineGradient(
                        interpolate(
                            linear(),
                            lineProgress(),
                            stop(0, color(color1)),
                            stop(1, color(color2))
                        )
                    )
                )
            }

            style.addLayer(lineLayer)
        }
    }

    private fun showMarkersAndLinks() {
        if (!::maplibreMap.isInitialized) return
        maplibreMap.getStyle { style ->
            val layersToRemove = style.layers.filter { it.id.startsWith("note-") || it.id.startsWith("link-layer-") }
            val sourcesToRemove = style.sources.filter { it.id.startsWith("note-") || it.id.startsWith("link-source-") }
            layersToRemove.forEach { style.removeLayer(it) }
            sourcesToRemove.forEach { style.removeSource(it) }

            notes.forEach { note ->
                val noteLocation = Location("")
                noteLocation.latitude = note.coordinates.latitude
                noteLocation.longitude = note.coordinates.longitude
                val color = ColorUtils.getColorForDate(this, note.relatedDate)
                addMarker(noteLocation, 0.08f, color, "note-${note.id}", note.icon)
            }

            drawLinks()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {}
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {}
            else -> {}
        }
    }

    fun checkAndRequestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {}
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {}
            else -> {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWithCallback(onLocationReady: (Location?) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            onLocationReady(location)
        }.addOnFailureListener { e ->
            Log.e("LocationError", "Failed to get location.", e)
            onLocationReady(null)
        }
    }

    private fun somethingWentWrongInfoMessage() {
        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
    }

    private fun zoomToSelf() {
        getLocationWithCallback { location ->
            if (location != null) {
                maplibreMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(15.0)
                    .build()
            } else {
                somethingWentWrongInfoMessage()
            }
            Toast.makeText(this, "Found you!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote() {
        val noteText = popUp.findViewById<EditText>(R.id.note_text_input).text.toString()
        if (noteText.isBlank()) {
            Toast.makeText(this, "Note text cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (editingNote != null) {
            editingNote!!.text = noteText
            editingNote!!.editDate = Date()
            removeMarker("note-${editingNote!!.id}")
            val noteLocation = Location("")
            noteLocation.latitude = editingNote!!.coordinates.latitude
            noteLocation.longitude = editingNote!!.coordinates.longitude
            addMarker(noteLocation, 0.08f, ColorUtils.getColorForDate(this, editingNote!!.relatedDate), "note-${editingNote!!.id}", editingNote!!.icon)
        } else {
            selectedLocation?.let { loc ->
                val newNote = Note(
                    coordinates = Coordinates(loc.latitude, loc.longitude),
                    text = noteText,
                    relatedDate = Date(),
                    creationDate = Date(),
                    editDate = Date(),
                    linkedNotes = mutableListOf(),
                    icon = editingNote?.icon
                )
                notes.add(newNote)
                val noteLocation = Location("")
                noteLocation.latitude = newNote.coordinates.latitude
                noteLocation.longitude = newNote.coordinates.longitude
                addMarker(noteLocation, 0.08f, ColorUtils.getColorForDate(this, newNote.relatedDate), "note-${newNote.id}", newNote.icon)
            }
        }
        storageHandler.writeJsonToFile("notes.json", notes)
        drawLinks()
        hideEditPopup()
    }

    private fun loadNotes() {
        val type = object : TypeToken<List<Note>>() {}.type
        val loadedNotes: List<Note>? = storageHandler.readJsonFromFile("notes.json", type)
        loadedNotes?.let {
            notes = it.toMutableList()
            notes.forEach { note ->
                if (note.linkedNotes == null) {
                    note.linkedNotes = mutableListOf()
                }
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
        datePicker.addOnPositiveButtonClickListener { date ->
            val selectedDate = Date(date)
            if (editingNote != null) {
                editingNote!!.relatedDate = selectedDate
                saveNote()
            } else {
                selectedLocation?.let {
                    val tempLocation = Location("")
                    tempLocation.latitude = it.latitude
                    tempLocation.longitude = it.longitude
                    removeMarker("temp_marker")
                    addMarker(tempLocation, 0.08f, ColorUtils.getColorForDate(this, selectedDate), "temp_marker")
                }
            }
        }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun getNoteById(noteId: String): Note? {
        return notes.find { it.id == noteId }
    }

    private fun showIconPicker() {
        val drawableResources = getAllDrawableResources()
        val iconAdapter = IconAdapter(this, drawableResources)

        val dialogView = layoutInflater.inflate(R.layout.icon_picker_popup, null)
        val iconGrid = dialogView.findViewById<GridView>(R.id.icon_grid)
        iconGrid.adapter = iconAdapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Icon")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("No Icon") { _, _ ->
                editingNote?.icon = null
                showEditPopup()
            }
            .create()

        iconGrid.setOnItemClickListener { _, _, position, _ ->
            editingNote?.icon = drawableResources[position]
            dialog.dismiss()
            showEditPopup()
        }

        dialog.show()
    }

    private fun getAllDrawableResources(): List<Int> {
        val unwantedDrawables = setOf(
            R.drawable.button_background,
            R.drawable.close_icon,
            R.drawable.delete_icon,
            R.drawable.delete_location,
            R.drawable.shape_button_prime_rounded_corners,
            R.drawable.shape_rounded_colors_popup,
            R.drawable.shape_rounded_square,
            R.drawable.icon_background
        )
        return R.drawable::class.java.fields.map { it.getInt(null) }.filter { !unwantedDrawables.contains(it) }
    }
}
