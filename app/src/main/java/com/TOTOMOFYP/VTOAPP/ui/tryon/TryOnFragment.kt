package com.TOTOMOFYP.VTOAPP.ui.tryon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.auth.AuthViewModel
import com.TOTOMOFYP.VTOAPP.ui.base.ComposeScreen
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPinkLight
import com.TOTOMOFYP.VTOAPP.ui.theme.DarkBackground
import com.TOTOMOFYP.VTOAPP.ui.theme.LightBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TryOnFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var tryOnViewModel: TryOnViewModel
    
    private var tempPhotoUri: Uri? = null
    
    private var isRequestingForMakeup = false
    private var isRequestingForLiveCamera = false
    private var isSelectingPhotoForMakeup = false
    private var showMakeupImageOptions = mutableStateOf(false)
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isRequestingForMakeup) {
                startMakeupARActivity()
                isRequestingForMakeup = false
            } else if (isRequestingForLiveCamera) {
                val intent = android.content.Intent(requireContext(), com.TOTOMOFYP.VTOAPP.BanubaActivityCamera::class.java)
                startActivity(intent)
                isRequestingForLiveCamera = false
            } else {
                launchCamera()
            }
        } else {
            val message = if (isRequestingForMakeup) {
                "Camera permission is required for virtual makeup"
            } else if (isRequestingForLiveCamera) {
                "Camera permission is required for live camera makeup"
            } else {
                "Camera permission is required for virtual try-on"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            isRequestingForMakeup = false
            isRequestingForLiveCamera = false
        }
    }
    
    private val requestGalleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if we have the necessary permission(s)
        val hasReadPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        if (hasReadPermission) {
            launchGallery()
        } else {
            Toast.makeText(
                requireContext(),
                "Storage permission is required to access photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            // Navigate directly to virtual try-on screen
            navigateToVirtualTryOn(tempPhotoUri!!)
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (isSelectingPhotoForMakeup) {
                // Navigate to makeup activity with selected photo
                navigateToMakeupWithPhoto(uri)
                isSelectingPhotoForMakeup = false
            } else {
                // Navigate directly to virtual try-on screen
                navigateToVirtualTryOn(uri)
            }
        }
    }
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the TryOnViewModel with AuthViewModel dependency (activity scope for sharing)
        tryOnViewModel = ViewModelProvider(
            requireActivity(),
            TryOnViewModel.Factory(authViewModel)
        )[TryOnViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VTOAppTheme {
                    ComposeScreen {
                        TryOnOptionsScreen(
                            onCameraClick = { checkCameraPermission() },
                            onGalleryClick = { checkGalleryPermission() },
                            onMakeupARClick = { launchMakeupAR() },
                            onGalleryMakeupClick = { showMakeupImageOptions.value = true },
                            onLiveCameraMakeupClick = { startLiveCameraMakeup() },
                            showMakeupImageOptions = showMakeupImageOptions.value,
                            onDismissMakeupOptions = { showMakeupImageOptions.value = false },
                            onSelectGalleryForMakeup = { 
                                showMakeupImageOptions.value = false
                                checkGalleryPermissionForMakeup() 
                            },
                            onSelectOutfitStorageForMakeup = { 
                                showMakeupImageOptions.value = false
                                navigateToOutfitStorageForMakeup()
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required for virtual try-on",
                    Toast.LENGTH_LONG
                ).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun checkGalleryPermission() {
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES
        // For older versions, we use READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is required to access photos",
                    Toast.LENGTH_LONG
                ).show()
                
                // Request the appropriate permission(s)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            else -> {
                // Request the appropriate permission(s)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
    }
    
    private fun checkGalleryPermissionForMakeup() {
        isSelectingPhotoForMakeup = true
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES
        // For older versions, we use READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is required to access photos for makeup",
                    Toast.LENGTH_LONG
                ).show()
                
                // Request the appropriate permission(s)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            else -> {
                // Request the appropriate permission(s)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
    }
    
    private fun launchCamera() {
        // Create a temporary file to store the photo
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(requireContext().cacheDir, "PHOTO_${timeStamp}.jpg")
        
        tempPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        
        takePictureLauncher.launch(tempPhotoUri)
    }
    
    private fun launchGallery() {
        pickImageLauncher.launch("image/*")
    }
    
    
    private fun navigateToVirtualTryOn(photoUri: Uri) {
        try {
            // Set the photo in the ViewModel first
            tryOnViewModel.setUserPhoto(photoUri)
            
            // Add a small delay to ensure the ViewModel state is properly set
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    // Check if fragment is still attached before navigating
                    if (isAdded && !isDetached) {
                        // Navigate directly to virtual try-on screen
                        findNavController().navigate(R.id.action_navigation_tryon_to_virtualTryOnFragment)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TryOnFragment", "Navigation error", e)
                    Toast.makeText(requireContext(), "Navigation failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TryOnFragment", "Error setting photo", e)
            Toast.makeText(requireContext(), "Failed to process photo. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToMakeupWithPhoto(photoUri: Uri) {
        try {
            // Launch BanubaActivity with the selected photo for makeup
            val intent = Intent(requireContext(), com.TOTOMOFYP.VTOAPP.BanubaActivity::class.java)
            intent.putExtra("photo_uri", photoUri.toString())
            intent.putExtra("source_mode", "photo")
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("TryOnFragment", "Error launching makeup with photo", e)
            Toast.makeText(requireContext(), "Failed to launch makeup. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToOutfitStorageForMakeup() {
        try {
            // Navigate to outfit storage selection for makeup
            // We'll use the existing navigation to OutfitsFragment but with a special flag
            val bundle = Bundle().apply {
                putBoolean("select_for_makeup", true)
            }
            findNavController().navigate(R.id.navigation_outfits, bundle)
        } catch (e: Exception) {
            android.util.Log.e("TryOnFragment", "Error navigating to outfit storage", e)
            Toast.makeText(requireContext(), "Failed to open outfit storage. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun launchMakeupAR() {
        // Check camera permission first
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted, launch makeup AR
                startMakeupARActivity()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required for virtual makeup",
                    Toast.LENGTH_LONG
                ).show()
                isRequestingForMakeup = true
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                isRequestingForMakeup = true
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startMakeupARActivity() {
        val intent = android.content.Intent(requireContext(), com.TOTOMOFYP.VTOAPP.BanubaActivity::class.java)
        startActivity(intent)
    }
    
    private fun startLiveCameraMakeup() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            isRequestingForLiveCamera = true
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            val intent = android.content.Intent(requireContext(), com.TOTOMOFYP.VTOAPP.BanubaActivityCamera::class.java)
            startActivity(intent)
        }
    }
    
}

@androidx.compose.runtime.Composable
fun TryOnOptionsScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onMakeupARClick: () -> Unit = {},
    onGalleryMakeupClick: () -> Unit = {},
    onLiveCameraMakeupClick: () -> Unit = {},
    showMakeupImageOptions: Boolean = false,
    onDismissMakeupOptions: () -> Unit = {},
    onSelectGalleryForMakeup: () -> Unit = {},
    onSelectOutfitStorageForMakeup: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LightBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp) // add padding
                .verticalScroll(rememberScrollState()), // scrollable
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // Top Section: Clothes Try-On
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ClothesToryOnSection(
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Bottom Section: Makeup Try-On
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                MakeupTryOnSection(
                    onMakeupARClick = {},
                    onGalleryMakeupClick = onGalleryMakeupClick,
                    onLiveCameraMakeupClick = onLiveCameraMakeupClick
                )
            }
        }
        
        // Makeup Image Options Dialog
        if (showMakeupImageOptions) {
            MakeupImageOptionsDialog(
                onDismiss = onDismissMakeupOptions,
                onSelectGallery = onSelectGalleryForMakeup,
                onSelectOutfitStorage = onSelectOutfitStorageForMakeup
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun ClothesToryOnSection(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Clothes Try-On",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Try on clothes with a photo",
                textAlign = TextAlign.Center,
                color = Color.DarkGray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Camera option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp)
                        .clickable(onClick = onCameraClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftBlushPink.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Camera",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Gallery option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp)
                        .clickable(onClick = onGalleryClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftBlushPink.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Gallery",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun MakeupTryOnSection(
    onMakeupARClick: () -> Unit,
    onGalleryMakeupClick: () -> Unit = {},
    onLiveCameraMakeupClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Makeup Try-On",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Apply virtual makeup",
                textAlign = TextAlign.Center,
                color = Color.DarkGray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Camera option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp)
                        .clickable(onClick = onLiveCameraMakeupClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftBlushPink.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Camera",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Image option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp)
                        .clickable(onClick = onGalleryMakeupClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftBlushPink.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Image",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Image",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun MakeupImageOptionsDialog(
    onDismiss: () -> Unit,
    onSelectGallery: () -> Unit,
    onSelectOutfitStorage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Image Source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose where to select your image for makeup application:",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
                
                // Gallery option
                Button(
                    onClick = onSelectGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftBlushPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White
                        )
                        Text(
                            text = "From Gallery",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Outfit Storage option
                Button(
                    onClick = onSelectOutfitStorage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftBlushPinkLight,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Outfit Storage",
                            tint = Color.Black
                        )
                        Text(
                            text = "From Outfit Storage",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Cancel button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
} 