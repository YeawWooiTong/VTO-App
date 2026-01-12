package com.TOTOMOFYP.VTOAPP.ui.events

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class EventsUiState(
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedEvent: Event? = null
)

class EventViewModel(
    private val context: Context? = null,
    private val eventRepository: EventRepository = EventRepository(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _showAddEventDialog = MutableStateFlow(false)
    val showAddEventDialog: StateFlow<Boolean> = _showAddEventDialog.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                combine(
                    eventRepository.getUpcomingEvents(),
                    eventRepository.getPastEvents()
                ) { upcoming, past ->
                    _uiState.value = _uiState.value.copy(
                        upcomingEvents = upcoming,
                        pastEvents = past,
                        isLoading = false
                    )
                }.collect()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load events: ${e.message}"
                )
            }
        }
    }

    fun createEvent(event: Event, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = eventRepository.createEvent(event)
            result.fold(
                onSuccess = { eventId ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Force refresh to get the latest data
                    loadEvents()
                    android.util.Log.d("EventViewModel", "Event created successfully: $eventId")
                    onComplete?.invoke()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create event: ${exception.message}"
                    )
                    android.util.Log.e("EventViewModel", "Failed to create event", exception)
                    onComplete?.invoke()
                }
            )
        }
    }
    
    fun refreshEvents() {
        loadEvents()
    }

    fun updateEvent(event: Event, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = eventRepository.updateEvent(event)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadEvents()
                    onComplete?.invoke()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update event: ${exception.message}"
                    )
                    onComplete?.invoke()
                }
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = eventRepository.deleteEvent(eventId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadEvents()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete event: ${exception.message}"
                    )
                }
            )
        }
    }


    fun assignOutfitToEvent(eventId: String, outfitId: String, outfitImageUrl: String?) {
        viewModelScope.launch {
            val result = eventRepository.assignOutfitToEvent(eventId, outfitId, outfitImageUrl)
            result.fold(
                onSuccess = { loadEvents() },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to assign outfit: ${exception.message}"
                    )
                }
            )
        }
    }

    fun selectEvent(event: Event) {
        _uiState.value = _uiState.value.copy(selectedEvent = event)
    }

    fun clearSelectedEvent() {
        _uiState.value = _uiState.value.copy(selectedEvent = null)
    }

    fun showAddEventDialog() {
        _showAddEventDialog.value = true
    }

    fun hideAddEventDialog() {
        _showAddEventDialog.value = false
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun getEventsByType(eventType: EventType): Flow<List<Event>> {
        return eventRepository.getEventsByType(eventType)
    }

    fun searchEvents(query: String): List<Event> {
        val currentState = _uiState.value
        val allEvents = currentState.upcomingEvents + currentState.pastEvents
        
        return if (query.isEmpty()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.title.contains(query, ignoreCase = true) ||
                event.description.contains(query, ignoreCase = true) ||
                event.location.contains(query, ignoreCase = true) ||
                event.eventType.displayName.contains(query, ignoreCase = true)
            }
        }
    }

    fun getEventsForDate(date: String): List<Event> {
        val currentState = _uiState.value
        val allEvents = currentState.upcomingEvents + currentState.pastEvents
        
        return allEvents.filter { event ->
            event.date == date
        }
    }

    private fun formatEventDateTime(event: Event): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateTime = dateFormat.parse("${event.date} ${event.time}")
            dateTime?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

class EventViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}