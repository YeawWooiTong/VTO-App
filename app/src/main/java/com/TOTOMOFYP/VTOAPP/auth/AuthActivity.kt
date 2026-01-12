package com.TOTOMOFYP.VTOAPP.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.Observer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.TOTOMOFYP.VTOAPP.MainActivity
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity to host all authentication screens (login, register, onboarding).
 * This uses Compose for the auth UI while the main app uses XML layouts.
 */
class AuthActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val TAG = "AuthActivity"
    private val auth = FirebaseAuth.getInstance()
    
    // Observers
    private lateinit var authObserver: Observer<Boolean>
    private lateinit var onboardingObserver: Observer<Boolean>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "AuthActivity onCreate called")
        Log.d(TAG, "Current user: ${auth.currentUser?.uid}, isAuthenticated: ${auth.currentUser != null}")
        
        // Create observers
        authObserver = Observer<Boolean> { isAuthenticated ->
            Log.d(TAG, "Authentication state changed: isAuthenticated = $isAuthenticated")
            if (isAuthenticated) {
                val user = auth.currentUser
                if (user != null) {
                    val isNewUser = user.metadata?.creationTimestamp == user.metadata?.lastSignInTimestamp
                    Log.d(TAG, "User metadata - creation: ${user.metadata?.creationTimestamp}, lastSignIn: ${user.metadata?.lastSignInTimestamp}")
                    Log.d(TAG, "Is new user: $isNewUser")
                    
                    if (isNewUser) {
                        Log.d(TAG, "New user detected - checking if they need onboarding")
                        // Even for new users, check onboarding state in case they completed it
                        checkOnboardingState()
                    } else {
                        Log.d(TAG, "Returning user - checking onboarding state")
                        // For returning users, check onboarding state
                        checkOnboardingState()
                    }
                }
            }
        }
        
        onboardingObserver = Observer<Boolean> { hasCompleted ->
            Log.d(TAG, "Onboarding state changed: hasCompleted = $hasCompleted")
            if (hasCompleted) {
                // If authenticated and completed onboarding, go to main app
                Log.d(TAG, "User is authenticated and has completed onboarding, starting MainActivity")
                startMainActivity()
            }
        }
        
        // Set up the Compose UI
        setContent {
            VTOAppTheme {
                AuthNavigation()
            }
        }
        
        // Observe authentication state after UI is set up
        authViewModel.isAuthenticated.observe(this, authObserver)
    }
    
    private fun checkOnboardingState() {
        // Start observing onboarding state
        authViewModel.hasCompletedOnboarding.observe(this, onboardingObserver)
    }
    
    private fun navigateBasedOnOnboardingState(hasCompleted: Boolean) {
        if (hasCompleted) {
            Log.d(TAG, "User has completed onboarding - going to main activity")
            startMainActivity()
        } else {
            Log.d(TAG, "User needs to complete onboarding - showing UserInfoScreen1")
            // This could be called from login callback, so we need to check if we're still in auth flow
            if (this::class.java.simpleName == "AuthActivity") {
                // We're in auth activity, safe to navigate
                findViewById<androidx.fragment.app.FragmentContainerView>(android.R.id.content)?.let {
                    // Navigate to onboarding if we're not already there
                    // This will be handled by the onboarding observer
                }
            }
        }
    }
    
    /**
     * Check if the current user is a new user
     */
    private fun isNewUser(): Boolean {
        val user = auth.currentUser
        return user?.let { 
            user.metadata?.creationTimestamp == user.metadata?.lastSignInTimestamp 
        } ?: false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up observers
        authViewModel.isAuthenticated.removeObserver(authObserver)
        authViewModel.hasCompletedOnboarding.removeObserver(onboardingObserver)
    }
    
    @Composable
    private fun AuthNavigation() {
        val navController = rememberNavController()
        
        NavHost(navController = navController, startDestination = "login") {
            // Login screen
            composable("login") { 
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = { 
                        Log.d(TAG, "Login success callback triggered")
                        val user = auth.currentUser
                        if (user != null) {
                            Log.d(TAG, "User logged in successfully - checking onboarding state")
                            // Always check onboarding state instead of relying on metadata
                            checkOnboardingState()
                        } else {
                            Log.e(TAG, "ERROR: User is null after login")
                            Toast.makeText(this@AuthActivity, "Login issue. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) 
            }
            
            // Register screen
            composable("register") { 
                RegisterScreen(
                    navController = navController,
                    onRegisterSuccess = {
                        Log.d(TAG, "Register success - new users need onboarding")
                        val user = auth.currentUser
                        if (user != null) {
                            Log.d(TAG, "New user registered - navigating to onboarding")
                            navController.navigate("userInfo1")
                        } else {
                            Log.e(TAG, "ERROR: User is null after registration")
                            Toast.makeText(this@AuthActivity, "Registration issue. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) 
            }
            
            // User info screen (onboarding)
            composable("userInfo1") { 
                UserInfoScreen1(
                    navController = navController,
                    onComplete = {
                        Log.d(TAG, "UserInfoScreen1: onComplete callback triggered")
                        // Check if user is still authenticated
                        if (auth.currentUser != null) {
                            Log.d(TAG, "User is authenticated, completing onboarding")
                            authViewModel.completeOnboarding()
                            startMainActivity()
                        } else {
                            Log.e(TAG, "ERROR: User is not authenticated at end of onboarding")
                            Toast.makeText(this@AuthActivity, 
                                "Authentication issue. Please login again.", 
                                Toast.LENGTH_LONG).show()
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                ) 
            }
        }
    }
    
    private fun startMainActivity() {
        Log.d(TAG, "Starting MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 