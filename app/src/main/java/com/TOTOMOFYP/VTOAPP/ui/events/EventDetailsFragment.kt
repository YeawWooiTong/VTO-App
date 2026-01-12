package com.TOTOMOFYP.VTOAPP.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.ui.base.BaseFragment
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme

class EventDetailsFragment : BaseFragment() {

    private lateinit var viewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = EventViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[EventViewModel::class.java]
        
        // Get event details from arguments
        val event = arguments?.let { bundle ->
            Event(
                id = bundle.getString("eventId", ""),
                title = bundle.getString("eventTitle", ""),
                date = bundle.getString("eventDate", ""),
                time = bundle.getString("eventTime", ""),
                location = bundle.getString("eventLocation", ""),
                description = bundle.getString("eventDescription", ""),
                eventType = try {
                    EventType.valueOf(bundle.getString("eventType", EventType.OTHER.name))
                } catch (e: Exception) {
                    EventType.OTHER
                },
                outfitImageUrl = bundle.getString("outfitImageUrl"),
                plannedOutfitId = bundle.getString("plannedOutfitId"),
                reminderEnabled = bundle.getBoolean("reminderEnabled", true),
                isAllDay = bundle.getBoolean("isAllDay", false)
            )
        } ?: return ComposeView(requireContext()) // Return empty view if no event data

        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    EventDetailsScreen(
                        event = event,
                        viewModel = viewModel,
                        onNavigateBack = {
                            findNavController().navigateUp()
                        },
                        onEditEvent = {
                            // Navigate to edit screen
                            val bundle = Bundle().apply {
                                putString("action", "edit")
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
                            findNavController().navigate(com.TOTOMOFYP.VTOAPP.R.id.addEventFragment, bundle)
                        },
                        onTryOnOutfit = {
                            // Navigate to try-on screen
                            // This would integrate with your existing try-on functionality
                            findNavController().navigate(com.TOTOMOFYP.VTOAPP.R.id.navigation_tryon)
                        },
                        onChangeOutfit = {
                            // Navigate to wardrobe or outfit selection
                            findNavController().navigate(com.TOTOMOFYP.VTOAPP.R.id.navigation_wardrobe)
                        }
                    )
                }
            }
        }
    }
}