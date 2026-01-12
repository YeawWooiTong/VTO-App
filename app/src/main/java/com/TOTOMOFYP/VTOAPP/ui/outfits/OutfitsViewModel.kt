package com.TOTOMOFYP.VTOAPP.ui.outfits

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OutfitsViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val outfitStorage = OutfitStorage()

    // UI state for outfits
    sealed class OutfitsUiState {
        object Loading : OutfitsUiState()
        data class Success(val outfits: List<Outfit>) : OutfitsUiState()
        data class Error(val message: String) : OutfitsUiState()
        object Empty : OutfitsUiState()
    }

    // Filter state - Simplified single-word categories
    data class FilterState(
        val selectedCategory: String = "All",
        val categories: List<String> = listOf(
            "All",
            "Casual",
            "Formal",
            "Business",
            "Party",
            "Wedding",
            "Sports",
            "Travel",
            "Loungewear",
            "Traditional",
            "Seasonal"
        )
    )

    // Mapping from AI metadata values to simplified categories
    private val categoryMapping = mapOf(
        "casual" to "Casual",
        "formal" to "Formal",
        "business" to "Business",
        "office" to "Business",
        "business / office" to "Business",
        "party" to "Party",
        "celebration" to "Party",
        "party / celebration" to "Party",
        "wedding" to "Wedding",
        "sports" to "Sports",
        "active" to "Sports",
        "sports / active" to "Sports",
        "travel" to "Travel",
        "vacation" to "Travel",
        "travel / vacation" to "Travel",
        "loungewear" to "Loungewear",
        "home" to "Loungewear",
        "loungewear / home" to "Loungewear",
        "traditional" to "Traditional",
        "cultural" to "Traditional",
        "traditional / cultural" to "Traditional",
        "seasonal" to "Seasonal",
        "weather" to "Seasonal",
        "seasonal / weather-based" to "Seasonal"
    )

    private val _uiState = MutableStateFlow<OutfitsUiState>(OutfitsUiState.Loading)
    val uiState: StateFlow<OutfitsUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private var allOutfits: List<Outfit> = emptyList()

    init {
        loadOutfits()
    }

    /**
     * Load all outfits for the current user
     */
    fun loadOutfits() {
        viewModelScope.launch {
            _uiState.value = OutfitsUiState.Loading

            val userId = authViewModel.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = OutfitsUiState.Error("User not logged in")
                return@launch
            }

            outfitStorage.getOutfits(userId).fold(
                onSuccess = { outfits ->
                    allOutfits = outfits
                    applyFilter()
                },
                onFailure = { exception ->
                    _uiState.value = OutfitsUiState.Error(exception.message ?: "Failed to load outfits")
                }
            )
        }
    }

    /**
     * Apply current filter to outfits - Using simplified categories with mapping
     */
    private fun applyFilter() {
        val filteredOutfits = if (_filterState.value.selectedCategory == "All") {
            allOutfits
        } else {
            val selectedCategory = _filterState.value.selectedCategory
            Log.d("OutfitsViewModel", "Filtering for category: $selectedCategory")

            allOutfits.filter { outfit ->
                Log.d("OutfitsViewModel", "Outfit ${outfit.name}: occasion='${outfit.metadata?.occasion}', style='${outfit.metadata?.style}'")

                // Check if outfit's occasion/style maps to the selected category
                val outfitOccasion = outfit.metadata?.occasion?.lowercase() ?: ""
                val outfitStyle = outfit.metadata?.style?.lowercase() ?: ""

                val mappedOccasion = categoryMapping[outfitOccasion]
                val mappedStyle = categoryMapping[outfitStyle]

                // Match if either occasion or style maps to the selected category
                mappedOccasion == selectedCategory || mappedStyle == selectedCategory
            }
        }

        _uiState.value = if (filteredOutfits.isEmpty()) {
            if (allOutfits.isEmpty()) {
                OutfitsUiState.Empty
            } else {
                OutfitsUiState.Success(emptyList()) // Filtered but no results
            }
        } else {
            OutfitsUiState.Success(filteredOutfits)
        }
    }

    /**
     * Set filter category
     */
    fun setFilterCategory(category: String) {
        _filterState.value = _filterState.value.copy(selectedCategory = category)
        applyFilter()
    }

    /**
     * Toggle the favorite status of an outfit
     */
    fun toggleFavorite(outfitId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val userId = authViewModel.currentUser.value?.uid ?: return@launch

            Log.d("OutfitsViewModel", "Toggling favorite for outfit $outfitId to $isFavorite")

            outfitStorage.toggleFavorite(userId, outfitId, isFavorite).fold(
                onSuccess = { updatedFavoriteStatus ->
                    Log.d("OutfitsViewModel", "Successfully updated favorite status to $updatedFavoriteStatus")
                    // Reload outfits to update UI
                    loadOutfits()
                },
                onFailure = { exception ->
                    Log.e("OutfitsViewModel", "Failed to toggle favorite for outfit $outfitId", exception)
                    // Could add error state or toast message here in the future
                }
            )
        }
    }

    /**
     * Delete an outfit
     */
    fun deleteOutfit(outfitId: String) {
        viewModelScope.launch {
            val userId = authViewModel.currentUser.value?.uid ?: return@launch

            outfitStorage.deleteOutfit(userId, outfitId).fold(
                onSuccess = {
                    // Reload outfits to update UI
                    loadOutfits()
                },
                onFailure = { exception ->
                    // Handle error
                }
            )
        }
    }

    /**
     * Refresh outfits - force reload from server
     */
    fun refreshOutfits() {
        viewModelScope.launch {
            val userId = authViewModel.currentUser.value?.uid ?: return@launch

            // Clear cache to force fresh data
            outfitStorage.clearCache(userId)
            loadOutfits()
        }
    }

    // Factory to create ViewModel with dependencies
    class Factory(private val authViewModel: AuthViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OutfitsViewModel::class.java)) {
                return OutfitsViewModel(authViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 