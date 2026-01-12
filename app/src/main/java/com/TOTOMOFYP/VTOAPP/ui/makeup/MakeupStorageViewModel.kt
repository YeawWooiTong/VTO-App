package com.TOTOMOFYP.VTOAPP.ui.makeup

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MakeupStorageViewModel(
    private val authViewModel: AuthViewModel
) : ViewModel() {
    
    private val makeupStorage = MakeupStorage()
    
    private val _uiState = MutableStateFlow<MakeupStorageUiState>(MakeupStorageUiState.Loading)
    val uiState: StateFlow<MakeupStorageUiState> = _uiState.asStateFlow()
    
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    sealed class MakeupStorageUiState {
        object Loading : MakeupStorageUiState()
        data class Success(val makeupLooks: List<MakeupLook>) : MakeupStorageUiState()
        data class Error(val message: String) : MakeupStorageUiState()
    }
    
    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
    
    init {
        loadMakeupLooks()
    }
    
    fun loadMakeupLooks() {
        viewModelScope.launch {
            _uiState.value = MakeupStorageUiState.Loading
            
            makeupStorage.getUserMakeupLooks()
                .onSuccess { looks ->
                    _uiState.value = MakeupStorageUiState.Success(looks)
                }
                .onFailure { exception ->
                    _uiState.value = MakeupStorageUiState.Error(
                        exception.message ?: "Failed to load makeup looks"
                    )
                }
        }
    }
    
    fun saveMakeupLook(name: String, image: Bitmap) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            
            makeupStorage.saveMakeupLook(name, image)
                .onSuccess {
                    _saveState.value = SaveState.Success
                    // Reload the looks to show the new one
                    loadMakeupLooks()
                }
                .onFailure { exception ->
                    _saveState.value = SaveState.Error(
                        exception.message ?: "Failed to save makeup look"
                    )
                }
        }
    }
    
    fun deleteMakeupLook(lookId: String) {
        viewModelScope.launch {
            makeupStorage.deleteMakeupLook(lookId)
                .onSuccess {
                    // Reload the looks to reflect the deletion
                    loadMakeupLooks()
                }
                .onFailure { exception ->
                    _uiState.value = MakeupStorageUiState.Error(
                        exception.message ?: "Failed to delete makeup look"
                    )
                }
        }
    }
    
    fun clearSaveState() {
        _saveState.value = SaveState.Idle
    }
    
    class Factory(
        private val authViewModel: AuthViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MakeupStorageViewModel::class.java)) {
                return MakeupStorageViewModel(authViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}