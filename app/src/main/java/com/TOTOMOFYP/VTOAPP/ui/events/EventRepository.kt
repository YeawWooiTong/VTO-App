package com.TOTOMOFYP.VTOAPP.ui.events

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

class EventRepository(private val context: Context? = null) {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    private fun getUserEventsCollection() = 
        firestore.collection("users")
            .document(auth.currentUser?.uid ?: "anonymous")
            .collection("events")

    suspend fun createEvent(event: Event): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val eventsCollection = getUserEventsCollection()
            val eventId = if (event.id.isEmpty()) {
                eventsCollection.document().id
            } else event.id
            
            val eventWithId = event.copy(id = eventId)
            eventsCollection.document(eventId).set(eventWithId).await()
            
            // Schedule reminder if enabled and context is available
            context?.let { ctx ->
                val notificationManager = EventNotificationManager(ctx)
                notificationManager.scheduleEventReminder(eventWithId)
            }
            
            Log.d("EventRepository", "Event created successfully: $eventId")
            Log.d("EventRepository", "Event details - Title: ${eventWithId.title}, Date: ${eventWithId.date}, Time: ${eventWithId.time}, IsAllDay: ${eventWithId.isAllDay}")
            Result.success(eventId)
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to create event", e)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val eventsCollection = getUserEventsCollection()
            eventsCollection.document(event.id).set(event).await()
            
            // Cancel old reminder and schedule new one if enabled and context is available
            context?.let { ctx ->
                val notificationManager = EventNotificationManager(ctx)
                notificationManager.cancelEventReminder(event.id)
                notificationManager.scheduleEventReminder(event)
            }
            
            Log.d("EventRepository", "Event updated successfully: ${event.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to update event", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val eventsCollection = getUserEventsCollection()
            eventsCollection.document(eventId).delete().await()
            
            // Cancel reminder when event is deleted
            context?.let { ctx ->
                val notificationManager = EventNotificationManager(ctx)
                notificationManager.cancelEventReminder(eventId)
            }
            
            Log.d("EventRepository", "Event deleted successfully: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to delete event", e)
            Result.failure(e)
        }
    }

    suspend fun getEvent(eventId: String): Result<Event?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val eventsCollection = getUserEventsCollection()
            val document = eventsCollection.document(eventId).get().await()
            val event = document.toObject(Event::class.java)
            Log.d("EventRepository", "Event retrieved: $eventId")
            Result.success(event)
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to get event", e)
            Result.failure(e)
        }
    }

    fun getUpcomingEvents(): Flow<List<Event>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("EventRepository", "User not authenticated")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val eventsCollection = getUserEventsCollection()
        Log.d("EventRepository", "Setting up upcoming events listener for user: ${currentUser.uid}")
        val listener = eventsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error getting upcoming events", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val event = doc.toObject(Event::class.java)
                        Log.d("EventRepository", "Parsed event: ${event?.title} on ${event?.date} at ${event?.time}")
                        event
                    } catch (e: Exception) {
                        Log.e("EventRepository", "Failed to parse event from document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d("EventRepository", "Retrieved ${events.size} total events from Firestore")
                
                // Filter events that are in the future (or today)
                val currentTime = System.currentTimeMillis()
                val upcomingEvents = events.filter { event ->
                    try {
                        val eventDateTime = if (event.isAllDay) {
                            // For all-day events, just parse the date and set to end of day
                            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsedDate = dateOnly.parse(event.date)
                            parsedDate?.let {
                                val calendar = Calendar.getInstance()
                                calendar.time = it
                                calendar.set(Calendar.HOUR_OF_DAY, 23)
                                calendar.set(Calendar.MINUTE, 59)
                                calendar.time
                            }
                        } else {
                            // For timed events, parse date and time
                            dateFormat.parse("${event.date} ${event.time}")
                        }
                        eventDateTime?.time ?: 0L >= currentTime
                    } catch (e: Exception) {
                        // If date parsing fails, include the event anyway
                        Log.w("EventRepository", "Failed to parse event date/time: ${event.date} ${event.time}", e)
                        true
                    }
                }.sortedBy { event ->
                    try {
                        val eventDateTime = if (event.isAllDay) {
                            // For all-day events, just parse the date
                            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dateOnly.parse(event.date)
                        } else {
                            // For timed events, parse date and time
                            dateFormat.parse("${event.date} ${event.time}")
                        }
                        eventDateTime?.time ?: 0L
                    } catch (e: Exception) {
                        Log.w("EventRepository", "Failed to parse event date/time for sorting: ${event.date} ${event.time}", e)
                        0L
                    }
                }
                
                Log.d("EventRepository", "Found ${upcomingEvents.size} upcoming events")
                trySend(upcomingEvents)
            }
        
        awaitClose { listener.remove() }
    }

    fun getPastEvents(): Flow<List<Event>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("EventRepository", "User not authenticated")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val eventsCollection = getUserEventsCollection()
        val listener = eventsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error getting past events", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val event = doc.toObject(Event::class.java)
                        Log.d("EventRepository", "Parsed event: ${event?.title} on ${event?.date} at ${event?.time}")
                        event
                    } catch (e: Exception) {
                        Log.e("EventRepository", "Failed to parse event from document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d("EventRepository", "Retrieved ${events.size} total events from Firestore")
                
                // Filter events that are in the past
                val currentTime = System.currentTimeMillis()
                val pastEvents = events.filter { event ->
                    try {
                        val eventDateTime = if (event.isAllDay) {
                            // For all-day events, just parse the date
                            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dateOnly.parse(event.date)
                        } else {
                            // For timed events, parse date and time
                            dateFormat.parse("${event.date} ${event.time}")
                        }
                        eventDateTime?.time ?: 0L < currentTime
                    } catch (e: Exception) {
                        Log.w("EventRepository", "Failed to parse past event date/time: ${event.date} ${event.time}", e)
                        false
                    }
                }.sortedByDescending { event ->
                    try {
                        val eventDateTime = if (event.isAllDay) {
                            // For all-day events, just parse the date
                            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dateOnly.parse(event.date)
                        } else {
                            // For timed events, parse date and time
                            dateFormat.parse("${event.date} ${event.time}")
                        }
                        eventDateTime?.time ?: 0L
                    } catch (e: Exception) {
                        Log.w("EventRepository", "Failed to parse past event date/time for sorting: ${event.date} ${event.time}", e)
                        0L
                    }
                }
                
                Log.d("EventRepository", "Found ${pastEvents.size} past events")
                trySend(pastEvents)
            }
        
        awaitClose { listener.remove() }
    }


    suspend fun assignOutfitToEvent(eventId: String, outfitId: String, outfitImageUrl: String?): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val eventsCollection = getUserEventsCollection()
            eventsCollection.document(eventId)
                .update(
                    "plannedOutfitId", outfitId,
                    "outfitImageUrl", outfitImageUrl
                ).await()
            Log.d("EventRepository", "Outfit assigned to event: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to assign outfit to event", e)
            Result.failure(e)
        }
    }

    fun getEventsWithOutfits(): Flow<List<Event>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("EventRepository", "User not authenticated")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val eventsCollection = getUserEventsCollection()
        val listener = eventsCollection
            .whereNotEqualTo("plannedOutfitId", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error getting events with outfits", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)
                }?.sortedBy { it.date } ?: emptyList()
                
                Log.d("EventRepository", "Found ${events.size} events with outfits")
                trySend(events)
            }
        
        awaitClose { listener.remove() }
    }

    fun getEventsByType(eventType: EventType): Flow<List<Event>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("EventRepository", "User not authenticated")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val eventsCollection = getUserEventsCollection()
        val listener = eventsCollection
            .whereEqualTo("eventType", eventType)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error getting events by type", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)
                }?.sortedBy { it.date } ?: emptyList()
                
                Log.d("EventRepository", "Found ${events.size} events of type ${eventType.displayName}")
                trySend(events)
            }
        
        awaitClose { listener.remove() }
    }
}