package com.TOTOMOFYP.VTOAPP.ui.events

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.TOTOMOFYP.VTOAPP.MainActivity
import com.TOTOMOFYP.VTOAPP.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EventNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "event_reminders"
        private const val CHANNEL_NAME = "Event Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for upcoming events and outfit reminders"
        
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_TIME = "event_time"
        const val EXTRA_OUTFIT_URL = "outfit_url"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun scheduleEventReminder(event: Event) {
        android.util.Log.d("EventNotificationManager", "Attempting to schedule reminder for event: ${event.title}")
        android.util.Log.d("EventNotificationManager", "Event details - Date: ${event.date}, Time: ${event.time}, IsAllDay: ${event.isAllDay}, ReminderEnabled: ${event.reminderEnabled}, ReminderTime: ${event.reminderTime}")
        
        if (!event.reminderEnabled) {
            android.util.Log.d("EventNotificationManager", "Reminder not scheduled - reminderEnabled: ${event.reminderEnabled}")
            return
        }
        
        // Skip all-day events for now as they don't have specific times
        if (event.isAllDay) {
            android.util.Log.d("EventNotificationManager", "Skipping reminder for all-day event")
            return
        }
        
        val eventDateTime = parseEventDateTime(event.date, event.time)
        android.util.Log.d("EventNotificationManager", "Parsed event datetime: $eventDateTime (${if (eventDateTime != null) java.util.Date(eventDateTime) else "null"})")
        
        if (eventDateTime == null || eventDateTime <= System.currentTimeMillis()) {
            android.util.Log.d("EventNotificationManager", "Event datetime is null or in the past, not scheduling reminder")
            return
        }
        
        val reminderTime = eventDateTime - (event.reminderTime * 60 * 60 * 1000) // Convert hours to milliseconds
        val currentTime = System.currentTimeMillis()
        
        android.util.Log.d("EventNotificationManager", "Reminder time: $reminderTime (${java.util.Date(reminderTime)})")
        android.util.Log.d("EventNotificationManager", "Current time: $currentTime (${java.util.Date(currentTime)})")
        
        if (reminderTime <= currentTime) {
            android.util.Log.d("EventNotificationManager", "Reminder time is in the past, not scheduling")
            return
        }
        
        val delay = reminderTime - currentTime
        android.util.Log.d("EventNotificationManager", "Scheduling reminder with delay: ${delay}ms (${delay / 1000 / 60} minutes)")
        
        val workData = Data.Builder()
            .putString(EXTRA_EVENT_ID, event.id)
            .putString(EXTRA_EVENT_TITLE, event.title)
            .putString(EXTRA_EVENT_TIME, event.time)
            .putString(EXTRA_OUTFIT_URL, event.outfitImageUrl)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .addTag("event_reminder_${event.id}")
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        android.util.Log.d("EventNotificationManager", "Reminder scheduled successfully for event: ${event.title}")
    }
    
    fun cancelEventReminder(eventId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("event_reminder_$eventId")
    }
    
    
    fun showEventReminderNotification(
        eventId: String,
        eventTitle: String,
        eventTime: String,
        outfitImageUrl: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon
            .setContentTitle("Event Reminder")
            .setContentText("$eventTitle is starting soon at $eventTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$eventTitle is starting soon at $eventTime. ${if (outfitImageUrl != null) "Your planned outfit is ready!" else "Don't forget to choose your outfit!"}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
        
        // Add action buttons
        val viewOutfitIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra("action", "view_outfit")
        }
        val viewOutfitPendingIntent = PendingIntent.getActivity(
            context,
            "${eventId}_outfit".hashCode(),
            viewOutfitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        notificationBuilder.addAction(
            android.R.drawable.ic_menu_gallery, // Using system icon
            if (outfitImageUrl != null) "View Outfit" else "Choose Outfit",
            viewOutfitPendingIntent
        )
        
        val snoozeIntent = Intent(context, EventReminderReceiver::class.java).apply {
            action = "SNOOZE_REMINDER"
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_EVENT_TIME, eventTime)
            putExtra(EXTRA_OUTFIT_URL, outfitImageUrl)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            "${eventId}_snooze".hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        notificationBuilder.addAction(
            android.R.drawable.ic_popup_reminder, // Using system icon
            "Snooze 15min",
            snoozePendingIntent
        )
        
        try {
            NotificationManagerCompat.from(context).notify(eventId.hashCode(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            e.printStackTrace()
        }
    }
    
    fun showMorningOfReminder(events: List<Event>) {
        if (events.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            "morning_reminder".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val eventTitles = events.map { it.title }
        val title = if (events.size == 1) {
            "Event Today: ${eventTitles.first()}"
        } else {
            "You have ${events.size} events today"
        }
        
        val text = if (events.size == 1) {
            val event = events.first()
            "${event.title} at ${event.time}. ${if (event.outfitImageUrl != null) "Your outfit is ready!" else "Don't forget to choose your outfit!"}"
        } else {
            eventTitles.joinToString(", ")
        }
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            NotificationManagerCompat.from(context).notify("morning_reminder".hashCode(), notificationBuilder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun parseEventDateTime(date: String, time: String): Long? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateTime = dateFormat.parse("$date $time")
            dateTime?.time
        } catch (e: Exception) {
            null
        }
    }
}

class EventReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        android.util.Log.d("EventReminderWorker", "EventReminderWorker triggered!")
        
        val eventId = inputData.getString(EventNotificationManager.EXTRA_EVENT_ID) ?: run {
            android.util.Log.e("EventReminderWorker", "Missing eventId")
            return Result.failure()
        }
        val eventTitle = inputData.getString(EventNotificationManager.EXTRA_EVENT_TITLE) ?: run {
            android.util.Log.e("EventReminderWorker", "Missing eventTitle")
            return Result.failure()
        }
        val eventTime = inputData.getString(EventNotificationManager.EXTRA_EVENT_TIME) ?: run {
            android.util.Log.e("EventReminderWorker", "Missing eventTime")
            return Result.failure()
        }
        val outfitImageUrl = inputData.getString(EventNotificationManager.EXTRA_OUTFIT_URL)
        
        android.util.Log.d("EventReminderWorker", "Showing notification for event: $eventTitle at $eventTime")
        
        val notificationManager = EventNotificationManager(applicationContext)
        notificationManager.showEventReminderNotification(eventId, eventTitle, eventTime, outfitImageUrl)
        
        android.util.Log.d("EventReminderWorker", "Notification shown successfully")
        return Result.success()
    }
}

class EventReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "SNOOZE_REMINDER" -> {
                val eventId = intent.getStringExtra(EventNotificationManager.EXTRA_EVENT_ID) ?: return
                val eventTitle = intent.getStringExtra(EventNotificationManager.EXTRA_EVENT_TITLE) ?: return
                val eventTime = intent.getStringExtra(EventNotificationManager.EXTRA_EVENT_TIME) ?: return
                val outfitImageUrl = intent.getStringExtra(EventNotificationManager.EXTRA_OUTFIT_URL)
                
                // Cancel current notification
                NotificationManagerCompat.from(context).cancel(eventId.hashCode())
                
                // Schedule snooze notification after 15 minutes
                val workData = Data.Builder()
                    .putString(EventNotificationManager.EXTRA_EVENT_ID, eventId)
                    .putString(EventNotificationManager.EXTRA_EVENT_TITLE, eventTitle)
                    .putString(EventNotificationManager.EXTRA_EVENT_TIME, eventTime)
                    .putString(EventNotificationManager.EXTRA_OUTFIT_URL, outfitImageUrl)
                    .build()
                
                val workRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .setInputData(workData)
                    .addTag("event_reminder_snooze_$eventId")
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}