package com.TOTOMOFYP.VTOAPP.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class EventsFragment : BaseFragment() {
    
    private lateinit var viewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = EventViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]
        
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    EventsScreen(
                        viewModel = viewModel,
                        onAddEventClick = {
                            // Navigate to add event screen
                            val bundle = Bundle().apply {
                                putString("action", "add")
                            }
                            findNavController().navigate(R.id.action_navigation_events_to_addEventFragment, bundle)
                        },
                        onEventClick = { event ->
                            // Navigate to event details
                            val bundle = Bundle().apply {
                                putString("eventId", event.id)
                                putString("eventTitle", event.title)
                                putString("eventDate", event.date)
                                putString("eventTime", event.time)
                                putString("eventLocation", event.location)
                                putString("eventDescription", event.description)
                                putString("eventType", event.eventType.name)
                                putString("outfitImageUrl", event.outfitImageUrl)
                                putString("plannedOutfitId", event.plannedOutfitId)
                                putBoolean("reminderEnabled", event.reminderEnabled)
                                putBoolean("isAllDay", event.isAllDay)
                            }
                            findNavController().navigate(R.id.action_navigation_events_to_eventDetailsFragment, bundle)
                        }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh events when returning to this screen
        if (::viewModel.isInitialized) {
            android.util.Log.d("EventsFragment", "onResume: Refreshing events")
            viewModel.refreshEvents()
        }
    }
} 