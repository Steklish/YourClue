# Requirements Document 
## 1 Introduction
This document outlines the requirements for the **YouClue** mobile application.

YouClue is an Android application designed for creating and managing geo-tagged notes. The software allows users to place notes on an interactive map, each associated with specific coordinates. Users can add descriptive text, assign a relevant date, and choose an icon for each note. The application also supports creating visual links between notes to represent relationships between them.

The application will store all data locally on the user's device. It will not support user accounts, cloud synchronization, or real-time collaborative features.

## 2 User Requirements
### 2.1 Software Interfaces
The software product interacts with the following external systems and libraries:
- **Android Operating System:** Provides the runtime environment, user interface components, and access to device hardware like the GPS.
- **MapLibre GL SDK:** Used to render the interactive map, display markers, and draw lines between linked notes.
- **Google Play Services (Location):** Utilized to fetch the device's current geographical location to center the map for the user.
- **Gson Library:** Employed for serializing and deserializing note objects to and from the JSON format for local storage.

### 2.2 User Interfaces
- **Main Map View:** The primary interface is a full-screen interactive map. Notes are displayed as tappable markers at their designated locations. The color of the markers corresponds to the "related date" of the note.
- **Note Creation:** Users can tap any point on the map to select a location. A button will appear, allowing them to create a new note at that spot.
- **Note Interaction:** A short tap on a note's marker displays an info window with its text. A long-press opens the note editing screen.
- **Note Editor:** A popup dialog allows for creating and editing notes. It includes a text field, a button to select a date, an icon picker, and options to link the note to others or delete it.
- **Search and Filter:** A search bar is present to filter notes on the map by their text content or date. A dedicated calendar view allows users to see and filter notes based on their "related date".
- **Map Controls:** On-screen buttons are provided for zooming in/out and re-centering the map on the user's current location.

### 2.3 User Characteristics
The intended users of this product are the general public. No special technical expertise or training is required. Users are expected to be familiar with basic smartphone operations, such as tapping, long-pressing, and using on-screen keyboards.

### 2.4 Assumptions and Dependencies
- The application is dependent on the Android OS (minimum SDK version 24).
- The device must have an active internet connection to download map tiles from the MapTiler service.
- For location-based features to function correctly, the device's location services must be enabled, and the user must grant the application location permissions.
- The device must have sufficient local storage available to save the notes data file.

## 3 System Requirements
These subsections contain the software requirements in detail.
### 3.1 Functional Requirements
- **FR1: Note Creation:** The system shall allow a user to create a note at any selected geographical coordinate on the map.
- **FR2: Note Content:** Each note shall store multi-line text, a creation date, a last-edited date, and a user-defined "related date".
- **FR3: Note Icon:** The system shall allow a user to optionally assign a predefined icon to a note.
- **FR4: Note Visualization:** The system shall display each note as a marker on the map at its stored coordinates.
- **FR5: Note Editing:** The system shall allow a user to modify the text, "related date", and icon of any existing note.
- **FR6: Note Deletion:** The system shall allow a user to permanently delete any existing note.
- **FR7: Note Linking:** The system shall allow a user to create a bidirectional link between two notes.
- **FR8: Link Visualization:** The system shall display a line on the map connecting the markers of any two notes that are linked.
- **FR9: Data Persistence:** The system shall save all notes and their links to a JSON file in the application's private local storage, ensuring data persists across sessions.
- **FR10: Map Navigation:** The system shall allow the user to pan and zoom the map.
- **FR11: Text/Date Search:** The system shall filter the notes displayed on the map to show only those whose text or formatted date matches a user's search query.
- **FR12: Calendar Filtering:** The system shall provide a calendar interface to filter the notes displayed on the map by their "related date".
- **FR13: User Location:** The system shall be able to find and display the user's current location on the map.

### 3.2 Non-Functional Requirements
#### 3.2.1 SOFTWARE QUALITY ATTRIBUTES
- **Reliability:** The application must ensure that all user data (notes, links) is saved correctly to local storage without corruption or loss under normal operating conditions. This will be measured by performing stress tests on save/load cycles and verifying data integrity.
- **Usability:** The application's interface must be intuitive and easy for new users to understand. Core functions like creating, viewing, and editing notes should be easily discoverable. This will be measured through user feedback and by timing how long it takes a new user to perform key tasks.
- **Performance:** The application must maintain a smooth user experience, with responsive map panning and zooming, even when displaying a large number of notes (e.g., 500+). Initial loading time should be under 3 seconds. This will be measured by profiling UI rendering performance and startup times.
- **Security:** The application must store its data in the app-specific private storage on the device, making it inaccessible to other applications.