package com.TOTOMOFYP.VTOAPP.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.google.firebase.auth.FirebaseAuth

data class UserProfile(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val stylePreference: String = "", // For backward compatibility
    val stylePreferences: List<String> = emptyList(), // New multiple preferences
    val hasCompletedOnboarding: Boolean = false,
    val createdAt: Long = 0L
)

class ProfileViewModel : ViewModel() {
    private val TAG = "ProfileViewModel"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _isLoading.value = true
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    _isLoading.value = false
                    if (document.exists()) {
                        // Handle both old single preference and new multiple preferences
                        val singlePreference = document.getString("stylePreference") ?: ""
                        val multiplePreferences = document.get("stylePreferences") as? List<String> ?: emptyList()

                        val profile = UserProfile(
                            fullName = document.getString("fullName") ?: "",
                            dateOfBirth = document.getString("dateOfBirth") ?: "",
                            stylePreference = singlePreference, // Keep for backward compatibility
                            stylePreferences = if (multiplePreferences.isNotEmpty()) multiplePreferences else if (singlePreference.isNotEmpty()) listOf(singlePreference) else emptyList(),
                            hasCompletedOnboarding = document.getBoolean("hasCompletedOnboarding") ?: false,
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                        _userProfile.value = profile
                        Log.d(TAG, "User profile loaded: $profile")
                    } else {
                        Log.d(TAG, "No profile document found")
                        _userProfile.value = UserProfile()
                    }
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    Log.e(TAG, "Error loading profile: ${exception.message}")
                    _userProfile.value = UserProfile()
                }
        }
    }

    fun updateProfile(updatedProfile: UserProfile, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _isLoading.value = true

            val userData = hashMapOf<String, Any>(
                "fullName" to updatedProfile.fullName,
                "dateOfBirth" to updatedProfile.dateOfBirth,
                "stylePreference" to (updatedProfile.stylePreferences.firstOrNull() ?: ""), // Backward compatibility
                "stylePreferences" to updatedProfile.stylePreferences,
                "hasCompletedOnboarding" to true,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(currentUser.uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    _isLoading.value = false
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "Profile updated successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    Log.e(TAG, "Error updating profile: ${exception.message}")
                    onError(exception.message ?: "Failed to update profile")
                }
        } else {
            onError("User not authenticated")
        }
    }
}

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        onSignOut = { signOut() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    private fun signOut() {
        // Use AuthViewModel to sign out (will trigger observer in MainActivity)
        authViewModel.signOut()
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun ProfileScreen(
        authViewModel: AuthViewModel,
        profileViewModel: ProfileViewModel,
        onSignOut: () -> Unit
    ) {
        val currentUser by authViewModel.currentUser.observeAsState()
        val userProfile by profileViewModel.userProfile.observeAsState()
        val isLoading by profileViewModel.isLoading.observeAsState()
        val context = LocalContext.current

        // Edit profile dialog state
        val showEditDialog = remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                // Empty top bar similar to wardrobe screen
                Spacer(modifier = Modifier.height(48.dp))
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
                )

                // Profile Header
                ProfileHeader(
                    user = currentUser,
                    userProfile = userProfile,
                    isLoading = isLoading ?: false,
                    onEditProfile = {
                        showEditDialog.value = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))



                // Style Preferences Card
                StylePreferencesCard(
                    userProfile = userProfile,
                    onUpdatePreferences = {
                        // TODO: Navigate to update preferences screen
                        Toast.makeText(context, "Update preferences - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                )





                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Sign Out",
                        color = MaterialTheme.colorScheme.onError
                    )
                }

                Spacer(modifier = Modifier.height(88.dp)) // Bottom padding for navigation
            }
        }

        // Edit Profile Dialog
        if (showEditDialog.value) {
            EditProfileDialog(
                userProfile = userProfile ?: UserProfile(),
                onDismiss = { showEditDialog.value = false },
                onSave = { updatedProfile ->
                    profileViewModel.updateProfile(
                        updatedProfile = updatedProfile,
                        onSuccess = {
                            showEditDialog.value = false
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
    @Composable
    fun ProfileHeader(
        user: com.google.firebase.auth.FirebaseUser?,
        userProfile: UserProfile?,
        isLoading: Boolean,
        onEditProfile: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = userProfile?.fullName?.takeIf { it.isNotBlank() }
                                ?: user?.displayName
                                ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user?.email ?: "user@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )

                    if (!isLoading && userProfile?.dateOfBirth?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Born: ${userProfile.dateOfBirth}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Edit Profile")
                    }
                }
            }
        }
    }

    @Composable
    fun MeasurementsCard(
        onUpdateMeasurements: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onUpdateMeasurements) {
                        Text("Edit")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Measurement items
                MeasurementItem(label = "Height", value = "Not set")
                Spacer(modifier = Modifier.height(12.dp))
                MeasurementItem(label = "Weight", value = "Not set")
                Spacer(modifier = Modifier.height(12.dp))
                MeasurementItem(label = "Sizes", value = "Not set")
            }
        }
    }

    @Composable
    fun MeasurementItem(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun StylePreferencesCard(
        userProfile: UserProfile?,
        onUpdatePreferences: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Style Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onUpdatePreferences) {
                        Text("Edit")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show actual style preferences or placeholder
                val displayPreferences = userProfile?.stylePreferences?.takeIf { it.isNotEmpty() }
                    ?: userProfile?.stylePreference?.takeIf { it.isNotBlank() }?.let { listOf(it) }
                    ?: emptyList()

                if (displayPreferences.isNotEmpty()) {
                    // Display preferences in a flowing layout
                    val chunkedPreferences = displayPreferences.chunked(3)
                    chunkedPreferences.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { preference ->
                                StyleChip(text = preference)
                            }
                        }
                        if (row != chunkedPreferences.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (displayPreferences.size == 1) {
                            "Your preferred style: ${displayPreferences.first()}"
                        } else {
                            "Your preferred styles: ${displayPreferences.joinToString(", ")}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No style preference set yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Set your style preferences to get better outfit recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    fun StyleChip(text: String) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    @Composable
    fun SettingsCard(
//        onSettingsClick: () -> Unit,
//        onHelpClick: () -> Unit,
        onAboutClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
//                SettingsItem(
//                    icon = Icons.Default.Settings,
//                    title = "Settings",
//                    onClick = onSettingsClick
//                )
//
//                Divider()
//
//                SettingsItem(
//                    icon = Icons.Default.Help,
//                    title = "Help & Support",
//                    onClick = onHelpClick
//                )
//
//                Divider()

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    onClick = onAboutClick
                )
            }
        }
    }

    @Composable
    fun SettingsItem(
        icon: ImageVector,
        title: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    @Composable
    fun EditProfileDialog(
        userProfile: UserProfile,
        onDismiss: () -> Unit,
        onSave: (UserProfile) -> Unit
    ) {
        val fullName = remember { mutableStateOf(userProfile.fullName) }
        val dateOfBirth = remember { mutableStateOf(userProfile.dateOfBirth) }
        val selectedStyles = remember { mutableStateOf(userProfile.stylePreferences.toSet()) }
        val showDatePicker = remember { mutableStateOf(false) }
        val showStylePicker = remember { mutableStateOf(false) }

        val styleOptions = listOf(
            "Casual", "Formal", "Business", "Party", "Wedding",
            "Sports", "Travel", "Loungewear", "Traditional", "Seasonal"
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Full Name
                    OutlinedTextField(
                        value = fullName.value,
                        onValueChange = { fullName.value = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date of Birth
                    OutlinedTextField(
                        value = dateOfBirth.value,
                        onValueChange = { },
                        label = { Text("Date of Birth") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker.value = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Style Preferences
                    OutlinedTextField(
                        value = if (selectedStyles.value.isEmpty()) "" else selectedStyles.value.joinToString(", "),
                        onValueChange = { },
                        label = { Text("Style Preferences") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showStylePicker.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Styles"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStylePicker.value = true },
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedProfile = userProfile.copy(
                            fullName = fullName.value,
                            dateOfBirth = dateOfBirth.value,
                            stylePreferences = selectedStyles.value.toList()
                        )
                        onSave(updatedProfile)
                    },
                    enabled = fullName.value.isNotBlank() && dateOfBirth.value.isNotBlank() && selectedStyles.value.isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )

        // Date picker dialog
        if (showDatePicker.value) {
            SimpleDatePickerDialog(
                currentDate = dateOfBirth.value,
                onDateSelected = { newDate ->
                    dateOfBirth.value = newDate
                    showDatePicker.value = false
                },
                onDismiss = { showDatePicker.value = false }
            )
        }

        // Style picker dialog
        if (showStylePicker.value) {
            StylePickerDialog(
                currentStyles = selectedStyles.value,
                styleOptions = styleOptions,
                onStylesSelected = { newStyles ->
                    selectedStyles.value = newStyles
                    showStylePicker.value = false
                },
                onDismiss = { showStylePicker.value = false }
            )
        }
    }

    @Composable
    fun SimpleDatePickerDialog(
        currentDate: String,
        onDateSelected: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()

        // Parse current date if available
        if (currentDate.isNotBlank()) {
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                formatter.parse(currentDate)?.let { calendar.time = it }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }

        LaunchedEffect(Unit) {
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    onDateSelected(formatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.setOnDismissListener { onDismiss() }
            datePickerDialog.show()
        }
    }

    @Composable
    fun StylePickerDialog(
        currentStyles: Set<String>,
        styleOptions: List<String>,
        onStylesSelected: (Set<String>) -> Unit,
        onDismiss: () -> Unit
    ) {
        val selectedStyles = remember { mutableStateOf(currentStyles.toMutableSet()) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Style Preferences") },
            text = {
                LazyColumn {
                    items(styleOptions) { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedStyles.value
                                    if (current.contains(style)) {
                                        current.remove(style)
                                    } else {
                                        current.add(style)
                                    }
                                    selectedStyles.value = current.toMutableSet()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = selectedStyles.value.contains(style),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onStylesSelected(selectedStyles.value) },
                    enabled = selectedStyles.value.isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
} 