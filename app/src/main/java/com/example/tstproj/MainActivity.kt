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
import android.graphics.Color
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.reflect.TypeToken
import org.maplibre.android.MapLibre
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.cardview.widget.CardView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.BitmapUtils
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var maplibreMap: MapLibreMap

    private lateinit var createNoteButton: Button
    private lateinit var popUp: ConstraintLayout
    private lateinit var container: ConstraintLayout
    private lateinit var blurredBackground: ImageView
    private lateinit var searchInputLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var searchInput: com.google.android.material.textfield.TextInputEditText
    var styleUrl: String? = null
    private var selectedLocation: LatLng? = null
    private lateinit var storageHandler: JsonStorage
    private var notes: MutableList<Note> = mutableListOf()
    private var searchResults: Set<String> = emptySet() // Store IDs of notes that match the search
    private var editingNote: Note? = null
    private var linkingNote: Note? = null
    private var infoWindowPopup: PopupWindow? = null
    private var currentInfoNote: Note? = null

    // Temp state for new notes
    private var newNoteIcon: Int? = null
    private var newNoteDate: Date = Date()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val calendarActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadNotes()
            // Apply current search filter if there's an active search
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch()
            } else {
                showMarkersAndLinks()
            }
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
        searchInputLayout = findViewById(R.id.search_input_layout)
        searchInput = findViewById(R.id.search_input)

        // Set up search functionality
        setupSearchFunctionality()

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
                    infoWindowPopup?.isShowing == true -> {
                        infoWindowPopup?.dismiss()
                        currentInfoNote = null
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

    private fun setupSearchFunctionality() {
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                // Add a small delay to avoid searching on every keystroke
                searchInput.removeCallbacks(searchRunnable)
                searchInput.postDelayed(searchRunnable, 300)
            }
        })
    }

    private val searchRunnable = Runnable {
        performSearch()
    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            // Show all notes normally when search is empty
            searchResults = emptySet()
        } else {
            // Find notes that match the search query
            val lowerQuery = query.lowercase()
            val matchingNotes = notes.filter { note ->
                // Check if note text contains the query
                note.text.lowercase().contains(lowerQuery) ||
                // Check if related date contains the query (searching by date)
                formatDate(note.relatedDate).lowercase().contains(lowerQuery) ||
                // Also search in creation date
                formatDate(note.creationDate).lowercase().contains(lowerQuery)
            }
            searchResults = matchingNotes.map { it.id }.toSet()

        }
        showMarkersAndLinks() // Refresh the map to show all notes with transparency for non-matching ones
        
        // Zoom to show all matching notes if search results exist
        if (query.isNotEmpty() && searchResults.isNotEmpty()) {
            // Post to run after the markers are updated to ensure the zoom works properly
            mapView.post {
                zoomToMatchingNotes()
                
                // Show info window for the first matching note
                val firstMatchingNote = notes.find { note -> searchResults.contains(note.id) }
                if (firstMatchingNote != null) {
                    // Find the LatLng for the first matching note to show its info window
                    val position = LatLng(
                        firstMatchingNote.coordinates.latitude,
                        firstMatchingNote.coordinates.longitude
                    )
                    showInfoWindowForNote(firstMatchingNote, position)
                }
            }
        }

    }

    private fun formatDate(date: Date): String {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return dateFormat.format(date)
    }
    
    private fun applyAlphaToColor(color: Int, alpha: Float): Int {
        val alphaInt = (255 * alpha).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alphaInt shl 24)
    }

    override fun onMapReady(map: MapLibreMap) {
        this.maplibreMap = map
        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "backdrop"
        val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        getLocationWithCallback { location ->
            map.setStyle(styleUrl) {
                if (location != null) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(15.0)
                        .build()
                    addMarker(
                        location,
                        0.04f,
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
            val noteLayerIds = notes.map { "note-${it.id}-background-layer" }.toTypedArray()
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
                            // Apply current search filter if there's an active search
                            val query = searchInput.text.toString().trim()
                            if (query.isNotEmpty()) {
                                performSearch()
                            } else {
                                showMarkersAndLinks()
                            }
                            Toast.makeText(this, "Notes linked!", Toast.LENGTH_SHORT).show()
                            linkingNote = null
                            return@addOnMapClickListener true
                        }
                    }
                }
                linkingNote = null
                Toast.makeText(this, "Link creation cancelled", Toast.LENGTH_SHORT).show()
                // Dismiss info window if clicking elsewhere during linking
                if (infoWindowPopup?.isShowing == true) {
                    infoWindowPopup?.dismiss()
                    currentInfoNote = null
                }
                return@addOnMapClickListener false
            }

            if (features.isNotEmpty()) {
                val featureId = features[0].getStringProperty("id")
                Log.d("InfoWindowDebug", "Feature clicked with ID: $featureId")
                if (featureId != null) {
                    val noteId = featureId.substringAfter("note-")
                    Log.d("InfoWindowDebug", "Extracted note ID: $noteId")
                    val note = notes.find { it.id == noteId }
                    if (note != null) {
                        // Show the info window for the clicked note
                        showInfoWindowForNote(note, point)
                        return@addOnMapClickListener true
                    } else {
                        Log.d("InfoWindowDebug", "Note not found for ID: $noteId")
                    }
                } else {
                    Log.d("InfoWindowDebug", "Feature ID is null")
                }
            } else {
                Log.d("InfoWindowDebug", "No features found at click location")
                // Dismiss info window if clicking elsewhere on the map
                if (infoWindowPopup?.isShowing == true) {
                    infoWindowPopup?.dismiss()
                    currentInfoNote = null
                }
            }

            selectedLocation = point
            val tempLocation = Location("")
            tempLocation.latitude = point.latitude
            tempLocation.longitude = point.longitude
            addMarker(tempLocation, 0.08f, ContextCompat.getColor(this, R.color.new_marker_color), "temp_marker", R.drawable.question_circle_svgrepo_com)
            createNoteButton.isEnabled = true
            createNoteButton.alpha = 1.0f
            true
        }

        map.addOnMapLongClickListener { point ->
            if (popUp.visibility == View.VISIBLE) return@addOnMapLongClickListener false

            val screenPoint = map.projection.toScreenLocation(point)
            val noteLayerIds = notes.map { "note-${it.id}-background-layer" }.toTypedArray()
            val features = map.queryRenderedFeatures(screenPoint, *noteLayerIds)

            if (features.isNotEmpty()) {
                val featureId = features[0].getStringProperty("id")
                if (featureId != null) {
                    val noteId = featureId.substringAfter("note-")
                    val note = notes.find { it.id == noteId }
                    if (note != null) {
                        // Dismiss the info window if a note is long-pressed for editing
                        if (infoWindowPopup?.isShowing == true) {
                            infoWindowPopup?.dismiss()
                            currentInfoNote = null
                        }
                        editingNote = note
                        showEditPopup()
                        return@addOnMapLongClickListener true
                    }
                }
            }
            false
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
        // Dismiss the info window when showing the edit popup
        if (infoWindowPopup?.isShowing == true) {
            infoWindowPopup?.dismiss()
            currentInfoNote = null
        }
        createNoteButton.visibility = View.GONE
        val noteTextInput = popUp.findViewById<EditText>(R.id.note_text_input)
        val linkButton = popUp.findViewById<Button>(R.id.link_note_button)
        val deleteButton = popUp.findViewById<Button>(R.id.delete_note_button)
        val linkedNotesRecyclerView = popUp.findViewById<RecyclerView>(R.id.linked_notes_recycler_view)
        val noLinkedNotesText = popUp.findViewById<TextView>(R.id.no_linked_notes_text)
        val iconPreview = popUp.findViewById<ImageView>(R.id.icon_preview)

        if (editingNote != null) {
            noteTextInput.setText(editingNote!!.text)
            popUp.findViewById<Button>(R.id.save_note_button).isEnabled = true
            linkButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            linkButton.setOnClickListener {
                linkingNote = editingNote
                hideEditPopup()
                Toast.makeText(this, "Tap another note to create a link", Toast.LENGTH_SHORT).show()
            }
            deleteButton.setOnClickListener {
                notes.remove(editingNote!!)
                storageHandler.writeJsonToFile("notes.json", notes)
                // Apply current search filter if there's an active search
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch()
                } else {
                    showMarkersAndLinks()
                }
                hideEditPopup()
            }

            val linkedNotes = editingNote!!.linkedNotes?.mapNotNull { getNoteById(it) } ?: emptyList()
            if (linkedNotes.isNotEmpty()) {
                val adapter = LinkedNoteAdapter(linkedNotes.toMutableList()) { unlinkedNote ->
                    editingNote?.linkedNotes?.remove(unlinkedNote.id)
                    unlinkedNote.linkedNotes?.remove(editingNote!!.id)
                    storageHandler.writeJsonToFile("notes.json", notes)
                    // Apply current search filter if there's an active search
                    val query = searchInput.text.toString().trim()
                    if (query.isNotEmpty()) {
                        performSearch()
                    } else {
                        showMarkersAndLinks()
                    }
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
            // New note
            newNoteIcon = null
            newNoteDate = Date()
            noteTextInput.text.clear()
            popUp.findViewById<Button>(R.id.save_note_button).isEnabled = selectedLocation != null
            linkButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
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
        createNoteButton.visibility = View.VISIBLE
        // Dismiss the info window when hiding the edit popup
        if (infoWindowPopup?.isShowing == true) {
            infoWindowPopup?.dismiss()
            currentInfoNote = null
        }
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

        maplibreMap.getStyle { style ->
            // Create GeoJSON source (same as before)
            val feature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
            feature.addStringProperty("id", sourceId)
            val source = style.getSource(sourceId) as? GeoJsonSource
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, feature))
            } else {
                source.setGeoJson(feature)
            }

            // Add background marker image (same as before)
            val backgroundIconId = "marker-background-$color"
            if (style.getImage(backgroundIconId) == null) {
                val backgroundDrawable = ResourcesCompat.getDrawable(resources, R.drawable.geo_marker, null)!!
                val wrappedDrawable = DrawableCompat.wrap(backgroundDrawable).mutate()
                DrawableCompat.setTint(wrappedDrawable, color)
                style.addImage(backgroundIconId, BitmapUtils.getBitmapFromDrawable(wrappedDrawable)!!)
            }

            // Add background layer
            val backgroundLayerId = "$sourceId-background-layer"
            if (style.getLayer(backgroundLayerId) == null) {
                val backgroundLayer = SymbolLayer(backgroundLayerId, sourceId)
                    .withProperties(
                        iconImage(backgroundIconId),
                        iconSize(size),
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true),
                        // Keep background upright and not shrinking
                        iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                        iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT)
                    )
                style.addLayer(backgroundLayer)
            }

            if (icon != null) {
                // Add icon image (same as before)
                val iconId = "marker-icon-$icon"
                if (style.getImage(iconId) == null) {
                    val iconDrawable = ResourcesCompat.getDrawable(resources, icon, null)!!.mutate()
                    iconDrawable.setTint(ContextCompat.getColor(this, R.color.white))
                    style.addImage(iconId, BitmapUtils.getBitmapFromDrawable(iconDrawable)!!)
                }

                // Add icon layer
                val iconLayerId = "$sourceId-icon-layer"
                if (style.getLayer(iconLayerId) == null) {
                    val iconLayer = SymbolLayer(iconLayerId, sourceId)
                        .withProperties(
                            iconImage(iconId),
                            iconSize(size * 0.4f),
                            iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true),

                            // 1. Keep icon upright and not shrinking
                            iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                            iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),

                            // 2. Move icon up by fixed pixels
                            iconTranslate(arrayOf(0f, -25f)),

                            // 3. THE FINAL FIX: Ensure the translation is always "UP" relative
                            // to the SCREEN, not the map.
                            iconTranslateAnchor(Property.ICON_TRANSLATE_ANCHOR_VIEWPORT)
                        )
                    style.addLayer(iconLayer)
                }
            }
        }
    }

    private fun removeMarker(sourceId: String) {
        maplibreMap.getStyle { style ->
            val backgroundLayerId = "$sourceId-background-layer"
            val iconLayerId = "$sourceId-icon-layer"

            style.getLayer(iconLayerId)?.let { style.removeLayer(it) }
            style.getLayer(backgroundLayerId)?.let { style.removeLayer(it) }
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

    private fun drawLinksForVisibleNotes() {
        maplibreMap.getStyle { style ->
            val layersToRemove = style.layers.filter { it.id.startsWith("link-layer-") }
            val sourcesToRemove = style.sources.filter { it.id.startsWith("link-source-") }
            layersToRemove.forEach { style.removeLayer(it) }
            sourcesToRemove.forEach { style.removeSource(it) }

            val drawnLinks = mutableSetOf<String>()

            // Only draw links between notes that are currently visible (matching search or no search)
            notes.forEach { note1 ->
                // Only process this note if it's visible
                val isNote1Visible = searchResults.isEmpty() || searchResults.contains(note1.id)
                if (isNote1Visible) {
                    note1.linkedNotes?.forEach { note2Id ->
                        // Only draw the link if both notes are visible
                        val isNote2Visible = searchResults.isEmpty() || searchResults.contains(note2Id)
                        val note2 = notes.find { it.id == note2Id }
                        if (note2 != null && isNote2Visible) {
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

            val geoJsonOptions = GeoJsonOptions()
                .withLineMetrics(true) // This is required for line-gradient to work

            // Pass the options into the GeoJsonSource constructor
            val geoJsonSource = GeoJsonSource(sourceId, lineString, geoJsonOptions)
            style.addSource(geoJsonSource)

            // Determine if the link should be visible based on search results
            val isLinkVisible = searchResults.isEmpty() || 
                (searchResults.contains(note1.id) || searchResults.contains(note2.id))
            
            val color1 = if (isLinkVisible) {
                ColorUtils.getColorForDate(this, note1.relatedDate)
            } else {
                // Make the link transparent if neither note matches search
                applyAlphaToColor(ColorUtils.getColorForDate(this, note1.relatedDate), 0.3f)
            }
            val color2 = if (isLinkVisible) {
                ColorUtils.getColorForDate(this, note2.relatedDate)
            } else {
                // Make the link transparent if neither note matches search
                applyAlphaToColor(ColorUtils.getColorForDate(this, note2.relatedDate), 0.3f)
            }

            val lineLayer = LineLayer(layerId, sourceId)
            if (color1 == color2) {
                lineLayer.withProperties(
                    lineWidth(4f),
                    lineColor(color1)
                )
            } else {
                lineLayer.withProperties(
                    lineWidth(4f),
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
        // Dismiss info window when markers are refreshed
        if (infoWindowPopup?.isShowing == true) {
            infoWindowPopup?.dismiss()
            currentInfoNote = null
        }

        if (!::maplibreMap.isInitialized) return
        maplibreMap.getStyle { style ->
            val layersToRemove = style.layers.filter { it.id.startsWith("note-") || it.id.startsWith("link-layer-") }
            val sourcesToRemove = style.sources.filter { it.id.startsWith("note-") || it.id.startsWith("link-source-") }
            layersToRemove.forEach { style.removeLayer(it) }
            sourcesToRemove.forEach { style.removeSource(it) }

            // Draw all links but only between notes that are visible (either matching search or no search active)
            drawLinksForVisibleNotes()

            notes.forEach { note ->
                val noteLocation = Location("")
                noteLocation.latitude = note.coordinates.latitude
                noteLocation.longitude = note.coordinates.longitude
                
                // Determine if the note should be highlighted or transparent
                val isMatchingNote = searchResults.isEmpty() || searchResults.contains(note.id)
                
                if (isMatchingNote) {
                    // Show matching/normal notes with original color
                    val color = ColorUtils.getColorForDate(this, note.relatedDate)
                    addMarker(noteLocation, 0.08f, color, "note-${note.id}", note.icon)
                } else {
                    // Show non-matching notes as 70% transparent and grey
                    val greyColor = ContextCompat.getColor(this, R.color.grey)
                    // Adjust alpha to make it 70% transparent (30% opaque)
                    val transparentGreyColor = applyAlphaToColor(greyColor, 0.3f)
                    addMarker(noteLocation, 0.08f, transparentGreyColor, "note-${note.id}", note.icon)
                }
            }
        }

    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                if (::maplibreMap.isInitialized) {
                    onMapReady(maplibreMap)
                }
            }
            else -> {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkAndRequestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {}
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // You can show a rationale dialog here if needed
            }
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
                Toast.makeText(this, "Found you!", Toast.LENGTH_SHORT).show()
            } else {
                somethingWentWrongInfoMessage()
            }
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
                    relatedDate = newNoteDate,
                    creationDate = Date(),
                    editDate = Date(),
                    linkedNotes = mutableListOf(),
                    icon = newNoteIcon
                )
                notes.add(newNote)
                val noteLocation = Location("")
                noteLocation.latitude = newNote.coordinates.latitude
                noteLocation.longitude = newNote.coordinates.longitude
                addMarker(noteLocation, 0.08f, ColorUtils.getColorForDate(this, newNote.relatedDate), "note-${newNote.id}", newNote.icon)
            }
        }
        storageHandler.writeJsonToFile("notes.json", notes)
        // Apply current search filter if there's an active search
        val query = searchInput.text.toString().trim()
        if (query.isNotEmpty()) {
            performSearch()
        } else {
            showMarkersAndLinks()
        }
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
                // You can optionally update a view in the popup to show the selected date
            } else {
                newNoteDate = selectedDate
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
        val iconPreview = popUp.findViewById<ImageView>(R.id.icon_preview)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Icon")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("No Icon") { _, _ ->
                if (editingNote != null) {
                    editingNote?.icon = null
                } else {
                    newNoteIcon = null
                }
                iconPreview.visibility = View.GONE
            }
            .create()

        iconGrid.setOnItemClickListener { _, _, position, _ ->
            val selectedIcon = drawableResources[position]
            if (editingNote != null) {
                editingNote?.icon = selectedIcon
            } else {
                newNoteIcon = selectedIcon
            }
            iconPreview.setImageResource(selectedIcon)
            iconPreview.visibility = View.VISIBLE
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getAllDrawableResources(): List<Int> {
        val unwantedDrawables = setOf(
            R.drawable.button_background,
            R.drawable.delete_icon,
            R.drawable.delete_location,
            R.drawable.shape_button_prime_rounded_corners,
            R.drawable.shape_rounded_colors_popup,
            R.drawable.shape_rounded_square,
            R.drawable.icon_background,
            R.drawable.ic_launcher_background,
            R.drawable.ic_launcher_foreground,
            R.drawable.yc
        )
        return R.drawable::class.java.fields.map { it.getInt(null) }.filter { resId ->
            val resourceName = resources.getResourceEntryName(resId)
            !unwantedDrawables.contains(resId) && !resourceName.startsWith("abc_")
        }
    }
    
    private fun showInfoWindowForNote(note: Note, position: LatLng) {
        // Close any existing info window
        if (infoWindowPopup?.isShowing == true) {
            infoWindowPopup?.dismiss()
        }
        
        // If clicking the same note again, close the info window and return
        if (currentInfoNote?.id == note.id) {
            currentInfoNote = null
            return
        }
        
        currentInfoNote = note
        
        // Inflate the info window layout
        val inflater = LayoutInflater.from(this)
        val infoView = inflater.inflate(R.layout.map_info_window, null)
        
        // Set the note text and date
        val noteText = infoView.findViewById<TextView>(R.id.info_window_text)
        val noteDate = infoView.findViewById<TextView>(R.id.info_window_date)
        val colorIndicator = infoView.findViewById<View>(R.id.info_window_color_indicator)
        
        noteText.text = note.text
        noteDate.text = formatDate(note.relatedDate)
        
        // Set the color indicator to match the note's marker color
        val noteColor = ColorUtils.getColorForDate(this, note.relatedDate)
        colorIndicator.setBackgroundColor(noteColor)
        
        // Create and configure the PopupWindow
        infoWindowPopup = PopupWindow(
            infoView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Set background and elevation
        infoWindowPopup?.setBackgroundDrawable(null)
        
        // Calculate position on screen based on map coordinates
        val screenPosition = maplibreMap.projection.toScreenLocation(position)
        
        // Show the popup at the calculated position
        infoWindowPopup?.showAtLocation(
            mapView,
            0, // No gravity - we'll position it manually 
            screenPosition.x.toInt(),
            screenPosition.y.toInt() - 200 // Adjust to position above the marker
        )
    }
    
    private fun zoomToMatchingNotes() {
        if (searchResults.isEmpty()) return
        
        // Find all matching notes
        val matchingNotes = notes.filter { note -> searchResults.contains(note.id) }
        
        if (matchingNotes.isEmpty()) return
        
        // Find the bounds of all matching notes
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        
        for (note in matchingNotes) {
            val lat = note.coordinates.latitude
            val lon = note.coordinates.longitude
            
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lon < minLon) minLon = lon
            if (lon > maxLon) maxLon = lon
        }
        
        // Add padding around the bounds
        val padding = 0.01 // Degrees - adjust for desired padding
        
        val southwest = LatLng(minLat - padding, minLon - padding)
        val northeast = LatLng(maxLat + padding, maxLon + padding)
        
        // Animate to the bounds of the matching notes
        val bounds = org.maplibre.android.geometry.LatLngBounds.from(
            northeast.latitude,
            northeast.longitude,
            southwest.latitude,
            southwest.longitude
        )
        
        try {
            maplibreMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    100 // padding in pixels
                ),
                1000 // duration in milliseconds
            )
        } catch (ex: Exception) {
            Log.e("ZoomError", "Error zooming to bounds: ${ex.message}")
        }
    }
}