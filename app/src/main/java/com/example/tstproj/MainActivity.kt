package com.example.tstproj

import android.Manifest
import android.R.attr.iconTint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.tstproj.BuildConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconColor
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.BitmapUtils
import org.maplibre.geojson.Point

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var mapView: MapView
    private lateinit var maplibreMap: MapLibreMap

    private lateinit var createMarkerButton: Button
    private lateinit var popUp: ConstraintLayout
    private lateinit var container: ConstraintLayout
    private lateinit var blurredBackground: ImageView
    var styleUrl : String? = null



    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity)

        createMarkerButton = findViewById(R.id.createMarkerButton)
        popUp = findViewById(R.id.popup)
        container = findViewById(R.id.container)
        // ADDED: Initialize the ImageView
        blurredBackground = findViewById(R.id.blurred_background)

        // Configure button
        createMarkerButton.isClickable = true // Set to true to make it clickable


        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "backdrop"
        styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"
        Log.d("MyAppTag", styleUrl!!)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 1. Initialize the OnBackPressedCallback
        backPressedCallback = object : OnBackPressedCallback(false) { // Start with disabled
            override fun handleOnBackPressed() {
                // This is called when the back button is pressed AND the callback is enabled.
                hideEditPopup()
            }
        }
        // 2. Add the callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        setListeners()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestLocationPermission()

        // We set the listener on the root view of the layout
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            // Get the insets for the system bars (status bar, navigation bar)
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // The 'insets.top' value is the exact height of the status bar
            val statusBarHeight = insets.top

            // Now, apply this height as a top margin to your target view
            mapView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }

            // Return the insets so other views can consume them if needed
            windowInsets
        }
    }

    fun setListeners(){
        createMarkerButton.setOnClickListener {
            showEditPopup()
        }
        popUp.findViewById<MaterialButton>(R.id.close_popup_button).setOnClickListener {
            this.hideEditPopup()
        }
        container.findViewById<MaterialButton>(R.id.find_self_button).setOnClickListener {
            this.zoomToSelf()
        }
        container.findViewById<MaterialButton>(R.id.zoom_in_button).setOnClickListener {
            if (::maplibreMap.isInitialized) { // check if ready
                maplibreMap.animateCamera(
                    CameraUpdateFactory.zoomTo(maplibreMap.zoom + 1f),
                    200 // Duration in milliseconds
                )
            }
        }
        container.findViewById<MaterialButton>(R.id.zoom_out_button).setOnClickListener {
            if (::maplibreMap.isInitialized) { // check if ready
                maplibreMap.animateCamera(
                    CameraUpdateFactory.zoomTo(maplibreMap.zoom - 1f),
                    200 // Duration in milliseconds
                )
            }
        }
        container.findViewById<MaterialButton>(R.id.open_calendar_button).setOnClickListener {
            val intent = Intent(this, CalendarSelectFiltersActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        this.maplibreMap = map
        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "backdrop"
        val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"


        this.getLocationWithCallback { location ->
            map.setStyle(styleUrl) {
                if (location != null) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(15.0)
                        .build()
                    addMarker(location, 0.08f, ContextCompat.getColor(this, R.color.primary_color))
                }
                else{
                    somethingWentWrongInfoMessage()
                }
            }
        }



    }

    // Lifecycle methods...
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

        // Use a blur radius between 1f and 25f
        val blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        blur.setRadius(20f) // Adjust the blur radius as needed
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

        maplibreMap.snapshot { snapshotBitmap ->
            val blurredBitmap = blurBitmap(this@MainActivity, snapshotBitmap)
            blurredBackground.setImageBitmap(blurredBitmap)

            // --- Animation for Blurred Background ---
            blurredBackground.alpha = 0f // Start fully transparent
            blurredBackground.visibility = View.VISIBLE
            blurredBackground.animate()
                .alpha(1f) // Animate to fully visible
                .setDuration(300) // Animation duration in milliseconds
                .setListener(null) // Clear any previous listeners

            // --- Animation for Popup ---
            popUp.alpha = 0f
            popUp.scaleX = 0.8f // Start slightly smaller for a "pop" effect
            popUp.scaleY = 0.8f
            popUp.visibility = View.VISIBLE

            popUp.animate()
                .alpha(1f)
                .scaleX(1f) // Animate to normal size
                .scaleY(1f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // This runs when the animation is complete
                        backPressedCallback.isEnabled = true
                    }
                })
        }
    }

    // --- EDITED: Animate hiding the popup ---
    fun hideEditPopup() {
        // --- Animation for Blurred Background ---
        blurredBackground.animate()
            .alpha(0f) // Animate to fully transparent
            .setDuration(100)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // This runs when the animation is complete
                    blurredBackground.visibility = View.GONE
                    // Optional: clear the bitmap to free up memory
                    blurredBackground.setImageBitmap(null)
                }
            })

        // --- Animation for Popup ---
        popUp.animate()
            .alpha(0f)
            .scaleX(0.8f) // Animate to a smaller size
            .scaleY(0.8f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    popUp.visibility = View.GONE
                    // We only disable the back press callback after the animation is finished
                    backPressedCallback.isEnabled = false
                }
            })
    }


    fun addMarker(position: Location, size: Float, @ColorInt color: Int) {
        val latitude: Double = position.latitude
        val longitude: Double = position.longitude

        // 1. Create a UNIQUE ID for the colored icon to avoid overwriting.
        // We can base it on the color's integer value.
        val uniqueIconId = "marker-icon-$color"

        maplibreMap.getStyle { style ->
            // 2. OPTIMIZATION: Only generate and add the tinted bitmap if it doesn't already exist in the style.
            if (style.getImage(uniqueIconId) == null) {
                // Get the original drawable
                val originalDrawable: Drawable = ResourcesCompat.getDrawable(resources, R.drawable.geo_marker, null)!!

                // Wrap the drawable to make it mutable and apply the tint
                val wrappedDrawable = DrawableCompat.wrap(originalDrawable).mutate()
                DrawableCompat.setTint(wrappedDrawable, color)

                // Convert the now-tinted drawable to a bitmap
                val bitmap = BitmapUtils.getBitmapFromDrawable(wrappedDrawable)!!

                // Add the colored bitmap to the style with our unique ID
                style.addImage(uniqueIconId, bitmap)
            }

            // 3. Create a GeoJSON feature for the point
            val geoJson = """
        {
          "type": "FeatureCollection",
          "features": [{
            "type": "Feature",
            "geometry": {
              "type": "Point",
              "coordinates": [${longitude}, ${latitude}]
            },
            "properties": {
              "icon": "$uniqueIconId" 
            }
          }]
        }
        """.trimIndent() // Use the unique ID in the properties

            // 4. Add or update the GeoJSON source.
            // For multiple markers, you'll want a more robust way to manage the GeoJSON,
            // but for a single one, this works.
            if (style.getSource("marker-source") == null) {
                style.addSource(GeoJsonSource("marker-source", geoJson))
            } else {
                (style.getSource("marker-source") as GeoJsonSource).setGeoJson(geoJson)
            }

            // 5. Add the symbol layer if it doesn't exist
            if (style.getLayer("marker-layer") == null) {
                val symbolLayer = SymbolLayer("marker-layer", "marker-source")
                    .withProperties(
                        iconImage(get("icon")), // The 'get("icon")' expression reads the unique ID from the GeoJSON
                        iconSize(size),
                        iconAnchor(ICON_ANCHOR_BOTTOM)
                    )
                style.addLayer(symbolLayer)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    // You can now get the location.
//                    getLocation()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    // You can still get the location, but it will be less accurate.
//                    getLocation()
                }
                else -> {
                    // No location access granted.
                    // Handle the case where the user denies the permission.
                    // You might want to show a dialog explaining why you need the permission.
                }
            }
        }

    fun checkAndRequestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explain to the user why you need this permission.
                // After showing the explanation, you can request the permission again.
                // For example, show a dialog here.
            }
            else -> {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWithCallback(onLocationReady: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // We have a result, call the callback with the location
                // The location can be null, so we pass it as-is.
                onLocationReady(location)
            }
            .addOnFailureListener { e ->
                // An error occurred, call the callback with null
                Log.e("LocationError", "Failed to get location.", e)
                onLocationReady(null)
            }
    }

    private fun somethingWentWrongInfoMessage(){
        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
    }

    private fun zoomToSelf(){
        this.getLocationWithCallback { location ->
            if (location != null) {
                this.maplibreMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(15.0)
                    .build()
            }
            else{
                somethingWentWrongInfoMessage()
            }
            Toast.makeText(this, "Found you!", Toast.LENGTH_SHORT).show()

        }
    }
}
