package com.TOTOMOFYP.VTOAPP.auth

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ViewModel to handle authentication state and operations.
 * Used to bridge Compose authentication screens with XML-based main app screens.
 */
class AuthViewModel : ViewModel() {
    
    private val TAG = "AuthViewModel"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Current user LiveData
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    // Authentication state
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    // Whether user has completed onboarding
    private val _hasCompletedOnboarding = MutableLiveData<Boolean>()
    val hasCompletedOnboarding: LiveData<Boolean> = _hasCompletedOnboarding
    
    // Initialization
    init {
        // Set initial values
        _currentUser.value = auth.currentUser
        _isAuthenticated.value = auth.currentUser != null
        
        // Check if user has completed onboarding
        checkOnboardingStatus()
        
        // Listen for authentication state changes
        auth.addAuthStateListener { firebaseAuth ->
            Log.d(TAG, "Auth state changed, user: ${firebaseAuth.currentUser?.uid}")
            _currentUser.value = firebaseAuth.currentUser
            _isAuthenticated.value = firebaseAuth.currentUser != null
            
            // Reset onboarding state on logout
            if (firebaseAuth.currentUser == null) {
                _hasCompletedOnboarding.value = false
            } else {
                // Check onboarding status for current user
                checkOnboardingStatus()
            }
        }
    }
    
    /**
     * Check if the current user has completed onboarding
     */
    private fun checkOnboardingStatus() {
        auth.currentUser?.let { user ->
            Log.d(TAG, "Checking onboarding status for user: ${user.uid}")
            // Check in Firebase if user has completed onboarding
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val hasCompleted = document.getBoolean("hasCompletedOnboarding") ?: false
                        Log.d(TAG, "Firestore onboarding status: $hasCompleted")
                        _hasCompletedOnboarding.value = hasCompleted
                    } else {
                        // Document doesn't exist - treat as not completed onboarding
                        Log.d(TAG, "No user document found - setting onboarding to false")
                        _hasCompletedOnboarding.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error checking onboarding status: ${exception.message}")
                    // Default to not completed
                    _hasCompletedOnboarding.value = false
                }
        } ?: run {
            _hasCompletedOnboarding.value = false
        }
    }
    
    /**
     * Sign in with email and password
     */
    fun signInWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Sign in successful")
                checkOnboardingStatus()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Sign in failed: ${exception.message}")
                onError(exception.message ?: "Authentication failed")
            }
    }
    
    /**
     * Register with email and password
     */
    fun registerWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Registration successful")
                _hasCompletedOnboarding.value = false
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Registration failed: ${exception.message}")
                onError(exception.message ?: "Registration failed")
            }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        Log.d(TAG, "Signing out")
        auth.signOut()
    }
    
    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding() {
        Log.d(TAG, "Marking onboarding as complete")
        _hasCompletedOnboarding.value = true
        
        // Save onboarding status to Firebase
        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .set(mapOf("hasCompletedOnboarding" to true), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Onboarding status saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving onboarding status: ${e.message}")
                }
        }
    }
} 