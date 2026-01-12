package com.TOTOMOFYP.VTOAPP.ui.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            // Reschedule all event reminders after device restart
            rescheduleEventReminders(context)
        }
    }
    
    private fun rescheduleEventReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventRepository = EventRepository()
                val notificationManager = EventNotificationManager(context)
                
                // Get all upcoming events and reschedule their reminders
                val upcomingEvents = eventRepository.getUpcomingEvents().first()
                
                upcomingEvents.forEach { event ->
                    if (event.reminderEnabled) {
                        notificationManager.scheduleEventReminder(event)
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }
}