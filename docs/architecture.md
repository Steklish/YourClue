# Architecture Document

## 1. Introduction
This document describes the software architecture of the **YouClue** Android application. The application is designed as a single-screen, map-centric utility. The architecture is intentionally simple, centering all core logic and user interaction within a single Android `Activity`. This approach is well-suited for the application's specific scope, which is focused on a single primary feature: interacting with notes on a map.

## 2. Architectural Style
The application follows a **View-centric** or **Activity-based** architectural style. This can be seen as a pragmatic implementation of the **Model-View-Controller (MVC)** pattern, where the `Activity` (`MainActivity`) takes on the roles of both the **View** and the **Controller**.

-   **Model:** The data structures of the application, primarily the `Note` and `Coordinates` data classes. They are plain data containers with no business logic.
-   **View:** The user interface, defined in XML layout files (`activity.xml`, `note_editor_popup.xml`, etc.) and rendered by the Android OS. The `MapView` from MapLibre is a key component of the View.
-   **Controller:** The `MainActivity` class acts as the central controller. It listens to user input from the View (map clicks, button taps), processes it, manipulates the Model (the in-memory list of notes), and updates the View to reflect the changes (e.g., drawing a new marker on the map).

This style was chosen for its simplicity and directness, avoiding the overhead of more complex patterns like MVVM or MVI which are not necessary for an application of this scale and scope.

## 3. Component Breakdown
The application is composed of several key components:

-   **`MainActivity.kt`**: This is the core component of the application. It is responsible for:
    -   Initializing and configuring the MapLibre `MapView`.
    -   Loading and saving notes from local storage.
    -   Handling all user interactions, including map gestures and button clicks.
    -   Managing the application's state (e.g., which note is being edited, whether the editor popup is visible).
    -   Rendering all visual elements on the map, such as note markers and the lines connecting them.
    -   Handling location permissions and fetching the user's current location.

-   **Data Models (`Note.kt`)**: A set of Kotlin `data class` files that define the application's data structure. The primary model is `Note`, which represents a single geo-tagged note.

-   **Storage (`LocalStorageHandler.kt`)**: This component abstracts the data persistence logic. It is responsible for serializing the list of `Note` objects into a JSON string (using Gson) and writing it to a local file, and the corresponding deserialization process when reading the file.

-   **UI Adapters (`IconAdapter.kt`, `LinkedNoteAdapter.kt`)**: Standard Android adapters used to populate `GridView` and `RecyclerView` widgets within the UI. For example, `IconAdapter` displays the grid of selectable icons, and `LinkedNoteAdapter` displays the list of linked notes in the editor popup.

-   **Layouts (XML files)**: These files define the structure and appearance of the user interface. The main layout is `activity.xml`, which contains the `MapView` and primary UI controls. Other layouts like `note_editor_popup.xml` define modular UI components.

-   **Utilities (`ColorUtils.kt`)**: Helper classes that contain stateless, reusable logic. `ColorUtils` provides a function to determine the color of a note marker based on its date.

## 4. Data Management
-   **Data Model**: The application's data is modeled around a primary object, `Note`, which contains coordinates, text, dates, an optional icon, and a list of IDs of other notes it is linked to.
-   **Data Storage**: All application data is stored in a single JSON file named `notes.json` within the app's private internal storage. There is no database.
-   **Data Flow**: The data flow is straightforward:
    1.  On application startup, `MainActivity` reads the `notes.json` file and deserializes its content into an in-memory `MutableList<Note>`.
    2.  All operations (create, edit, delete, link) are performed directly on this in-memory list.
    3.  After each modification, the entire list is re-serialized to JSON and written back to the `notes.json` file, overwriting the previous content. This ensures data persistence.

## 5. User Interface
The UI is built using the **Android View System** with layouts defined in **XML**. It is not built with Jetpack Compose.

-   **`MainActivity`** directly manages all UI widgets. It holds references to buttons, text fields, and the map view itself.
-   User interactions are handled through event listeners (e.g., `setOnClickListener`, `addOnMapClickListener`) set up within `MainActivity`.
-   UI updates are performed imperatively. The code in `MainActivity` directly calls methods on the View objects to change their state, visibility, or content (e.g., `popUp.setVisibility(View.VISIBLE)`, `maplibreMap.addLayer(...)`).

## 6. External Services
The application relies on two main external services:

-   **MapTiler**: Provides the map tiles and map styles. The application communicates with the MapTiler API using an API key to fetch the visual map data.
-   **Google Play Services (Location)**: Used to access the device's location services. This allows the application to get the user's current geographical position for the "find my location" feature.