package com.TOTOMOFYP.VTOAPP

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.TOTOMOFYP.VTOAPP.auth.AuthActivity
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val TAG = "MainActivity"
    
    // Notification permission launcher
    private val requestNotificationPermission = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Notification permission granted")
            } else {
                Log.d(TAG, "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure edge-to-edge content - this tells the system to draw behind the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        Log.d(TAG, "MainActivity onCreate")
        
        // Note: Firebase is already initialized in MyApplication class
        
        // Log current auth state
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "Current user: ${currentUser?.uid}, isAuthenticated: ${currentUser != null}")
        
        // Check if user is authenticated
        if (currentUser == null) {
            Log.d(TAG, "No authenticated user, redirecting to AuthActivity")
            redirectToAuth()
            return
        }
        
        // Set up view binding first so we can show a loading UI if needed
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up proper window insets handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // Only apply bottom padding to avoid status bar extra space
            binding.bottomNavContainer.root.updatePadding(bottom = navigationBars.bottom)
            // Return insets unchanged so they can propagate to other views if needed
            windowInsets
        }
        
        // Check if onboarding is complete
        var onboardingChecked = false
        authViewModel.hasCompletedOnboarding.observe(this, Observer { hasCompleted ->
            if (!onboardingChecked) {
                onboardingChecked = true
                Log.d(TAG, "Onboarding status checked: $hasCompleted")
                if (hasCompleted != true) {
                    Log.d(TAG, "Onboarding not completed, redirecting to AuthActivity")
                    redirectToAuth()
                    return@Observer
                }
                
                // Only if onboarding is complete, set up the main app UI
                setupMainUI()
                // Request notification permission after UI is set up
                requestNotificationPermissionIfNeeded()
            }
        })
        
        // Observe authentication state for changes (like sign out)
        authViewModel.isAuthenticated.observe(this, Observer { isAuthenticated ->
            Log.d(TAG, "Authentication state changed: $isAuthenticated")
            if (!isAuthenticated) {
                // If signed out, redirect to auth activity
                Log.d(TAG, "User signed out, redirecting to AuthActivity")
                redirectToAuth()
            }
        })
    }
    
    /**
     * Set up the main UI components after authentication is verified
     */
    private fun setupMainUI() {
        // Set up navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Connect bottom navigation with navigation controller
        binding.bottomNavContainer.bottomNavigation.apply {
            setupWithNavController(navController)
            
            // Override the default navigation behavior to always go to the initial state
            setOnItemSelectedListener { menuItem ->
                // Clear the back stack to prevent nested navigation
                navController.popBackStack()
                
                // Navigate to the selected destination
                when (menuItem.itemId) {
                    R.id.navigation_outfits -> navController.navigate(R.id.navigation_outfits)
                    R.id.navigation_wardrobe -> navController.navigate(R.id.navigation_wardrobe)
                    R.id.navigation_try_on -> navController.navigate(R.id.navigation_try_on)
                    R.id.navigation_events -> navController.navigate(R.id.navigation_events)
                    R.id.navigation_profile -> navController.navigate(R.id.navigation_profile)
                }
                true
            }
        }
        
        // Set up the Try On FAB
        binding.bottomNavContainer.fabTryon.setOnClickListener {
            // Navigate directly to the TryOn initial screen regardless of current state
            navController.popBackStack()
            navController.navigate(R.id.navigation_try_on)
        }
    }
    
    /**
     * Redirect to authentication activity
     */
    private fun redirectToAuth() {
        Log.d(TAG, "Redirecting to AuthActivity")
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Request notification permission for Android 13+ (API 33+)
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version")
        }
    }
}
