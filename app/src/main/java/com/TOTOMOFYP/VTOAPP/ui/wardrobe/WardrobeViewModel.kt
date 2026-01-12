package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeViewModel(private val authViewModel: AuthViewModel) : ViewModel() {
    
    private val clothingImageStorage = ClothingImageStorage()
    
    // UI State
    private val _uiState = MutableStateFlow<WardrobeUiState>(WardrobeUiState.Loading)
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()
    
    // Current category
    private val _currentCategory = MutableStateFlow(WardrobeCategory.ALL)
    val currentCategory: StateFlow<WardrobeCategory> = _currentCategory.asStateFlow()

    // Segmentation and camera states
    private val _capturedImageUri = MutableStateFlow<String?>(null)
    val capturedImageUri: StateFlow<String?> = _capturedImageUri.asStateFlow()
    
    private val _segmentedBitmap = MutableStateFlow<Bitmap?>(null)
    val segmentedBitmap: StateFlow<Bitmap?> = _segmentedBitmap.asStateFlow()
    
    fun setCategory(category: WardrobeCategory) {
        _currentCategory.value = category
        loadClothingItems()
    }
    
    fun setCapturedImageUri(uri: String?) {
        _capturedImageUri.value = uri
    }
    
    fun setSegmentedBitmap(bitmap: Bitmap?) {
        _segmentedBitmap.value = bitmap
    }

    init {
        loadClothingItems()
    }
    
    fun loadClothingItems() {
        viewModelScope.launch {
            _uiState.value = WardrobeUiState.Loading
            
            val userId = authViewModel.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = WardrobeUiState.Error("User not logged in")
                return@launch
            }
            
            clothingImageStorage.getClothingImages(
                userId = userId,
                category = _currentCategory.value,
                onSuccess = { items ->
                    _uiState.value = if (items.isEmpty()) {
                        WardrobeUiState.Empty
                    } else {
                        WardrobeUiState.Success(items)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = WardrobeUiState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }
    
    // Local upload removed - server handles upload via /categorize/{user_id} endpoint
    // After server processing, data is automatically saved to Firebase and loaded via loadClothingItems()
    
    
    fun deleteClothingItem(clothingId: String) {
        viewModelScope.launch {
            val userId = authViewModel.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = WardrobeUiState.Error("User not logged in")
                return@launch
            }
            
            clothingImageStorage.deleteClothingItem(
                userId = userId,
                clothingId = clothingId,
                onSuccess = {
                    loadClothingItems()
                },
                onFailure = { exception ->
                    _uiState.value = WardrobeUiState.Error(exception.message ?: "Deletion failed")
                }
            )
        }
    }
    
    // Factory to create ViewModel with dependencies
    class Factory(private val authViewModel: AuthViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WardrobeViewModel::class.java)) {
                return WardrobeViewModel(authViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Sealed class to represent the UI state
sealed class WardrobeUiState {
    object Loading : WardrobeUiState()
    object Empty : WardrobeUiState()
    data class Success(val items: List<ClothingImageItem>) : WardrobeUiState()
    data class Error(val message: String) : WardrobeUiState()
} 