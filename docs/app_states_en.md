# YourcClue Application States

## Overview
This document describes the various states of the YourcClue application for generating a state diagram. YourcClue is a geolocation-based note-taking Android application that allows users to create, edit, and link notes on a map.

## Application States

### 1. **Initial State / Launch State**
- **Description**: The application starts and initializes basic components
- **Activities**: Loading map resources, checking permissions, initializing storage
- **Triggers**: App launch by user
- **Transitions**: 
  - To Map View State (if permissions granted)
  - To Permission Request State (if permissions not granted)

### 2. **Permission Request State**
- **Description**: Waiting for user to grant location permissions
- **Activities**: Displaying permission request dialog
- **Triggers**: App detects missing required permissions
- **Transitions**:
  - To Map View State (if permissions granted)
  - To Permission Denied State (if permissions denied)

### 3. **Permission Denied State**
- **Description**: User has denied location permissions
- **Activities**: Displaying warning message, offering alternative options
- **Triggers**: Permission denied by user
- **Transitions**:
  - To Map View State (if user grants permissions later)
  - To Exit State (if user chooses not to use app)

### 4. **Map View State (Main State)**
- **Description**: Main application state with interactive map displayed
- **Activities**: Map rendering, marker display, user interaction handling
- **Triggers**: Successful initialization and permissions
- **Sub-states/Events**:
  - Map is loading
  - Map is ready and displaying
  - GPS location is being acquired
  - Current user location is marked on map
- **Transitions**:
  - To Note Creation State (when user taps to create marker)
  - To Note Edit State (when user long-presses existing note)
  - To Note View State (when user taps existing note)
  - To Calendar Filter State (when opening calendar)
  - To Search State (when using search functionality)

### 5. **Note Creation State**
- **Description**: User is in the process of creating a new note
- **Activities**: Temporary marker placement, note editor popup displayed
- **Triggers**: User clicks "Add Marker" button or taps map after marker placement
- **Sub-states/Events**:
  - Temporary marker placed on map
  - Note editor popup is visible
  - User enters note text
  - User selects date for note
  - User selects icon for note
- **Transitions**:
  - To Map View State (when note is saved)
  - To Map View State (when note creation is cancelled)
  - To Icon Selection State (when choosing icon)

### 6. **Note Edit State**
- **Description**: User is editing an existing note
- **Activities**: Note editor popup displayed with existing note data
- **Triggers**: User long-presses existing note marker
- **Sub-states/Events**:
  - Note editor popup shows existing content
  - User modifies note text
  - User modifies assigned date
  - User changes icon
  - User manages linked notes
  - User deletes the note
- **Transitions**:
  - To Map View State (when note is saved)
  - To Map View State (when editing is cancelled)
  - To Delete Confirmation State (when note is deleted)
  - To Link Creation State (when linking to another note)
  - To Icon Selection State (when choosing icon)

### 7. **Note View State**
- **Description**: User views a note without editing
- **Activities**: Info window displayed with note content
- **Triggers**: User taps existing note marker
- **Sub-states/Events**:
  - Info window appears with note text
  - Note details are displayed
- **Transitions**:
  - To Map View State (when clicking elsewhere)
  - To Note Edit State (when entering edit mode)
  - To Link Creation State (when linking to another note)

### 8. **Link Creation State**
- **Description**: User is in the process of linking notes together
- **Activities**: Interface for connecting notes, visual feedback for linking
- **Triggers**: User selects "link" option on a note
- **Sub-states/Events**:
  - Linking mode activated
  - User selects first note to link
  - User selects second note to link
  - Link created between notes
- **Transitions**:
  - To Map View State (when linking is completed)
  - To Note Edit State (when returning to note editing)
  - To Map View State (when linking is cancelled)

### 9. **Icon Selection State**
- **Description**: User is selecting an icon for a note
- **Activities**: Displaying icon gallery, allowing icon selection
- **Triggers**: User clicks "select icon" button in note editor
- **Sub-states/Events**:
  - Icon picker dialog is displayed
  - User browses available icons
  - User selects an icon
- **Transitions**:
  - To Note Creation State (when icon is selected)
  - To Note Edit State (when icon is selected)
  - To Note Creation State (when icon selection is cancelled)
  - To Note Edit State (when icon selection is cancelled)

### 10. **Calendar Filter State**
- **Description**: User is applying date filters to notes
- **Activities**: Calendar interface displayed, date range selection
- **Triggers**: User clicks calendar button
- **Sub-states/Events**:
  - Calendar view is displayed
  - User selects date range
  - Notes filtered by selected dates
- **Transitions**:
  - To Map View State (when filter is applied)
  - To Map View State (when calendar is closed)

### 11. **Search State**
- **Description**: User is searching for specific notes
- **Activities**: Search input active, results filtering, highlighting matches
- **Triggers**: User types in search bar
- **Sub-states/Events**:
  - Text input in search field
  - Search results updating in real-time
  - Matching notes highlighted on map
  - Non-matching notes hidden
- **Transitions**:
  - To Map View State (when search is cleared)
  - To Note View State (when selecting search result)
  - To Note Edit State (when selecting search result for editing)

### 12. **Delete Confirmation State**
- **Description**: User is confirming deletion of a note
- **Activities**: Confirmation dialog displayed
- **Triggers**: User clicks delete button in note editor
- **Sub-states/Events**:
  - Confirmation dialog appears
  - User confirms or cancels deletion
- **Transitions**:
  - To Map View State (when deletion is confirmed)
  - To Note Edit State (when deletion is cancelled)

### 13. **Map Navigation State**
- **Description**: User is navigating the map (zooming, panning)
- **Activities**: Map interaction, location tracking
- **Triggers**: User performs map gestures or clicks navigation buttons
- **Sub-states/Events**:
  - Zoom in/out
  - Map panning
  - Center on user location
- **Transitions**:
  - To Map View State (when navigation is complete)

### 14. **Exit State**
- **Description**: Application is closing
- **Activities**: Saving current state, cleaning up resources
- **Triggers**: User exits the application
- **Transitions**: None (application terminates)

## State Transitions Summary

- Most states flow back to the **Map View State** (main application view)
- The **Map View State** is the central hub connecting to other states
- **Note Creation** and **Note Edit** states share similar UI elements and transitions
- **Search** and **Calendar Filter** states can work independently or together
- Permission states must be resolved before reaching the main application states

## Key UI Components in States

- MapView: Present in Map View, Note Creation, Note Edit, and Note View states
- Note Editor Popup: Present in Note Creation and Note Edit states
- Search Bar: Available in Map View State and remains active during search
- Navigation Controls: Available in Map View State for zooming and location
- Icon Picker: Modal dialog in Icon Selection State

## Special Considerations

- The state system should maintain data consistency across state transitions
- Note data changes should persist across all states
- Search and filter states should be additive (search within filtered results)
- Link creation should validate that notes exist and can be linked
- GPS permissions are critical for core functionality
- Offline functionality may be limited depending on map implementation