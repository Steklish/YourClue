# Classes Documentation for YourcClue Android Application

This document provides an overview of all classes in the YourcClue project, including activities and their properties.

## Activities

### MainActivity
Main activity of the application that handles map display and note management.

**Properties:**
- `mapView: MapView` - MapLibre map view component
- `maplibreMap: MapLibreMap` - MapLibre map instance
- `createNoteButton: Button` - Button to create new notes
- `popUp: ConstraintLayout` - Popup layout for note editing
- `container: ConstraintLayout` - Main container layout
- `blurredBackground: ImageView` - Background for popup with blur effect
- `searchInputLayout: TextInputLayout` - Layout for search input
- `searchInput: TextInputEditText` - Search input field
- `styleUrl: String?` - URL for map style
- `selectedLocation: LatLng?` - Currently selected location on map
- `storageHandler: JsonStorage` - Storage handler instance
- `notes: MutableList<Note>` - List of all notes
- `searchResults: Set<String>` - Set of IDs for notes matching search
- `editingNote: Note?` - Note currently being edited
- `linkingNote: Note?` - Note that is in linking mode
- `infoWindowPopup: PopupWindow?` - Info window popup for notes
- `currentInfoNote: Note?` - Note currently showing info window
- `startDateFilter: Date?` - Start date for date filtering
- `endDateFilter: Date?` - End date for date filtering
- `shouldApplyDefaultFilter: Boolean` - Whether to apply default current month filter
- `lastSelectedStartDate: Date?` - Last selected start date
- `lastSelectedEndDate: Date?` - Last selected end date
- `newNoteIcon: Int?` - Icon for new note
- `newNoteDate: Date` - Date for new note
- `fusedLocationClient: FusedLocationProviderClient` - Google location services client

### CreateNote
Activity for creating notes (currently basic implementation).

**Properties:**
- No specific properties defined (extends AppCompatActivity)

### CalendarSelectFiltersActivity
Activity for selecting date ranges to filter notes.

**Properties:**
- `binding: ActivityCalendarSelectFiltersBinding` - View binding for the activity
- `storageHandler: JsonStorage` - Storage handler instance
- `allNotes: MutableList<Note>` - All notes loaded from storage
- `noteAdapter: NoteAdapter` - Adapter for displaying notes in RecyclerView
- `dataChanged: Boolean` - Flag indicating if data was modified

## Data Classes

### Coordinates
Represents geographic coordinates.

**Properties:**
- `latitude: Double` - Latitude value
- `longitude: Double` - Longitude value

### Note
Represents a note with location and metadata.

**Properties:**
- `id: String` - Unique identifier for the note (default: UUID.randomUUID().toString())
- `coordinates: Coordinates` - Geographic coordinates of the note
- `text: String` - Text content of the note
- `relatedDate: Date` - Date associated with the note
- `creationDate: Date` - Date when the note was created
- `editDate: Date` - Date when the note was last edited
- `references: List<String>` - List of other note IDs this note references (default: emptyList())
- `imageReference: String?` - Optional reference to an image (default: null)
- `linkedNotes: MutableList<String>?` - Mutable list of linked note IDs (default: null)
- `icon: Int?` - Optional icon resource ID (default: null)

## Storage Classes

### JsonStorage (Interface)
Interface for JSON storage operations.

**Methods:**
- `writeJsonToFile(fileName: String, data: T)` - Serializes and saves data to JSON file
- `readJsonFromFile(fileName: String, type: Type): T?` - Reads and deserializes JSON from file

### LocalStorageHandler
Implementation of JsonStorage using local file system.

**Properties:**
- `context: Context` - Android application context
- `gson: Gson` - Gson instance for JSON serialization

## Utility Classes

### ColorUtils
Utility object for color-related operations.

**Methods:**
- `getColorForDate(context: Context, date: Date): Int` - Gets color based on the day of the week from the given date

### IconAdapter
Adapter for displaying icons in a grid view.

**Properties:**
- `context: Context` - Android application context
- `icons: List<Int>` - List of drawable resource IDs for icons

### LinkedNoteAdapter
Adapter for displaying linked notes in a RecyclerView.

**Properties:**
- `linkedNotes: MutableList<Note>` - List of linked notes
- `unlinkAction: (Note) -> Unit` - Callback function when a note is unlinked

**Inner Class - ViewHolder:**
- `linkedNoteText: TextView` - Text view showing linked note text
- `unlinkButton: Button` - Button to unlink the note

### NoteAdapter
Adapter for displaying notes in a RecyclerView.

**Properties:**
- `notes: MutableList<Note>` - List of notes to display
- `onNoteClick: (Note) -> Unit` - Callback function when a note is clicked
- `onDelete: (Note) -> Unit` - Callback function when a note is deleted
- `onLocateNote: (Note) -> Unit` - Callback function when locate button is clicked

**Inner Class - NoteViewHolder:**
- `noteText: TextView` - Text view showing note text
- `colorIndicator: View` - View showing the color indicator for the note
- `noteDate: TextView` - Text view showing note creation date
- `deleteButton: Button` - Button to delete the note
- `locateButton: MaterialButton` - Button to locate the note on the map
- `noteIcon: ImageView` - Image view showing the note icon

## Theme Classes

### Color (Composable theme colors)
Contains color definitions for the application theme.

**Colors:**
- `Purple80: Color` - Purple color for dark theme (0xFFD0BCFF)
- `PurpleGrey80: Color` - Purple grey color for dark theme (0xFFCCC2DC)
- `Pink80: Color` - Pink color for dark theme (0xFFEFB8C8)
- `Purple40: Color` - Purple color for light theme (0xFF6650a4)
- `PurpleGrey40: Color` - Purple grey color for light theme (0xFF625b71)
- `Pink40: Color` - Pink color for light theme (0xFF7D5260)

### Theme (Composable theme)
Contains the main theme for the application.

**Properties:**
- `darkTheme: Boolean` - Whether to use dark theme (default: system theme)
- `dynamicColor: Boolean` - Whether to use dynamic colors (default: true)
- `content: @Composable () -> Unit` - Content to apply the theme to

**Color Schemes:**
- `DarkColorScheme` - Color scheme for dark theme
- `LightColorScheme` - Color scheme for light theme

### Type (Typography)
Contains typography definitions for the application theme.

**Properties:**
- `Typography: Typography` - Set of Material typography styles
- `bodyLarge: TextStyle` - Default text style for body text