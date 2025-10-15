private fun showInfoWindow(note: Note) {
    Log.d("InfoWindowDebug", "showInfoWindow called for note: ${note.id}, text:
${note.text.take(30)}...")

    // Since MapLibre might have issues with network, we'll implement a fallback
    // For debugging, let's use a simple Toast first to verify note text content
    Toast.makeText(this, note.text, Toast.LENGTH_SHORT).show()

    // Also add the original MapLibre approach (it might work when network is available)
    val markerColor = ColorUtils.getColorForDate(this, note.relatedDate)
    maplibreMap.getStyle { style ->
        val sourceId = "info-window-source-${note.id}"
        val layerId = "info-window-layer-${note.id}"
        val imageId = "info-window-background"
        val shape = R.drawable.info_window_background

        if (style.getImage(imageId) == null) {
            val backgroundDrawable = ContextCompat.getDrawable(this, shape)
            backgroundDrawable?.setTint(markerColor)
            val bitmap = Bitmap.createBitmap(512, 521, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            backgroundDrawable?.setBounds(0, 0, canvas.width, canvas.height)
            backgroundDrawable?.draw(canvas)
            style.addImage(imageId, bitmap, false)
        }

        val feature = Feature.fromGeometry(Point.fromLngLat(note.coordinates.longitude,
note.coordinates.latitude))
        feature.addStringProperty("text", note.text)

        val source = GeoJsonSource(sourceId, FeatureCollection.fromFeatures(arrayOf(feature)))
        if (style.getSource(sourceId) == null) {
            style.addSource(source)
        } else {
            (style.getSource(sourceId) as? GeoJsonSource)?.setGeoJson(feature)
        }

        val symbolLayer = SymbolLayer(layerId, sourceId)
            .withProperties(
                iconImage(imageId),
                iconSize(0.3f), // Smaller for better visibility
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconOffset(arrayOf(0f, -20f)),

                textField(get("text")),
                textSize(12f),
                textColor(ContextCompat.getColor(this, R.color.black)),
                textHaloColor(ContextCompat.getColor(this, R.color.white)),
                textHaloWidth(1f),
                textIgnorePlacement(true),
                textAllowOverlap(true),
                textAnchor(Property.TEXT_ANCHOR_CENTER),
                textOffset(arrayOf(0f, -2f))
            )

        if (style.getLayer(layerId) != null) {
            style.removeLayer(layerId)
        }
        style.addLayer(symbolLayer)
    }

    // Track that this info window is now open
    openInfoWindows.add(note.id)
}
