package com.TOTOMOFYP.VTOAPP

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.banuba.sdk.input.CameraDevice
import com.banuba.sdk.input.CameraDeviceConfigurator
import com.banuba.sdk.input.CameraInput
import com.banuba.sdk.input.PhotoInput
import com.banuba.sdk.output.SurfaceOutput
import com.banuba.sdk.output.FrameOutput
import com.banuba.sdk.output.IOutput
import com.banuba.sdk.player.Player
import com.banuba.sdk.player.PlayerTouchListener
import com.banuba.sdk.frame.FramePixelBuffer
import com.banuba.sdk.frame.FramePixelBufferFormat
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import com.TOTOMOFYP.VTOAPP.ui.makeup.MakeupStorage
import org.json.JSONObject
import coil.ImageLoader
import coil.request.ImageRequest

/**
 * Real-time face AR activity using Banuba SDK for virtual try-on
 */
class BanubaActivity : ComponentActivity() {

    private companion object {
        private const val TAG = "BanubaActivity"

        // Required permissions for Banuba SDK
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        const val REQUEST_CODE_PERMISSIONS = 1000
    }

    private lateinit var surfaceView: SurfaceView

    // Banuba SDK components
    private val player by lazy(LazyThreadSafetyMode.NONE) {
        Player()
    }

    private val cameraDevice by lazy(LazyThreadSafetyMode.NONE) {
        CameraDevice(requireNotNull(applicationContext), this@BanubaActivity)
    }

    private lateinit var surfaceOutput: SurfaceOutput
    private var frameOutput: FrameOutput? = null
    
    // Capture state for makeup save
    private var isCapturingForSave = mutableStateOf(false)
    private var saveCallback: ((Bitmap?) -> Unit)? = null
    
    // PhotoInput for offline photo processing
    private val photoInput by lazy(LazyThreadSafetyMode.NONE) {
        PhotoInput()
    }
    
    // Gallery image picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadPhotoFromGallery(it) }
    }

    internal var activeEffects = mutableStateOf(setOf<String>())
    private var isFrontCamera = mutableStateOf(true)
    private var isPhotoMode = mutableStateOf(false)
    private var selectedPhoto = mutableStateOf<Bitmap?>(null)
    private var pendingPhotoUri: Uri? = null
    
    // Makeup save functionality
    private val makeupStorage = MakeupStorage()
    private var showSaveDialog = mutableStateOf(false)
    private var saveDialogName = mutableStateOf("")
    private var isSaving = mutableStateOf(false)
    
    // Effect parameters - saved values (permanent) - all set to mid level (0.5f = level 2)
    private var softlightStrength = mutableStateOf(0.5f)
    private var teethWhiteningStrength = mutableStateOf(0.5f)
    internal var eyesWhiteningStrength = mutableStateOf(0.5f)
    private var foundationStrength = mutableStateOf(0.5f)
    private var foundationSkinTone = mutableStateOf(3) // Default to medium skin tone (index 3)
    internal var eyebagsStrength = mutableStateOf(0.5f)
    internal var eyeshadowStyle = mutableStateOf(0) // Default to Natural Brown
    internal var eyeshadowIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyelashStyle = mutableStateOf(0) // Default to Volume Lashes
    internal var eyelashIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyelinerStyle = mutableStateOf(0) // Default to Black
    internal var eyelinerIntensity = mutableStateOf(0.5f) // Default to mid intensity
    
    // Preview parameters - temporary values while adjusting (only applied when tick is pressed) - all set to mid level (0.5f = level 2)
    internal var softlightPreview = mutableStateOf(0.5f)
    internal var teethWhiteningPreview = mutableStateOf(0.5f)
    internal var eyesWhiteningPreview = mutableStateOf(0.5f)
    internal var foundationPreview = mutableStateOf(0.5f)
    internal var foundationSkinTonePreview = mutableStateOf(3) // Default to medium skin tone
    internal var eyebagsPreview = mutableStateOf(0.5f)
    internal var eyeshadowStylePreview = mutableStateOf(0) // Default to Natural Brown
    internal var eyeshadowIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyelashStylePreview = mutableStateOf(0) // Default to Volume Lashes
    internal var eyelashIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyelinerStylePreview = mutableStateOf(0) // Default to Black
    internal var eyelinerIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyebrowStyle = mutableStateOf(0) // Default to Natural Soft Brown
    internal var eyebrowIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var eyebrowStylePreview = mutableStateOf(0) // Default to Natural Soft Brown  
    internal var eyebrowIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    
    // Session memory - stores temporary settings when switching between effects
    private var sessionMemory = mutableMapOf<String, MutableMap<String, Any>>()
    
    // Session memory functions
    private fun saveToSessionMemory(effectName: String) {
        val effectData = mutableMapOf<String, Any>()
        
        when (effectName) {
            "TeethWhitening" -> {
                effectData["intensity"] = teethWhiteningCategoryIntensityPreview.value
            }
            "SoftLight" -> {
                effectData["intensity"] = softLightCategoryIntensityPreview.value
            }
            "Foundation" -> {
                effectData["intensity"] = foundationCategoryIntensityPreview.value
                selectedFoundationSkinTone.value?.let { tone ->
                    effectData["skinTone"] = tone
                }
            }
            "EyesWhitening" -> {
                effectData["intensity"] = eyesWhiteningPreview.value
            }
            "Eyebags" -> {
                effectData["intensity"] = eyebagsPreview.value
            }
            "Eyeshadow" -> {
                effectData["style"] = eyeshadowStylePreview.value
                effectData["intensity"] = eyeshadowIntensityPreview.value
            }
            "Eyelashes" -> {
                effectData["style"] = eyelashStylePreview.value
                effectData["intensity"] = eyelashIntensityPreview.value
            }
            "Eyeliner" -> {
                effectData["style"] = eyelinerStylePreview.value
                effectData["intensity"] = eyelinerIntensityPreview.value
            }
            "Eyebrows" -> {
                effectData["style"] = eyebrowStylePreview.value
                effectData["intensity"] = eyebrowIntensityPreview.value
            }
            else -> {
                // Handle lip shades
                if (effectName.startsWith("Lips_") || selectedLipShade.value != null) {
                    effectData["intensity"] = lipShadeIntensityPreview.value
                    selectedLipShade.value?.let { shade ->
                        effectData["shade"] = shade
                    }
                }
            }
        }
        
        if (effectData.isNotEmpty()) {
            sessionMemory[effectName] = effectData
            Log.d(TAG, "Saved $effectName to session memory: $effectData")
        }
    }
    
    private fun loadFromSessionMemory(effectName: String) {
        sessionMemory[effectName]?.let { effectData ->
            Log.d(TAG, "Loading $effectName from session memory: $effectData")
            
            when (effectName) {
                "TeethWhitening" -> {
                    effectData["intensity"]?.let { 
                        teethWhiteningCategoryIntensityPreview.value = it as Float
                    }
                }
                "SoftLight" -> {
                    effectData["intensity"]?.let { 
                        softLightCategoryIntensityPreview.value = it as Float
                    }
                }
                "Foundation" -> {
                    effectData["intensity"]?.let { 
                        foundationCategoryIntensityPreview.value = it as Float
                    }
                    effectData["skinTone"]?.let { 
                        selectedFoundationSkinTone.value = it as Int
                    }
                }
                "EyesWhitening" -> {
                    effectData["intensity"]?.let { 
                        eyesWhiteningPreview.value = it as Float
                    }
                }
                "Eyebags" -> {
                    effectData["intensity"]?.let { 
                        eyebagsPreview.value = it as Float
                    }
                }
                "Eyeshadow" -> {
                    effectData["style"]?.let { 
                        eyeshadowStylePreview.value = it as Int
                    }
                    effectData["intensity"]?.let { 
                        eyeshadowIntensityPreview.value = it as Float
                    }
                }
                "Eyelashes" -> {
                    effectData["style"]?.let { 
                        eyelashStylePreview.value = it as Int
                    }
                    effectData["intensity"]?.let { 
                        eyelashIntensityPreview.value = it as Float
                    }
                }
                "Eyeliner" -> {
                    effectData["style"]?.let { 
                        eyelinerStylePreview.value = it as Int
                    }
                    effectData["intensity"]?.let { 
                        eyelinerIntensityPreview.value = it as Float
                    }
                }
                "Eyebrows" -> {
                    effectData["style"]?.let { 
                        eyebrowStylePreview.value = it as Int
                    }
                    effectData["intensity"]?.let { 
                        eyebrowIntensityPreview.value = it as Float
                    }
                }
                else -> {
                    // Handle lip shades
                    if (effectName.startsWith("Lips_") || effectData.containsKey("shade")) {
                        effectData["intensity"]?.let { 
                            lipShadeIntensityPreview.value = it as Float
                        }
                        effectData["shade"]?.let { 
                            selectedLipShade.value = it as String
                        }
                    }
                }
            }
        }
    }
    
    private fun removeFromSessionMemory(effectName: String) {
        sessionMemory.remove(effectName)
        Log.d(TAG, "Removed $effectName from session memory")
    }
    
    // Cancel function - removes effect from session memory and active effects
    private fun cancelEffectChanges() {
        val currentEffect = when {
            selectedMainEffect.value == "Eyes" -> selectedEyeEffect.value ?: "Eyes"
            selectedMainEffect.value != null -> selectedMainEffect.value
            else -> null
        }
        
        currentEffect?.let { effect ->
            // Remove from session memory
            removeFromSessionMemory(effect)
            
            // Remove from active effects
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove(effect)
            
            // For lips, remove all lip shade effects
            if (effect.startsWith("Lips") || effect == "Lips") {
                currentActive.removeAll { it.startsWith("ClassicRedGlam") || it.startsWith("WineLuxury") || 
                                        it.startsWith("SoftNudeGlow") || it.startsWith("MauveElegance") ||
                                        it.startsWith("PeachyCoralPop") || it.startsWith("BerryShine") ||
                                        it.startsWith("PlumDrama") || it.startsWith("BabyPinkGlossy") }
            }
            
            activeEffects.value = currentActive.toSet()
            
            // Close all category panels
            closeAllCategories()
            
            // Reload effects without the cancelled effect
            loadCombinedEffects(activeEffects.value)
            
            Log.d(TAG, "Cancelled changes for $effect")
        }
        
        // Close the current panel
        if (selectedMainEffect.value != null) {
            selectedMainEffect.value = null
        }
    }
    
    private fun closeAllCategories() {
        isTeethWhiteningCategoryExpanded.value = false
        isSoftLightCategoryExpanded.value = false
        isFoundationCategoryExpanded.value = false
        isLipsCategoryExpanded.value = false
        selectedEyeEffect.value = null
        selectedLipShade.value = null
        selectedFoundationSkinTone.value = null
    }
    
    // Helper function to save currently open category to session before switching (temporary storage only)
    private fun saveCurrentCategoryToSession() {
        when {
            isFoundationCategoryExpanded.value -> saveToSessionMemory("Foundation")
            isTeethWhiteningCategoryExpanded.value -> saveToSessionMemory("TeethWhitening")
            isSoftLightCategoryExpanded.value -> saveToSessionMemory("SoftLight")
            isLipsCategoryExpanded.value -> {
                selectedLipShade.value?.let { saveToSessionMemory(it) }
            }
            selectedMainEffect.value == "Eyes" -> {
                selectedEyeEffect.value?.let { saveToSessionMemory(it) }
            }
        }
    }
    
    // Lips category state
    private var isLipsCategoryExpanded = mutableStateOf(false)
    private var selectedLipShade = mutableStateOf<String?>(null)
    internal var lipShadeIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var lipShadeIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    
    // Foundation category state
    private var isFoundationCategoryExpanded = mutableStateOf(false)
    private var selectedFoundationSkinTone = mutableStateOf<Int?>(null)
    internal var foundationCategoryIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var foundationCategoryIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    
    // Teeth Whitening category state
    private var isTeethWhiteningCategoryExpanded = mutableStateOf(false)
    internal var teethWhiteningCategoryIntensity = mutableStateOf(0.5f) // Default to mid intensity  
    internal var teethWhiteningCategoryIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    
    // Soft Light category state
    private var isSoftLightCategoryExpanded = mutableStateOf(false)
    internal var softLightCategoryIntensity = mutableStateOf(0.5f) // Default to mid intensity
    internal var softLightCategoryIntensityPreview = mutableStateOf(0.5f) // Default to mid intensity
    
    // Note: currentSliderEffect removed - all effects now use category structure
    
    // Track original activeEffects state before opening slider (for proper cancel)
    internal var originalActiveEffects = setOf<String>()
    
    // Main effect selection state (unified bottom panel like Eyes category)
    private var selectedMainEffect = mutableStateOf<String?>(null)
    private var isMainEffectPanelExpanded = mutableStateOf(false)
    
    // Eye effect state (now part of unified structure)
    internal var selectedEyeEffect = mutableStateOf<String?>(null)
    
    // Skin tone options for foundation (RGB values)
    private val skinToneColors = listOf(
        "0.98 0.88 0.80", // Level 1 - Very Fair
        "0.95 0.82 0.72", // Level 2 - Fair
        "0.90 0.75 0.65", // Level 3 - Light
        "0.82 0.68 0.55", // Level 4 - Medium
        "0.72 0.55 0.42", // Level 5 - Tan
        "0.55 0.40 0.30", // Level 6 - Deep
        "0.38 0.25 0.18"  // Level 7 - Very Deep
    )
    
    private fun getSkinToneColor(index: Int): String {
        return skinToneColors.getOrElse(index) { skinToneColors[2] } // Default to medium
    }
    
    private fun getEyelashConfig(styleIndex: Int): Map<String, Any> {
        return when (styleIndex) {
            0 -> mapOf( // Volume Lashes
                "color" to "0 0 0",
                "finish" to "volume",
                "coverage" to "high"
            )
            1 -> mapOf( // Lengthening Lashes
                "color" to "0 0 0",
                "finish" to "lengthening",
                "coverage" to "high"
            )
            2 -> mapOf( // Length and Volume
                "color" to "0 0 0",
                "finish" to "lengthandvolume",
                "coverage" to "high"
            )
            3 -> mapOf( // Natural Upper Lashes
                "color" to "0 0 0",
                "finish" to "natural",
                "coverage" to "mid"
            )
            4 -> mapOf( // Natural Bottom Lashes
                "color" to "0 0 0",
                "finish" to "natural_bottom",
                "coverage" to "mid"
            )
            else -> mapOf( // Default to Volume Lashes
                "color" to "0 0 0",
                "finish" to "volume",
                "coverage" to "high"
            )
        }
    }
    
    private fun getEyelashConfigWithIntensity(styleIndex: Int, intensity: Float): Map<String, Any> {
        val baseConfig = getEyelashConfig(styleIndex).toMutableMap()
        
        // Use direct coverage values: 0, 0.25, 0.50, 0.75, 1.0
        baseConfig["coverage"] = intensity
        return baseConfig
    }
    
    private fun getEyelinerConfig(styleIndex: Int): Map<String, Any> {
        return when (styleIndex) {
            0 -> mapOf( // Black
                "name" to "Black",
                "color" to "0 0 0",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            1 -> mapOf( // Brown
                "name" to "Brown",
                "color" to "0.36 0.25 0.20",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            2 -> mapOf( // Dark Grey
                "name" to "Dark Grey",
                "color" to "0.25 0.25 0.25",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            3 -> mapOf( // Navy Blue
                "name" to "Navy Blue",
                "color" to "0.0 0.0 0.5",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            4 -> mapOf( // Emerald Green
                "name" to "Emerald Green",
                "color" to "0.0 0.4 0.25",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            5 -> mapOf( // Burgundy Red
                "name" to "Burgundy Red",
                "color" to "0.5 0.0 0.2",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            6 -> mapOf( // Purple Plum
                "name" to "Purple Plum",
                "color" to "0.4 0.2 0.6",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
            else -> mapOf( // Default to Black
                "name" to "Black",
                "color" to "0 0 0",
                "finish" to "matte_liquid",
                "coverage" to "high"
            )
        }
    }
    
    private fun getEyelinerConfigWithIntensity(styleIndex: Int, intensity: Float): Map<String, Any> {
        val baseConfig = getEyelinerConfig(styleIndex).toMutableMap()
        
        // Use direct coverage values: 0, 0.25, 0.50, 0.75, 1.0
        baseConfig["coverage"] = intensity
        return baseConfig
    }
    
    private fun getEyebrowConfig(styleIndex: Int): Map<String, Any> {
        return when (styleIndex) {
            0 -> mapOf( // Natural Soft Brown
                "color" to "0.39 0.26 0.20",
                "finish" to "matte",
                "coverage" to "mid"
            )
            1 -> mapOf( // Defined Dark Brown
                "color" to "0.25 0.15 0.10", 
                "finish" to "matte",
                "coverage" to "high"
            )
            2 -> mapOf( // Feathered Wet Look
                "color" to "0.28 0.18 0.15",
                "finish" to "wet",
                "coverage" to "mid"
            )
            3 -> mapOf( // Soft Blonde
                "color" to "0.75 0.65 0.50",
                "finish" to "matte", 
                "coverage" to "low"
            )
            4 -> mapOf( // Clear Gel Look
                "color" to "0.0 0.0 0.0",
                "finish" to "clear",
                "coverage" to "low"
            )
            else -> mapOf(
                "color" to "0.39 0.26 0.20",
                "finish" to "matte",
                "coverage" to "mid"
            )
        }
    }
    
    private fun getEyebrowConfigWithIntensity(styleIndex: Int, intensity: Float): Map<String, Any> {
        val baseConfig = getEyebrowConfig(styleIndex).toMutableMap()
        
        // Use direct coverage values: 0, 0.25, 0.50, 0.75, 1.0
        baseConfig["coverage"] = intensity
        return baseConfig
    }
    
    private fun getLipShadeConfig(shadeIndex: Int): Map<String, Any> {
        return when (shadeIndex) {
            0 -> mapOf( // Classic Red Glam
                "makeup_lipstick" to mapOf(
                    "color" to "0.80 0.05 0.10",
                    "finish" to "matte_liquid",
                    "coverage" to "high"
                ),
                "makeup_lipsliner" to mapOf(
                    "color" to "0.75 0.05 0.08",
                    "finish" to "shimmer", 
                    "coverage" to "high",
                    "liner" to "3 5"
                )
            )
            1 -> mapOf( // Wine Luxury
                "makeup_lipstick" to mapOf(
                    "color" to "0.40 0.05 0.15",
                    "finish" to "matte_velvet",
                    "coverage" to "high"
                ),
                "makeup_lipsshine" to mapOf(
                    "color" to "0.45 0.05 0.20",
                    "finish" to "shine",
                    "coverage" to "mid"
                )
            )
            2 -> mapOf( // Soft Nude Glow
                "makeup_lipstick" to mapOf(
                    "color" to "0.75 0.60 0.55",
                    "finish" to "cream",
                    "coverage" to "mid"
                ),
                "makeup_lipsgloss" to mapOf(
                    "threshold" to 0.95,
                    "contour" to 0.5,
                    "weakness" to 0.5,
                    "multiplier" to 1.5,
                    "alpha" to 0.7
                )
            )
            3 -> mapOf( // Mauve Elegance
                "makeup_lipstick" to mapOf(
                    "color" to "0.65 0.45 0.55",
                    "finish" to "satin",
                    "coverage" to "high"
                ),
                "makeup_lipsliner" to mapOf(
                    "color" to "0.55 0.35 0.45",
                    "finish" to "shimmer",
                    "coverage" to "mid",
                    "liner" to "2 5"
                )
            )
            4 -> mapOf( // Peachy Coral Pop
                "makeup_lipstick" to mapOf(
                    "color" to "0.95 0.45 0.35",
                    "finish" to "cream_shine",
                    "coverage" to "high"
                ),
                "makeup_lipsshine" to mapOf(
                    "color" to "1.0 0.5 0.4",
                    "finish" to "glitter",
                    "coverage" to "mid"
                )
            )
            5 -> mapOf( // Berry Shine
                "makeup_lipstick" to mapOf(
                    "color" to "0.55 0.10 0.30",
                    "finish" to "glossy_cream_shimmer",
                    "coverage" to "high"
                ),
                "makeup_lipsgloss" to mapOf(
                    "threshold" to 0.96,
                    "contour" to 0.45,
                    "weakness" to 0.5,
                    "multiplier" to 1.5,
                    "alpha" to 0.8
                )
            )
            6 -> mapOf( // Plum Drama
                "makeup_lipstick" to mapOf(
                    "color" to "0.35 0.10 0.30",
                    "finish" to "matte_powder",
                    "coverage" to "high"
                ),
                "makeup_lipsliner" to mapOf(
                    "color" to "0.30 0.08 0.28",
                    "finish" to "shimmer",
                    "coverage" to "high",
                    "liner" to "4 5"
                )
            )
            7 -> mapOf( // Baby Pink Glossy
                "makeup_lipstick" to mapOf(
                    "color" to "0.95 0.70 0.80",
                    "finish" to "balm",
                    "coverage" to "mid"
                ),
                "makeup_lipsgloss" to mapOf(
                    "threshold" to 0.94,
                    "contour" to 0.5,
                    "weakness" to 0.4,
                    "multiplier" to 1.6,
                    "alpha" to 0.85
                )
            )
            else -> mapOf( // Default to Classic Red Glam
                "makeup_lipstick" to mapOf(
                    "color" to "0.80 0.05 0.10",
                    "finish" to "matte_liquid",
                    "coverage" to "high"
                )
            )
        }
    }
    
    private fun getLipShadeConfigWithIntensity(shadeIndex: Int, intensity: Float): Map<String, Any> {
        val baseConfig = getLipShadeConfig(shadeIndex).toMutableMap()
        
        // Apply intensity with correct parameters for each component
        baseConfig.forEach { (key, value) ->
            if (value is Map<*, *>) {
                val adjustedMap = (value as Map<String, Any>).toMutableMap()
                
                when (key) {
                    "makeup_lipstick", "makeup_lipsliner", "makeup_lipsshine" -> {
                        // For lipstick, lipsliner, lipsshine: use coverage parameter
                        adjustedMap["coverage"] = intensity
                    }
                    "makeup_lipsgloss" -> {
                        // For lipsgloss: use alpha parameter
                        adjustedMap["alpha"] = intensity
                    }
                }
                
                baseConfig[key] = adjustedMap
            }
        }
        
        return baseConfig
    }
    
    private fun getEyeshadowConfig(styleIndex: Int): List<Map<String, Any>> {
        return when (styleIndex) {
            0 -> listOf( // Natural Brown
                mapOf("color" to "0.45 0.35 0.30", "finish" to "matte", "coverage" to "mid"),
                mapOf("color" to "0.85 0.75 0.65", "finish" to "shimmer", "coverage" to "low")
            )
            1 -> listOf( // Minimalistic Copper
                mapOf("color" to "0.68 0.46 0.34", "finish" to "shimmer", "coverage" to "mid")
            )
            2 -> listOf( // Subtle Blue
                mapOf("color" to "0.55 0.67 0.78", "finish" to "matte", "coverage" to "mid"),
                mapOf("color" to "0.75 0.82 0.90", "finish" to "glitter", "coverage" to "low")
            )
            3 -> listOf( // Glittery Purple
                mapOf("color" to "0.76 0.69 0.88", "finish" to "glitter", "coverage" to "high")
            )
            4 -> listOf( // Pink Eyeshadow
                mapOf("color" to "1.00 0.72 0.72", "finish" to "matte", "coverage" to "mid")

            )

            else -> listOf( // Default to Natural Brown
                mapOf("color" to "0.45 0.35 0.30", "finish" to "matte", "coverage" to "mid"),
                mapOf("color" to "0.85 0.75 0.65", "finish" to "shimmer", "coverage" to "low")
            )
        }
    }
    
    private fun getEyeshadowConfigWithIntensity(styleIndex: Int, intensity: Float): List<Map<String, Any>> {
        val baseConfig = getEyeshadowConfig(styleIndex)
        return baseConfig.map { colorMap ->
            val adjustedMap = colorMap.toMutableMap()
            
            // Use direct coverage values: 0, 0.25, 0.50, 0.75, 1.0
            adjustedMap["coverage"] = intensity
            adjustedMap
        }
    }
    
    // Makeup save functionality
    private fun showSaveMakeupDialog() {
        showSaveDialog.value = true
        saveDialogName.value = "My Look ${System.currentTimeMillis()}" // Default name
    }
    
    private fun saveMakeupLook() {
        if (saveDialogName.value.isBlank()) {
            Toast.makeText(this, "Please enter a name for your look", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSaving.value = true
        
        // Use async capture to avoid blocking main thread
        captureCurrentFrameForSave { bitmap ->
            if (bitmap != null) {
                // Save to Firebase Storage in background thread
                lifecycleScope.launch {
                    try {
                        makeupStorage.saveMakeupLook(saveDialogName.value.trim(), bitmap)
                            .onSuccess {
                                runOnUiThread {
                                    Toast.makeText(this@BanubaActivity, "Makeup look saved!", Toast.LENGTH_SHORT).show()
                                    showSaveDialog.value = false
                                    saveDialogName.value = ""
                                    isSaving.value = false
                                }
                            }
                            .onFailure { exception ->
                                runOnUiThread {
                                    Toast.makeText(this@BanubaActivity, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    isSaving.value = false
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving makeup look", e)
                        runOnUiThread {
                            Toast.makeText(this@BanubaActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            isSaving.value = false
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@BanubaActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    isSaving.value = false
                }
            }
        }
    }
    
    private fun captureCurrentFrameForSave(onCaptureComplete: (Bitmap?) -> Unit) {
        try {
            Log.d(TAG, "Attempting to capture frame - Photo mode: ${isPhotoMode.value}")
            
            if (isPhotoMode.value && selectedPhoto.value != null) {
                // Photo mode: Use PixelCopy to capture from surface
                captureFromSurfaceView(onCaptureComplete)
            } else {
                // Camera mode: Use FrameOutput to capture from camera stream
                captureFromFrameOutput(onCaptureComplete)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate frame capture", e)
            onCaptureComplete(null)
        }
    }
    
    private fun captureFromFrameOutput(onCaptureComplete: (Bitmap?) -> Unit) {
        try {
            Log.d(TAG, "Using FrameOutput capture for camera mode")
            
            if (frameOutput == null) {
                Log.e(TAG, "FrameOutput not available")
                Toast.makeText(this, "Frame capture not available", Toast.LENGTH_SHORT).show()
                onCaptureComplete(null)
                return
            }
            
            if (isCapturingForSave.value) {
                Log.w(TAG, "Capture already in progress")
                onCaptureComplete(null)
                return
            }
            
            // Set capture state and callback for async capture
            isCapturingForSave.value = true
            saveCallback = { bitmap ->
                Log.d(TAG, "FrameOutput capture callback received")
                onCaptureComplete(bitmap)
            }
            
            Log.d(TAG, "FrameOutput capture initiated - waiting for next frame...")
            
            // Set up timeout handler to prevent hanging
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isCapturingForSave.value) {
                    Log.e(TAG, "FrameOutput capture timed out")
                    isCapturingForSave.value = false
                    saveCallback = null
                    onCaptureComplete(null)
                }
            }, 3000) // 3 second timeout
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate FrameOutput capture", e)
            isCapturingForSave.value = false
            saveCallback = null
            onCaptureComplete(null)
        }
    }
    
    private fun captureFromSurfaceView(onCaptureComplete: (Bitmap?) -> Unit) {
        try {
            Log.d(TAG, "Using SurfaceView capture for photo mode")
            
            if (!::surfaceView.isInitialized) {
                Log.e(TAG, "SurfaceView not initialized")
                onCaptureComplete(null)
                return
            }
            
            val width = surfaceView.width
            val height = surfaceView.height
            
            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid surface dimensions: ${width}x${height}")
                onCaptureComplete(null)
                return
            }
            
            // Create bitmap with surface dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Use PixelCopy to capture from SurfaceView (requires API 24+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.view.PixelCopy.request(
                    surfaceView.holder.surface,
                    bitmap,
                    { result ->
                        if (result == android.view.PixelCopy.SUCCESS) {
                            Log.d(TAG, "SurfaceView capture successful: ${width}x${height}")
                            onCaptureComplete(bitmap)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $result")
                            onCaptureComplete(null)
                        }
                    },
                    android.os.Handler(android.os.Looper.getMainLooper())
                )
            } else {
                // Fallback for older Android versions - return the original photo with effects applied
                Log.w(TAG, "PixelCopy not available, using original photo as fallback")
                onCaptureComplete(selectedPhoto.value)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture from SurfaceView", e)
            onCaptureComplete(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle intent extras for photo makeup - store for later use
        intent?.let { intent ->
            val photoUriString = intent.getStringExtra("photo_uri")
            val sourceMode = intent.getStringExtra("source_mode")
            
            if (photoUriString != null && sourceMode == "photo") {
                try {
                    val photoUri = Uri.parse(photoUriString)
                    // Store the URI to load after surface is initialized
                    pendingPhotoUri = photoUri
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing photo URI from intent", e)
                }
            }
        }

        setContent {
            VTOAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BanubaARScreen(
                        onGalleryClick = { openGallery() },
                        onEffectToggle = { effectPath -> toggleEffect(effectPath) },
                        isPhotoMode = isPhotoMode.value,
                        activeEffects = activeEffects.value,
                        onSoftlightLevelChange = { level -> updateSoftlightLevel(level) },
                        onTeethWhiteningLevelChange = { level -> updateTeethWhiteningLevel(level) },
                        onEyesWhiteningLevelChange = { level -> updateEyesWhiteningLevel(level) },
                        onFoundationLevelChange = { level -> updateFoundationLevel(level) },
                        onFoundationSkinToneChange = { skinTone -> updateFoundationSkinTone(skinTone) },
                        onEyebagsLevelChange = { level -> updateEyebagsLevel(level) },
                        onEyeshadowStyleChange = { style -> updateEyeshadowStyle(style) },
                        onEyeshadowIntensityChange = { level -> updateEyeshadowIntensity(level) },
                        onEyelashStyleChange = { style -> updateEyelashStyle(style) },
                        onEyelashIntensityChange = { level -> updateEyelashIntensity(level) },
                        onEyelinerStyleChange = { style -> updateEyelinerStyle(style) },
                        onEyelinerIntensityChange = { level -> updateEyelinerIntensity(level) },
                        onEyebrowStyleChange = { style -> updateEyebrowStyle(style) },
                        onEyebrowIntensityChange = { level -> updateEyebrowIntensity(level) },
                        onLipShadeSelect = { shadeName -> selectLipShade(shadeName) },
                        onLipShadeIntensityChange = { level -> updateLipShadeIntensity(level) },
                        onSaveSlider = { saveSliderChanges() },
                        onCloseSlider = { cancelEffectChanges() },
                        selectedEyeEffect = selectedEyeEffect.value,
                        onSelectEyeEffect = { effectPath -> selectEyeEffect(effectPath) },
                        selectedMainEffect = selectedMainEffect.value,
                        onSelectMainEffect = { effectName -> selectMainEffect(effectName) },
                        isMainEffectPanelExpanded = isMainEffectPanelExpanded.value,
                        onToggleMainEffectPanel = { toggleMainEffectPanel() },
                        selectedLipShade = selectedLipShade.value,
                        selectedFoundationSkinTone = selectedFoundationSkinTone.value,
                        onFoundationSkinToneSelect = { skinToneIndex -> selectFoundationSkinTone(skinToneIndex) },
                        onFoundationCategoryIntensityChange = { level -> updateFoundationCategoryIntensity(level) },
                        onTeethWhiteningCategoryIntensityChange = { level -> updateTeethWhiteningCategoryIntensity(level) },
                        onSoftLightCategoryIntensityChange = { level -> updateSoftLightCategoryIntensity(level) },
                        closeAllCategoryPanels = { closeAllCategoryPanels() },
                        onSaveLookClick = { showSaveMakeupDialog() }
                    )
                    
                    // Save makeup dialog
                    if (showSaveDialog.value) {
                        SaveMakeupDialog(
                            name = saveDialogName.value,
                            onNameChange = { saveDialogName.value = it },
                            onSave = { saveMakeupLook() },
                            onDismiss = { 
                                showSaveDialog.value = false
                                saveDialogName.value = ""
                            },
                            isSaving = isSaving.value
                        )
                    }
                }
            }
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanubaSDK() {
        try {
            // Configure camera for highest available quality according to Banuba SDK docs
            // Following official documentation pattern from SDK guide
            cameraDevice.configurator
                .setLens(CameraDeviceConfigurator.LensSelector.FRONT) // Front camera for makeup
                .setVideoCaptureSize(CameraDeviceConfigurator.FHD_CAPTURE_SIZE) // Use HD for video (1280x720)
                .setImageCaptureSize(CameraDeviceConfigurator.QHD_CAPTURE_SIZE) // Use HD for image capture (1280x720)
                .commit() // Must call commit() to apply settings per SDK docs

            // Start camera device as per SDK documentation
            // Permission must be obtained before calling start() per docs
            cameraDevice.start()

            // Connect input and output to player according to SDK docs structure
            // Use SurfaceOutput for display and FrameOutput for capture when available
            if (frameOutput != null) {
                player.use(CameraInput(cameraDevice), arrayOf(surfaceOutput, frameOutput!!))
            } else {
                player.use(CameraInput(cameraDevice), arrayOf(surfaceOutput))
            }

            // Set touch listener for face interaction as per SDK docs
            surfaceView.setOnTouchListener(PlayerTouchListener(applicationContext, player))

            Log.d(TAG, "Banuba Player API configured successfully with HD resolution (1280x720)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Banuba Player API", e)
        }
    }

    internal fun initializeSurfaceView(surfaceView: SurfaceView) {
        Log.d(TAG, "Initializing SurfaceView for makeup-only mode")
        this.surfaceView = surfaceView
        surfaceOutput = SurfaceOutput(surfaceView.holder)
        
        // Initialize FrameOutput for capture (only when needed)
        try {
            frameOutput = FrameOutput(object : FrameOutput.IFramePixelBufferProvider {
                override fun onFrame(output: IOutput, framePixelBuffer: FramePixelBuffer?) {
                    if (isCapturingForSave.value && framePixelBuffer != null) {
                        Log.d(TAG, "FrameOutput received frame for save")
                        
                        // Convert frame to bitmap
                        val bitmap = convertFrameToBitmap(framePixelBuffer)
                        
                        // Reset capture state
                        isCapturingForSave.value = false
                        
                        // Call save callback on main thread
                        runOnUiThread {
                            saveCallback?.invoke(bitmap)
                            saveCallback = null
                        }
                    }
                }
            })
            
            // Set FrameOutput format to RGBA for easier conversion to Bitmap
            frameOutput?.setFormat(FramePixelBufferFormat.BPC8_RGBA)
            
            Log.d(TAG, "FrameOutput initialized for capture")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FrameOutput", e)
            frameOutput = null
        }
        
        setupBanubaSDK()
        
        // Load pending photo if available
        pendingPhotoUri?.let { uri ->
            loadPhotoFromGallery(uri)
            pendingPhotoUri = null
        }
    }

    private fun convertFrameToBitmap(framePixelBuffer: FramePixelBuffer): Bitmap? {
        return try {
            val width = framePixelBuffer.width
            val height = framePixelBuffer.height
            val format = framePixelBuffer.format
            
            Log.d(TAG, "Converting FramePixelBuffer to Bitmap: ${width}x${height}, format: $format")
            
            when (format) {
                FramePixelBufferFormat.BPC8_RGBA -> {
                    // RGBA format - 4 bytes per pixel
                    val pixelBuffer = framePixelBuffer.buffer
                    val pixelArray = ByteArray(pixelBuffer.remaining())
                    pixelBuffer.get(pixelArray)
                    
                    // Convert RGBA to ARGB for Android Bitmap
                    val argbPixels = IntArray(width * height)
                    for (i in 0 until width * height) {
                        val r = pixelArray[i * 4].toInt() and 0xFF
                        val g = pixelArray[i * 4 + 1].toInt() and 0xFF
                        val b = pixelArray[i * 4 + 2].toInt() and 0xFF
                        val a = pixelArray[i * 4 + 3].toInt() and 0xFF
                        
                        // Convert RGBA to ARGB
                        argbPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)
                    
                    Log.d(TAG, "Successfully converted RGBA FramePixelBuffer to Bitmap")
                    bitmap
                }
                else -> {
                    Log.w(TAG, "Unsupported FramePixelBuffer format for conversion: $format")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting FramePixelBuffer to Bitmap", e)
            null
        }
    }

    private fun capturePhoto() {
        // TODO: Implement photo capture using PhotoInput
        Log.d(TAG, "Photo capture functionality to be implemented")
    }
    
    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun loadPhotoFromGallery(uri: Uri) {
        try {
            val uriString = uri.toString()
            
            // Check if this is a Firebase Storage URL
            if (uriString.startsWith("https://firebasestorage.googleapis.com")) {
                // Load from Firebase Storage URL
                loadPhotoFromFirebaseStorage(uriString)
                return
            }
            
            // Load from local URI (gallery)
            val inputStream = contentResolver.openInputStream(uri)
            
            // Decode with reduced size to prevent memory issues and lag
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Reduce image size by half
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Ensure ARGB_8888 format
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            if (bitmap != null) {
                // Ensure bitmap is in ARGB_8888 format as required by Banuba SDK
                val convertedBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                    Log.d(TAG, "Converting gallery bitmap from ${bitmap.config} to ARGB_8888")
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }
                
                // Store the selected photo
                selectedPhoto.value = convertedBitmap
                isPhotoMode.value = true
                
                // Switch to photo processing mode
                setupPhotoMode(convertedBitmap)
                
                Log.d(TAG, "Photo loaded from gallery for makeup processing (${convertedBitmap.width}x${convertedBitmap.height}) with config: ${convertedBitmap.config}")
            } else {
                Toast.makeText(this, "Failed to decode photo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load photo from gallery", e)
            Toast.makeText(this, "Failed to load photo", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadPhotoFromFirebaseStorage(imageUrl: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading photo from Firebase Storage: $imageUrl")
                
                // Use Coil to load the image from Firebase Storage
                val request = ImageRequest.Builder(this@BanubaActivity)
                    .data(imageUrl)
                    .target { drawable ->
                        val originalBitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (originalBitmap != null) {
                            // Ensure bitmap is in ARGB_8888 format as required by Banuba SDK
                            val bitmap = if (originalBitmap.config != Bitmap.Config.ARGB_8888) {
                                Log.d(TAG, "Converting bitmap from ${originalBitmap.config} to ARGB_8888")
                                originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                originalBitmap
                            }
                            
                            // Store the selected photo
                            selectedPhoto.value = bitmap
                            isPhotoMode.value = true
                            
                            // Switch to photo processing mode
                            setupPhotoMode(bitmap)
                            
                            Log.d(TAG, "Photo loaded from Firebase Storage for makeup processing (${bitmap.width}x${bitmap.height}) with config: ${bitmap.config}")
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@BanubaActivity, "Failed to decode photo from storage", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .error(android.R.drawable.ic_delete)
                    .listener(
                        onError = { _, result ->
                            runOnUiThread {
                                Toast.makeText(this@BanubaActivity, "Failed to load photo from storage", Toast.LENGTH_SHORT).show()
                            }
                            Log.e(TAG, "Failed to load photo from Firebase Storage: ${result.throwable}")
                        }
                    )
                    .build()
                
                ImageLoader(this@BanubaActivity).execute(request)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading photo from Firebase Storage", e)
                runOnUiThread {
                    Toast.makeText(this@BanubaActivity, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupPhotoMode(bitmap: Bitmap) {
        try {
            // Stop camera first
            cameraDevice.stop()
            
            // Wait a moment for camera to fully stop
            Thread.sleep(100)
            
            // Clear any existing effects
            player.loadAsync("")
            
            // Switch to manual rendering mode for PhotoInput
            player.setRenderMode(Player.RenderMode.MANUAL)
            
            // Set up PhotoInput with SurfaceOutput
            player.use(photoInput, surfaceOutput)
            
            // Process the bitmap using PhotoInput
            photoInput.take(bitmap)
            
            // Force initial render to display the photo
            player.render()
            
            // Start continuous rendering loop for photo mode
            startPhotoRenderingLoop()
            
            // Load current effects if any are active
            if (activeEffects.value.isNotEmpty()) {
                // Small delay before applying effects
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    loadCombinedEffects(activeEffects.value)
                }, 200)
            }
            
            Log.d(TAG, "Photo mode setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup photo mode", e)
            Toast.makeText(this, "Failed to apply effects to photo", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val photoRenderHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var photoRenderRunnable: Runnable? = null
    
    private fun startPhotoRenderingLoop() {
        stopPhotoRenderingLoop() // Stop any existing loop
        
        photoRenderRunnable = object : Runnable {
            override fun run() {
                if (isPhotoMode.value && selectedPhoto.value != null) {
                    try {
                        player.render()
                        // Continue rendering at 30 FPS for smooth interaction
                        photoRenderHandler.postDelayed(this, 33)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in photo rendering loop", e)
                    }
                }
            }
        }
        photoRenderRunnable?.let { photoRenderHandler.post(it) }
    }
    
    private fun stopPhotoRenderingLoop() {
        photoRenderRunnable?.let { photoRenderHandler.removeCallbacks(it) }
        photoRenderRunnable = null
    }
    
    private fun backToCamera() {
        try {
            // Stop photo rendering loop
            stopPhotoRenderingLoop()
            
            // Clear photo mode
            isPhotoMode.value = false
            selectedPhoto.value = null
            
            // Switch back to automatic rendering mode
            player.setRenderMode(Player.RenderMode.LOOP)
            
            // Restart camera and setup CameraInput
            setupBanubaSDK()
            
            // Reload current effects for camera mode
            loadCombinedEffects(activeEffects.value)
            
            Log.d(TAG, "Switched back to camera mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch back to camera", e)
            Toast.makeText(this, "Failed to switch to camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun flipCamera() {
        try {
            isFrontCamera.value = !isFrontCamera.value

            // Follow SDK documentation pattern for camera configuration
            val lens = if (isFrontCamera.value) {
                CameraDeviceConfigurator.LensSelector.FRONT
            } else {
                CameraDeviceConfigurator.LensSelector.BACK
            }

            // Stop camera before reconfiguration per SDK docs
            cameraDevice.stop()
            
            // Reconfigure with highest available quality settings following SDK pattern
            cameraDevice.configurator
                .setLens(lens)
                .setVideoCaptureSize(CameraDeviceConfigurator.FHD_CAPTURE_SIZE) // HD video (1280x720)
                .setImageCaptureSize(CameraDeviceConfigurator.QHD_CAPTURE_SIZE) // HD image capture (1280x720)
                .commit() // Must call commit() to apply settings per SDK docs
                
            // Restart camera after configuration per SDK docs
            cameraDevice.start()

            Log.d(TAG, "Camera flipped to: ${if (isFrontCamera.value) "front" else "back"} with HD quality (1280x720)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flip camera", e)
        }
    }

    
    // Main effect selection function (like selectEyeEffect but for main effects)
    private fun selectMainEffect(effectName: String) {
        // Store original activeEffects state when starting a new session (if not already stored)
        if (originalActiveEffects.isEmpty()) {
            originalActiveEffects = activeEffects.value.toSet()
        }
        
        selectedMainEffect.value = effectName
        
        // Special handling for Eyes category
        if (effectName == "Eyes") {
            // Auto-select EyesWhitening as default if no session data
            if (selectedEyeEffect.value == null && sessionMemory.keys.none { it.startsWith("Eyes") }) {
                selectedEyeEffect.value = "EyesWhitening"
                Log.d(TAG, "Auto-selected EyesWhitening for Eyes category")
            }
            
            // Load any saved eye effect from session memory (temporary values only)
            selectedEyeEffect.value?.let { loadFromSessionMemory(it) }
        } else {
            // Load from session memory if available for regular effects (temporary values only)
            loadFromSessionMemory(effectName)
            
            // Add effect to active effects for regular effects
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add(effectName)
            activeEffects.value = currentActive.toSet()
            
            // Initialize preview values with current saved values (not session memory)
            when (effectName) {
                "Foundation" -> {
                    foundationCategoryIntensityPreview.value = foundationCategoryIntensity.value
                    foundationSkinTonePreview.value = foundationSkinTone.value
                }
                "TeethWhitening" -> {
                    teethWhiteningCategoryIntensityPreview.value = teethWhiteningCategoryIntensity.value
                }
                "SoftLight" -> {
                    softLightCategoryIntensityPreview.value = softLightCategoryIntensity.value
                }
                "Lips" -> {
                    lipShadeIntensityPreview.value = lipShadeIntensity.value
                }
            }
        }
        
        // Load effects with preview
        loadCombinedEffectsWithPreview(activeEffects.value)
        
        Log.d(TAG, "Selected main effect: $effectName")
    }
    
    private fun toggleMainEffectPanel() {
        isMainEffectPanelExpanded.value = !isMainEffectPanelExpanded.value
    }
    
    private fun selectEyeEffect(effectPath: String) {
        // Store original activeEffects state when starting a new session (if not already stored)
        if (originalActiveEffects.isEmpty()) {
            originalActiveEffects = activeEffects.value.toSet()
        }
        
        selectedEyeEffect.value = effectPath
        
        // Load from session memory if available (temporary values only)
        loadFromSessionMemory(effectPath)
        
        // Handle the effect like normal toggleEffect but within eyes category
        val adjustableEffects = listOf("EyesWhitening", "Eyebags", "Eyeshadow", "Eyelashes", "Eyeliner", "Eyebrows")
        
        if (adjustableEffects.contains(effectPath)) {
            // Add effect to active if not already active
            val currentActive = activeEffects.value.toMutableSet()
            if (!currentActive.contains(effectPath)) {
                currentActive.add(effectPath)
                activeEffects.value = currentActive.toSet()
            }
            
            // Initialize preview values with current saved values (not session memory)
            when (effectPath) {
                "EyesWhitening" -> eyesWhiteningPreview.value = eyesWhiteningStrength.value
                "Eyebags" -> eyebagsPreview.value = eyebagsStrength.value
                "Eyeshadow" -> {
                    eyeshadowStylePreview.value = eyeshadowStyle.value
                    eyeshadowIntensityPreview.value = eyeshadowIntensity.value
                }
                "Eyelashes" -> {
                    eyelashStylePreview.value = eyelashStyle.value
                    eyelashIntensityPreview.value = eyelashIntensity.value
                }
                "Eyeliner" -> {
                    eyelinerStylePreview.value = eyelinerStyle.value
                    eyelinerIntensityPreview.value = eyelinerIntensity.value
                }
                "Eyebrows" -> {
                    eyebrowStylePreview.value = eyebrowStyle.value
                    eyebrowIntensityPreview.value = eyebrowIntensity.value
                }
            }
            
            Log.d(TAG, "Showing inline eye effect controls for: $effectPath")
        }
        
        // Load combined effects
        loadCombinedEffectsWithPreview(activeEffects.value)
    }

    private fun toggleEffect(effectPath: String) {
        try {
            if (effectPath == "Clear") {
                // Clear all effects and slider
                activeEffects.value = setOf()
                // currentSliderEffect removed - using category structure
                player.loadAsync("")
                Log.d(TAG, "Cleared all makeup effects")
                return
            }

            // Check if this effect has adjustable parameters (all effects now handled by their categories)
            val adjustableEffects = listOf<String>()
            
            if (adjustableEffects.contains(effectPath)) {
                // Save original state before opening slider (for proper cancel)
                originalActiveEffects = activeEffects.value.toSet()
                
                // For adjustable effects, show slider and initialize preview values
                val currentActive = activeEffects.value.toMutableSet()
                if (!currentActive.contains(effectPath)) {
                    currentActive.add(effectPath)
                    activeEffects.value = currentActive.toSet()
                }
                
                // Initialize preview values with current saved values
                when (effectPath) {
                    "SoftLight" -> softlightPreview.value = softlightStrength.value
                    "TeethWhitening" -> teethWhiteningPreview.value = teethWhiteningStrength.value
                    "Foundation" -> {
                        foundationPreview.value = foundationStrength.value
                        foundationSkinTonePreview.value = foundationSkinTone.value
                    }
                }
                
                // currentSliderEffect removed - using category structure
                Log.d(TAG, "Showing slider for effect: $effectPath (preview mode)")
            } else {
                // For non-adjustable effects, toggle normally
                val currentActive = activeEffects.value.toMutableSet()
                if (currentActive.contains(effectPath)) {
                    currentActive.remove(effectPath)
                    Log.d(TAG, "Removed effect: $effectPath")
                } else {
                    currentActive.add(effectPath)
                    Log.d(TAG, "Added effect: $effectPath")
                }
                activeEffects.value = currentActive.toSet()
            }
            
            // Load combined effects
            loadCombinedEffects(activeEffects.value)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling effect $effectPath: ${e.message}", e)
        }
    }

    internal fun loadCombinedEffects(effects: Set<String>) {
        try {
            if (effects.isEmpty()) {
                player.loadAsync("")
                Log.d(TAG, "No effects active, cleared all")
                return
            }

            // Create combined effect configuration
            val combinedConfig = createCombinedEffectConfig(effects)
            
            // Create temporary effect directory structure
            val tempEffectDir = java.io.File(filesDir, "temp_effect")
            tempEffectDir.mkdirs()
            val configFile = java.io.File(tempEffectDir, "config.json")
            configFile.writeText(combinedConfig)
            
            // Load the combined effect
            val effect = player.loadAsync(tempEffectDir.absolutePath)
            if (effect != null) {
                Log.d(TAG, "Successfully loaded combined effects: ${effects.joinToString(", ")}")
                
                // If in photo mode, reprocess the photo with new effects
                if (isPhotoMode.value && selectedPhoto.value != null) {
                    selectedPhoto.value?.let { bitmap ->
                        try {
                            photoInput.take(bitmap)
                            // Don't call render here, let the rendering loop handle it
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reprocess photo with effects", e)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Failed to load combined effects")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading combined effects: ${e.message}", e)
        }
    }

    
    // 🎯 5-Level preview updates (temporary until tick is pressed)
    private fun updateSoftlightLevel(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        softlightPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("SoftLight")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("SoftLight")
            activeEffects.value = currentActive.toSet()
        }
        
        // Load effect with preview value (temporary)
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateTeethWhiteningLevel(level: Int) {
        val newValue = level * 0.25f
        teethWhiteningPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("TeethWhitening")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("TeethWhitening")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyesWhiteningLevel(level: Int) {
        val newValue = level * 0.25f
        eyesWhiteningPreview.value = newValue
        
        // Update active effects state for preview
        val currentActive = activeEffects.value.toMutableSet()
        if (newValue == 0f) {
            currentActive.remove("EyesWhitening")
        } else {
            currentActive.add("EyesWhitening")
        }
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
        Log.d(TAG, "Updated Eyes Whitening level to: $level (value: $newValue)")
    }
    
    private fun updateFoundationLevel(level: Int) {
        val newValue = level * 0.25f
        foundationPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("Foundation")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Foundation")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateFoundationSkinTone(skinToneIndex: Int) {
        foundationSkinTonePreview.value = skinToneIndex
        // Reload effects with new skin tone
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyebagsLevel(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        eyebagsPreview.value = newValue
        
        // Update active effects state for preview
        val currentActive = activeEffects.value.toMutableSet()
        if (newValue == 0f) {
            currentActive.remove("Eyebags")
        } else {
            currentActive.add("Eyebags")
        }
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
        Log.d(TAG, "Updated Eyebags level to: $level (value: $newValue)")
    }
    
    private fun updateEyeshadowStyle(styleIndex: Int) {
        eyeshadowStylePreview.value = styleIndex
        
        // Always add Eyeshadow to active effects when style is selected
        val currentActive = activeEffects.value.toMutableSet()
        currentActive.add("Eyeshadow")
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyeshadowIntensity(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        eyeshadowIntensityPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("Eyeshadow")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Eyeshadow")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyelashStyle(styleIndex: Int) {
        eyelashStylePreview.value = styleIndex
        
        // Always add Eyelashes to active effects when style is selected
        val currentActive = activeEffects.value.toMutableSet()
        currentActive.add("Eyelashes")
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyelashIntensity(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        eyelashIntensityPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("Eyelashes")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Eyelashes")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyelinerStyle(styleIndex: Int) {
        eyelinerStylePreview.value = styleIndex
        
        // Always add Eyeliner to active effects when style is selected
        val currentActive = activeEffects.value.toMutableSet()
        currentActive.add("Eyeliner")
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyelinerIntensity(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        eyelinerIntensityPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("Eyeliner")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Eyeliner")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyebrowStyle(styleIndex: Int) {
        eyebrowStylePreview.value = styleIndex
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateEyebrowIntensity(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        eyebrowIntensityPreview.value = newValue
        
        // Update active effects state for preview
        if (newValue == 0f) {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.remove("Eyebrows")
            activeEffects.value = currentActive.toSet()
        } else {
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Eyebrows")
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun closeAllCategoryPanels() {
        isFoundationCategoryExpanded.value = false
        selectedFoundationSkinTone.value = null
        isLipsCategoryExpanded.value = false
        selectedLipShade.value = null
        isTeethWhiteningCategoryExpanded.value = false
        isSoftLightCategoryExpanded.value = false
    }
    
    // Helper functions to track preview session state
    private fun hasSessionStarted(): Boolean {
        return (foundationCategoryIntensityPreview.value != foundationCategoryIntensity.value ||
                lipShadeIntensityPreview.value != lipShadeIntensity.value ||
                teethWhiteningCategoryIntensityPreview.value != teethWhiteningCategoryIntensity.value ||
                softLightCategoryIntensityPreview.value != softLightCategoryIntensity.value)
    }
    
    private fun hasFoundationBeenAdjusted(): Boolean {
        return foundationCategoryIntensityPreview.value != foundationCategoryIntensity.value ||
               foundationSkinTonePreview.value != foundationSkinTone.value
    }
    
    private fun hasLipsBeenAdjusted(): Boolean {
        return lipShadeIntensityPreview.value != lipShadeIntensity.value
    }
    
    private fun hasTeethWhiteningBeenAdjusted(): Boolean {
        return teethWhiteningCategoryIntensityPreview.value != teethWhiteningCategoryIntensity.value
    }
    
    private fun hasSoftLightBeenAdjusted(): Boolean {
        return softLightCategoryIntensityPreview.value != softLightCategoryIntensity.value
    }
    
    private fun closeAllCategoryPanelsWithoutChangingPreview() {
        // Save current category to session memory before switching (for temporary storage)
        saveCurrentCategoryToSession()
        
        // Close all category panels but KEEP preview changes (for switching between effects)
        if (isFoundationCategoryExpanded.value) {
            isFoundationCategoryExpanded.value = false
            selectedFoundationSkinTone.value = null
            // DO NOT revert preview values - keep them for cumulative preview
        }
        
        if (isLipsCategoryExpanded.value) {
            isLipsCategoryExpanded.value = false
            selectedLipShade.value = null
            // DO NOT revert preview values - keep them for cumulative preview
        }
        
        if (isTeethWhiteningCategoryExpanded.value) {
            isTeethWhiteningCategoryExpanded.value = false
            // DO NOT revert preview values - keep them for cumulative preview
        }
        
        if (isSoftLightCategoryExpanded.value) {
            isSoftLightCategoryExpanded.value = false
            // DO NOT revert preview values - keep them for cumulative preview
        }
        
        // Continue using preview effects (cumulative effect)
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun toggleFoundationCategory() {
        if (isFoundationCategoryExpanded.value) {
            // Just close the panel, keep preview changes and active effects
            isFoundationCategoryExpanded.value = false
            selectedFoundationSkinTone.value = null
        } else {
            // Opening Foundation panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isFoundationCategoryExpanded.value = true
            
            // Load from session memory if available (temporary values only)
            loadFromSessionMemory("Foundation")
            
            // Add Foundation to active effects for preview
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Foundation")
            activeEffects.value = currentActive.toSet()
            
            // Auto-select skin tone 3 (medium) as default if no skin tone selected and no session data
            if (selectedFoundationSkinTone.value == null && !sessionMemory.containsKey("Foundation")) {
                selectedFoundationSkinTone.value = 3
                foundationSkinTonePreview.value = 3
            }
            
            loadCombinedEffectsWithPreview(activeEffects.value)
            Log.d(TAG, "Opened Foundation adjustment panel")
        }
    }
    
    private fun toggleLipsCategory() {
        if (isLipsCategoryExpanded.value) {
            // Just close the panel, keep preview changes and active effects
            isLipsCategoryExpanded.value = false
            selectedLipShade.value = null
        } else {
            // Opening Lips panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isLipsCategoryExpanded.value = true
            
            // Auto-select Classic Red Glam as default if no shade selected and no session data
            if (selectedLipShade.value == null && sessionMemory.isEmpty()) {
                selectedLipShade.value = "ClassicRedGlam"
            }
            
            // Load from session memory if available (temporary values only)
            selectedLipShade.value?.let { loadFromSessionMemory(it) }
            
            // Add lip shade to active effects for preview
            val currentActive = activeEffects.value.toMutableSet()
            selectedLipShade.value?.let { shade ->
                currentActive.add(shade)
            }
            activeEffects.value = currentActive.toSet()
            
            loadCombinedEffectsWithPreview(activeEffects.value)
            Log.d(TAG, "Opened Lips adjustment panel")
        }
    }
    
    private fun toggleTeethWhiteningCategory() {
        if (isTeethWhiteningCategoryExpanded.value) {
            // Just close the panel, keep preview changes
            isTeethWhiteningCategoryExpanded.value = false
        } else {
            // Opening Teeth Whitening panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isTeethWhiteningCategoryExpanded.value = true
            
            // Load from session memory if available (temporary values only)
            loadFromSessionMemory("TeethWhitening")
            
            // Add TeethWhitening to active effects for preview
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("TeethWhitening")
            activeEffects.value = currentActive.toSet()
            
            loadCombinedEffectsWithPreview(activeEffects.value)
            Log.d(TAG, "Opened Teeth Whitening adjustment panel")
        }
    }
    
    private fun toggleSoftLightCategory() {
        if (isSoftLightCategoryExpanded.value) {
            // Just close the panel, keep preview changes
            isSoftLightCategoryExpanded.value = false
        } else {
            // Opening Soft Light panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isSoftLightCategoryExpanded.value = true
            
            // Load from session memory if available (temporary values only)
            loadFromSessionMemory("SoftLight")
            
            // Add SoftLight to active effects for preview
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("SoftLight")
            activeEffects.value = currentActive.toSet()
            
            loadCombinedEffectsWithPreview(activeEffects.value)
            Log.d(TAG, "Opened Soft Light adjustment panel")
        }
    }
    
    private fun selectLipShade(shadeName: String) {
        selectedLipShade.value = shadeName
        
        // Remove all previous lip shades and add the new one
        val currentActive = activeEffects.value.toMutableSet()
        
        // Remove all lip shade effects first
        val lipShadeNames = listOf("ClassicRedGlam", "WineLuxury", "SoftNudeGlow", "MauveElegance", 
                                  "PeachyCoralPop", "BerryShine", "PlumDrama", "BabyPinkGlossy")
        lipShadeNames.forEach { currentActive.remove(it) }
        
        // Add the new selected lip shade
        currentActive.add(shadeName)
        activeEffects.value = currentActive.toSet()
        
        // Initialize preview values with current saved values
        lipShadeIntensityPreview.value = lipShadeIntensity.value
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun selectFoundationSkinTone(skinToneIndex: Int) {
        selectedFoundationSkinTone.value = skinToneIndex
        foundationSkinTonePreview.value = skinToneIndex // Also update preview value
        
        // Add Foundation effect to active effects for preview
        val currentActive = activeEffects.value.toMutableSet()
        currentActive.add("Foundation")
        activeEffects.value = currentActive.toSet()
        
        // Initialize preview values with current saved values  
        foundationPreview.value = foundationStrength.value
        
        // Immediately apply the skin tone change
        loadCombinedEffectsWithPreview(activeEffects.value)
        
        Log.d(TAG, "Selected foundation skin tone: $skinToneIndex with color: ${getSkinToneColor(skinToneIndex)}")
    }
    
    private fun updateFoundationCategoryIntensity(level: Int) {
        val newValue = level / 4.0f
        foundationCategoryIntensityPreview.value = newValue
        
        // Update active effects state for preview
        val currentActive = activeEffects.value.toMutableSet()
        if (newValue == 0f) {
            currentActive.remove("Foundation")
        } else {
            currentActive.add("Foundation")
        }
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateTeethWhiteningCategoryIntensity(level: Int) {
        val newValue = level / 4.0f
        teethWhiteningCategoryIntensityPreview.value = newValue
        
        // Update active effects state for preview
        val currentActive = activeEffects.value.toMutableSet()
        if (newValue == 0f) {
            currentActive.remove("TeethWhitening")
        } else {
            currentActive.add("TeethWhitening")
        }
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateSoftLightCategoryIntensity(level: Int) {
        val newValue = level / 4.0f
        softLightCategoryIntensityPreview.value = newValue
        
        // Update active effects state for preview
        val currentActive = activeEffects.value.toMutableSet()
        if (newValue == 0f) {
            currentActive.remove("SoftLight")
        } else {
            currentActive.add("SoftLight")
        }
        activeEffects.value = currentActive.toSet()
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    private fun updateLipShadeIntensity(level: Int) {
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0)
        lipShadeIntensityPreview.value = newValue
        
        // Update active effects state for preview
        selectedLipShade.value?.let { shadeName ->
            val currentActive = activeEffects.value.toMutableSet()
            if (newValue == 0f) {
                currentActive.remove(shadeName)
            } else {
                currentActive.add(shadeName)
            }
            activeEffects.value = currentActive.toSet()
        }
        
        loadCombinedEffectsWithPreview(activeEffects.value)
    }
    
    // Load combined effects with preview values (temporary)
    private fun loadCombinedEffectsWithPreview(effects: Set<String>) {
        try {
            if (effects.isEmpty()) {
                player.loadAsync("")
                Log.d(TAG, "No preview effects active, cleared all")
                return
            }

            // Create combined effect configuration using PREVIEW values
            val combinedConfig = createCombinedEffectConfigWithPreview(effects)
            
            // Create temporary effect directory structure
            val tempEffectDir = java.io.File(filesDir, "temp_effect")
            tempEffectDir.deleteRecursively() // Clean previous
            tempEffectDir.mkdirs()
            val configFile = java.io.File(tempEffectDir, "config.json")
            configFile.writeText(combinedConfig)
            
            val effect = player.loadAsync(tempEffectDir.absolutePath)
            if (effect != null) {
                Log.d(TAG, "Successfully loaded PREVIEW effects: ${effects.joinToString(", ")}")
                
                // If in photo mode, reprocess with preview effects
                if (isPhotoMode.value && selectedPhoto.value != null) {
                    selectedPhoto.value?.let { bitmap ->
                        try {
                            photoInput.take(bitmap)
                            // Don't call render here, let the rendering loop handle it
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reprocess photo with preview effects", e)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Failed to load preview effects")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preview effects: ${e.message}", e)
        }
    }
    
    // Create config using PREVIEW values instead of saved values
    private fun createCombinedEffectConfigWithPreview(effects: Set<String>): String {
        val combinedFaceConfig = mutableMapOf<String, Any>()
        
        // Add dynamic effects with current PREVIEW parameter values
        effects.forEach { effectPath ->
            when (effectPath) {
                "SoftLight" -> {
                    // Check if SoftLight category is expanded (new structure) or old slider structure
                    val intensityToUse = if (isSoftLightCategoryExpanded.value) {
                        softLightCategoryIntensityPreview.value
                    } else {
                        softlightPreview.value
                    }
                    combinedFaceConfig["softlight"] = mapOf(
                        "strength" to intensityToUse
                    )
                }
                "TeethWhitening" -> {
                    // Check if TeethWhitening category is expanded (new structure) or old slider structure
                    val intensityToUse = if (isTeethWhiteningCategoryExpanded.value) {
                        teethWhiteningCategoryIntensityPreview.value
                    } else {
                        teethWhiteningPreview.value
                    }
                    combinedFaceConfig["teeth_whitening"] = mapOf(
                        "strength" to intensityToUse
                    )
                }
                "EyesWhitening" -> {
                    combinedFaceConfig["eyes_whitening"] = mapOf(
                        "strength" to eyesWhiteningPreview.value
                    )
                }
                "Foundation" -> {
                    // Check if Foundation category is expanded (new structure) or old slider structure
                    val skinToneToUse = if (isFoundationCategoryExpanded.value && selectedFoundationSkinTone.value != null) {
                        selectedFoundationSkinTone.value!!
                    } else {
                        foundationSkinTonePreview.value
                    }
                    val intensityToUse = if (isFoundationCategoryExpanded.value) {
                        foundationCategoryIntensityPreview.value
                    } else {
                        foundationPreview.value
                    }
                    
                    // Apply to both concealer and foundation simultaneously
                    combinedFaceConfig["makeup_foundation"] = mapOf(
                        "color" to getSkinToneColor(skinToneToUse),
                        "finish" to "natural",
                        "coverage" to intensityToUse
                    )
                    combinedFaceConfig["makeup_concealer"] = mapOf(
                        "color" to getSkinToneColor(skinToneToUse),
                        "finish" to "natural",
                        "coverage" to intensityToUse
                    )
                }
                "Eyebags" -> {
                    combinedFaceConfig["makeup_eyebags"] = mapOf(
                        "alpha" to eyebagsPreview.value
                    )
                }
                "Eyeshadow" -> {
                    // Only apply eyeshadow if intensity > 0 to avoid black container
                    if (eyeshadowIntensityPreview.value > 0f) {
                        combinedFaceConfig["makeup_eyeshadow"] = getEyeshadowConfigWithIntensity(eyeshadowStylePreview.value, eyeshadowIntensityPreview.value)
                    }
                }
                "Eyelashes" -> {
                    // Only apply eyelashes if intensity > 0 to avoid black container
                    if (eyelashIntensityPreview.value > 0f) {
                        combinedFaceConfig["makeup_eyelashes"] = getEyelashConfigWithIntensity(eyelashStylePreview.value, eyelashIntensityPreview.value)
                    }
                }
                "Eyeliner" -> {
                    // Only apply eyeliner if intensity > 0 to avoid black container
                    if (eyelinerIntensityPreview.value > 0f) {
                        combinedFaceConfig["makeup_eyeliner"] = getEyelinerConfigWithIntensity(eyelinerStylePreview.value, eyelinerIntensityPreview.value)
                    }
                }
                "Eyebrows" -> {
                    // Only apply eyebrows if intensity > 0
                    if (eyebrowIntensityPreview.value > 0f) {
                        combinedFaceConfig["makeup_eyebrows"] = getEyebrowConfigWithIntensity(eyebrowStylePreview.value, eyebrowIntensityPreview.value)
                    }
                }
                "ClassicRedGlam" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(0, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "WineLuxury" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(1, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "SoftNudeGlow" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(2, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "MauveElegance" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(3, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "PeachyCoralPop" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(4, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "BerryShine" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(5, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "PlumDrama" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(6, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "BabyPinkGlossy" -> {
                    if (lipShadeIntensityPreview.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(7, lipShadeIntensityPreview.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                else -> {
                    // For other effects, read from config file as before
                    try {
                        val configText = assets.open("effects/$effectPath/config.json").bufferedReader().use { it.readText() }
                        val config = org.json.JSONObject(configText)
                        val faces = config.getJSONArray("faces")
                        if (faces.length() > 0) {
                            val faceConfig = faces.getJSONObject(0)
                            faceConfig.keys().forEach { key ->
                                if (key != "makeup_base") {
                                    combinedFaceConfig[key] = jsonToMap(faceConfig.get(key))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading config for $effectPath: ${e.message}")
                    }
                }
            }
        }
        
        val combinedConfig = mapOf(
            "scene" to "Preview Combined Effects",
            "version" to "2.0.0",
            "camera" to emptyMap<String, Any>(),
            "faces" to listOf(combinedFaceConfig)
        )
        
        return org.json.JSONObject(combinedConfig).toString(2)
    }


    // ✅ Save button - Apply preview values to permanent values and close panel
    private fun saveSliderChanges() {
        
        // Handle Eyes category save - save selected eye effect preview values
        if (selectedMainEffect.value == "Eyes" && selectedEyeEffect.value != null) {
            selectedEyeEffect.value?.let { effect ->
                when (effect) {
                    "EyesWhitening" -> eyesWhiteningStrength.value = eyesWhiteningPreview.value
                    "Eyebags" -> eyebagsStrength.value = eyebagsPreview.value
                    "Eyeshadow" -> {
                        eyeshadowStyle.value = eyeshadowStylePreview.value
                        eyeshadowIntensity.value = eyeshadowIntensityPreview.value
                    }
                    "Eyelashes" -> {
                        eyelashStyle.value = eyelashStylePreview.value
                        eyelashIntensity.value = eyelashIntensityPreview.value
                    }
                    "Eyeliner" -> {
                        eyelinerStyle.value = eyelinerStylePreview.value
                        eyelinerIntensity.value = eyelinerIntensityPreview.value
                    }
                    "Eyebrows" -> {
                        eyebrowStyle.value = eyebrowStylePreview.value
                        eyebrowIntensity.value = eyebrowIntensityPreview.value
                    }
                }
            }
        }
        
        // Handle main effects save
        selectedMainEffect.value?.let { effect ->
            when (effect) {
                "TeethWhitening" -> {
                    teethWhiteningStrength.value = teethWhiteningPreview.value
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (teethWhiteningPreview.value > 0f) {
                        currentActive.add("TeethWhitening")
                    } else {
                        currentActive.remove("TeethWhitening")
                    }
                    activeEffects.value = currentActive.toSet()
                    
                    loadCombinedEffects(activeEffects.value)
                    Log.d(TAG, "Saved teeth whitening changes and closed panel")
                }
                "SoftLight" -> {
                    softlightStrength.value = softlightPreview.value
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (softlightPreview.value > 0f) {
                        currentActive.add("SoftLight")
                    } else {
                        currentActive.remove("SoftLight")
                    }
                    activeEffects.value = currentActive.toSet()
                    
                    loadCombinedEffects(activeEffects.value)
                    Log.d(TAG, "Saved soft light changes and closed panel")
                }
                "Foundation" -> {
                    foundationStrength.value = foundationPreview.value
                    foundationSkinTone.value = selectedFoundationSkinTone.value ?: 3
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (foundationPreview.value > 0f) {
                        currentActive.add("Foundation")
                    } else {
                        currentActive.remove("Foundation")
                    }
                    activeEffects.value = currentActive.toSet()
                    
                    loadCombinedEffects(activeEffects.value)
                    Log.d(TAG, "Saved foundation changes and closed panel")
                }
                "Lips" -> {
                    selectedLipShade.value?.let { shade ->
                        lipShadeIntensity.value = lipShadeIntensityPreview.value
                        
                        val currentActive = activeEffects.value.toMutableSet()
                        if (lipShadeIntensityPreview.value > 0f) {
                            currentActive.add(shade)
                        } else {
                            currentActive.remove(shade)
                        }
                        activeEffects.value = currentActive.toSet()
                        
                        loadCombinedEffects(activeEffects.value)
                        Log.d(TAG, "Saved lip shade changes and closed panel")
                    }
                }
            }
            
            // Close the panel
            selectedMainEffect.value = null
        }
        
    }
    
    // ✕ Cancel button - Cancel ALL preview changes from the entire session and revert to original state
    private fun closeSliderWithoutSaving() {
        
        // Revert ALL category preview values to their saved values (entire session cancelled)
        foundationCategoryIntensityPreview.value = foundationCategoryIntensity.value
        foundationSkinTonePreview.value = foundationSkinTone.value
        lipShadeIntensityPreview.value = lipShadeIntensity.value
        teethWhiteningCategoryIntensityPreview.value = teethWhiteningCategoryIntensity.value
        softLightCategoryIntensityPreview.value = softLightCategoryIntensity.value
        
        // Revert all eye effect preview values to saved values
        eyesWhiteningPreview.value = eyesWhiteningStrength.value
        eyebagsPreview.value = eyebagsStrength.value
        eyeshadowStylePreview.value = eyeshadowStyle.value
        eyeshadowIntensityPreview.value = eyeshadowIntensity.value
        eyelashStylePreview.value = eyelashStyle.value
        eyelashIntensityPreview.value = eyelashIntensity.value
        eyelinerStylePreview.value = eyelinerStyle.value
        eyelinerIntensityPreview.value = eyelinerIntensity.value
        eyebrowStylePreview.value = eyebrowStyle.value
        eyebrowIntensityPreview.value = eyebrowIntensity.value
        
        // Clear session memory (all temporary adjustments)
        sessionMemory.clear()
        
        // Restore original activeEffects state (before the session started)
        activeEffects.value = originalActiveEffects
        loadCombinedEffects(activeEffects.value)
        
        // Close all panels and reset selections
        closeAllCategoryPanels()
        selectedEyeEffect.value = null
        selectedMainEffect.value = null
        
        Log.d(TAG, "Cancelled ALL preview changes and reverted to original state")
    }


    private fun createCombinedEffectConfig(effects: Set<String>): String {
        val combinedFaceConfig = mutableMapOf<String, Any>()
        
        // Add dynamic effects with current parameter values
        effects.forEach { effectPath ->
            when (effectPath) {
                "SoftLight" -> {
                    combinedFaceConfig["softlight"] = mapOf(
                        "strength" to softlightStrength.value
                    )
                }
                "TeethWhitening" -> {
                    combinedFaceConfig["teeth_whitening"] = mapOf(
                        "strength" to teethWhiteningStrength.value
                    )
                }
                "EyesWhitening" -> {
                    combinedFaceConfig["eyes_whitening"] = mapOf(
                        "strength" to eyesWhiteningStrength.value
                    )
                }
                "Foundation" -> {
                    // Apply to both concealer and foundation simultaneously  
                    combinedFaceConfig["makeup_foundation"] = mapOf(
                        "color" to getSkinToneColor(foundationSkinTone.value),
                        "finish" to "natural",
                        "coverage" to foundationStrength.value
                    )
                    combinedFaceConfig["makeup_concealer"] = mapOf(
                        "color" to getSkinToneColor(foundationSkinTone.value),
                        "finish" to "natural",
                        "coverage" to foundationStrength.value
                    )
                }
                "Eyebags" -> {
                    combinedFaceConfig["makeup_eyebags"] = mapOf(
                        "alpha" to eyebagsStrength.value
                    )
                }
                "Eyeshadow" -> {
                    // Only apply eyeshadow if intensity > 0 to avoid black container
                    if (eyeshadowIntensity.value > 0f) {
                        combinedFaceConfig["makeup_eyeshadow"] = getEyeshadowConfigWithIntensity(eyeshadowStyle.value, eyeshadowIntensity.value)
                    }
                }
                "Eyelashes" -> {
                    // Only apply eyelashes if intensity > 0 to avoid black container
                    if (eyelashIntensity.value > 0f) {
                        combinedFaceConfig["makeup_eyelashes"] = getEyelashConfigWithIntensity(eyelashStyle.value, eyelashIntensity.value)
                    }
                }
                "Eyeliner" -> {
                    // Only apply eyeliner if intensity > 0 to avoid black container
                    if (eyelinerIntensity.value > 0f) {
                        combinedFaceConfig["makeup_eyeliner"] = getEyelinerConfigWithIntensity(eyelinerStyle.value, eyelinerIntensity.value)
                    }
                }
                "Eyebrows" -> {
                    // Only apply eyebrows if intensity > 0
                    if (eyebrowIntensity.value > 0f) {
                        combinedFaceConfig["makeup_eyebrows"] = getEyebrowConfigWithIntensity(eyebrowStyle.value, eyebrowIntensity.value)
                    }
                }
                "ClassicRedGlam" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(0, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "WineLuxury" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(1, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "SoftNudeGlow" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(2, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "MauveElegance" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(3, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "PeachyCoralPop" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(4, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "BerryShine" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(5, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "PlumDrama" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(6, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                "BabyPinkGlossy" -> {
                    if (lipShadeIntensity.value > 0f) {
                        val lipConfig = getLipShadeConfigWithIntensity(7, lipShadeIntensity.value)
                        combinedFaceConfig.putAll(lipConfig)
                    }
                }
                else -> {
                    // For other effects, read from config file as before
                    try {
                        val configText = assets.open("effects/$effectPath/config.json").bufferedReader().use { it.readText() }
                        val config = org.json.JSONObject(configText)
                        val faces = config.getJSONArray("faces")
                        if (faces.length() > 0) {
                            val faceConfig = faces.getJSONObject(0)
                            faceConfig.keys().forEach { key ->
                                if (key != "makeup_base") { // Don't override base config
                                    combinedFaceConfig[key] = jsonToMap(faceConfig.get(key))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading config for $effectPath: ${e.message}")
                    }
                }
            }
        }
        
        val combinedConfig = mapOf(
            "scene" to "Combined Effects",
            "version" to "2.0.0",
            "camera" to emptyMap<String, Any>(),
            "faces" to listOf(combinedFaceConfig)
        )
        
        return org.json.JSONObject(combinedConfig).toString(2)
    }
    
    private fun jsonToMap(jsonValue: Any): Any {
        return when (jsonValue) {
            is org.json.JSONObject -> {
                val map = mutableMapOf<String, Any>()
                jsonValue.keys().forEach { key ->
                    map[key] = jsonToMap(jsonValue.get(key))
                }
                map
            }
            is org.json.JSONArray -> {
                (0 until jsonValue.length()).map { jsonToMap(jsonValue.get(it)) }
            }
            else -> jsonValue
        }
    }

    override fun onStart() {
        super.onStart()
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        // Camera is now started in setupBanubaSDK() after configuration
        // This function kept for compatibility but camera start moved to proper place per SDK docs
        Log.d(TAG, "Camera start handled in setupBanubaSDK()")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                val notGrantedPermission = permissions.zip(grantResults.toTypedArray())
                    .firstOrNull { (_, result) -> result != PackageManager.PERMISSION_GRANTED }
                    ?.first

                Toast.makeText(
                    this,
                    "Permission $notGrantedPermission is required for AR effects",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            player.play()
            Log.d(TAG, "Player resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume player", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            player.pause()
            Log.d(TAG, "Player paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause player", e)
        }
    }

    override fun onStop() {
        try {
            cameraDevice.stop()
            Log.d(TAG, "Camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera", e)
        }
        super.onStop()
    }

    override fun onDestroy() {
        try {
            // Stop photo rendering loop
            stopPhotoRenderingLoop()
            
            // Release Banuba SDK resources
            cameraDevice.close()
            surfaceOutput.close()
            frameOutput?.close()
            player.close()
            
            // Clear photo reference
            selectedPhoto.value = null
            
            Log.d(TAG, "Banuba SDK resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Banuba SDK resources", e)
        }
        super.onDestroy()
    }
}
@Composable
fun IntensitySelector(
    currentLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    val totalLevels = 4 // levels 0..4 (5 steps)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Low/High labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "None",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = "Max",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
        
        // Intensity selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp), // Further reduced height
            contentAlignment = Alignment.Center
        ) {
            // Background thin line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Reduced width
                    .height(3.dp) // Reduced height
                    .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )

            // Small faded dots
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..totalLevels).forEach { _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp) // Reduced size
                            .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            // Clickable circles (current level)
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..totalLevels).forEach { level ->
                    val isSelected = currentLevel == level
                    Box(
                        modifier = Modifier
                            .size(22.dp) // Fixed size for consistent clickable area
                            .clickable { onLevelChange(level) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp) // Reduced size
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, Color.Black, CircleShape) // Thinner border
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FourLevelIntensitySelector(
    currentLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    val totalLevels = 3 // levels 0..3 (4 steps: none, low, medium, high)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // None/High labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "None",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = "High",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
        
        // Intensity selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background thin line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(3.dp)
                    .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )

            // Small faded dots
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..totalLevels).forEach { _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            // Clickable circles (current level)
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..totalLevels).forEach { level ->
                    val isSelected = currentLevel == level
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onLevelChange(level) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, Color.Black, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EyebrowStyleSelector(
    currentStyle: Int,
    onStyleChange: (Int) -> Unit
) {
    val eyebrowStyles = listOf(
        "Natural Soft Brown",
        "Defined Dark Brown", 
        "Feathered Wet Look",
        "Soft Blonde",
        "Clear Gel Look"
    )

    Column {
        Text(
            text = eyebrowStyles[currentStyle],
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(eyebrowStyles.size) { index ->
                val isSelected = currentStyle == index
                Button(
                    onClick = { onStyleChange(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = eyebrowStyles[index],
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EyeshadowStyleSelector(
    currentStyle: Int,
    onStyleChange: (Int) -> Unit
) {
    val eyeshadowStyles = listOf(
        "Natural Brown",
        "Minimalistic Copper",
        "Subtle Blue",
        "Glittery Purple",
        "Pink Eyeshadow"
    )

    Column {
        Text(
            text = eyeshadowStyles[currentStyle],
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(eyeshadowStyles) { index, styleName ->
                Button(
                    onClick = { onStyleChange(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStyle == index) SoftBlushPink else Color.White.copy(alpha = 0.2f),
                        contentColor = if (currentStyle == index) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = styleName,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EyelashStyleSelector(
    currentStyle: Int,
    onStyleChange: (Int) -> Unit
) {
    val eyelashStyles = listOf(
        "Volume Lashes",
        "Lengthening Lashes",
        "Length and Volume",
        "Natural Upper Lashes",
        "Natural Bottom Lashes"
    )

    Column {
        Text(
            text = eyelashStyles[currentStyle],
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(eyelashStyles) { index, styleName ->
                Button(
                    onClick = { onStyleChange(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStyle == index) SoftBlushPink else Color.White.copy(alpha = 0.2f),
                        contentColor = if (currentStyle == index) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = styleName,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EyelinerStyleSelector(
    currentStyle: Int,
    onStyleChange: (Int) -> Unit
) {
    val eyelinerStyles = listOf(
        "Black",
        "Brown", 
        "Dark Grey",
        "Navy Blue",
        "Emerald Green",
        "Burgundy Red",
        "Purple Plum"
    )

    Column {
        Text(
            text = eyelinerStyles[currentStyle],
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(eyelinerStyles) { index, styleName ->
                Button(
                    onClick = { onStyleChange(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStyle == index) SoftBlushPink else Color.White.copy(alpha = 0.2f),
                        contentColor = if (currentStyle == index) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = styleName,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SkinToneSelector(
    currentSkinTone: Int,
    onSkinToneChange: (Int) -> Unit
) {
    val skinToneColors = listOf(
        Color(0xFFFAE0CC), // Level 1 - Very Fair
        Color(0xFFF2D1B8), // Level 2 - Fair
        Color(0xFFE6BFA6), // Level 3 - Light
        Color(0xFFD1AD8C), // Level 4 - Medium
        Color(0xFFB88C6B), // Level 5 - Tan
        Color(0xFF8C664D), // Level 6 - Deep
        Color(0xFF61402E)  // Level 7 - Very Deep
    )
    
    val skinToneNames = listOf(
        "Very Fair", "Fair", "Light", "Medium", 
        "Tan", "Deep", "Very Deep"
    )

    Column {
        Text(
            text = "Skin Tone: ${skinToneNames[currentSkinTone]}",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            skinToneColors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(24.dp) // Reduced size
                        .background(color, CircleShape)
                        .border(
                            width = if (currentSkinTone == index) 2.dp else 1.dp,
                            color = if (currentSkinTone == index) Color.White else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onSkinToneChange(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanubaARScreen(
    onGalleryClick: () -> Unit,
    onEffectToggle: (String) -> Unit,
    isPhotoMode: Boolean,
    activeEffects: Set<String>,
    onSoftlightLevelChange: (Int) -> Unit,
    onTeethWhiteningLevelChange: (Int) -> Unit,
    onEyesWhiteningLevelChange: (Int) -> Unit,
    onFoundationLevelChange: (Int) -> Unit,
    onFoundationSkinToneChange: (Int) -> Unit,
    onEyebagsLevelChange: (Int) -> Unit,
    onEyeshadowStyleChange: (Int) -> Unit,
    onEyeshadowIntensityChange: (Int) -> Unit,
    onEyelashStyleChange: (Int) -> Unit,
    onEyelashIntensityChange: (Int) -> Unit,
    onEyelinerStyleChange: (Int) -> Unit,
    onEyelinerIntensityChange: (Int) -> Unit,
    onEyebrowStyleChange: (Int) -> Unit,
    onEyebrowIntensityChange: (Int) -> Unit,
    onLipShadeSelect: (String) -> Unit,
    onLipShadeIntensityChange: (Int) -> Unit,
    onSaveSlider: () -> Unit,
    onCloseSlider: () -> Unit,
    selectedEyeEffect: String?,
    onSelectEyeEffect: (String) -> Unit,
    selectedMainEffect: String?,
    onSelectMainEffect: (String) -> Unit,
    isMainEffectPanelExpanded: Boolean,
    onToggleMainEffectPanel: () -> Unit,
    selectedLipShade: String?,
    selectedFoundationSkinTone: Int?,
    onFoundationSkinToneSelect: (Int) -> Unit,
    onFoundationCategoryIntensityChange: (Int) -> Unit,
    onTeethWhiteningCategoryIntensityChange: (Int) -> Unit,
    onSoftLightCategoryIntensityChange: (Int) -> Unit,
    closeAllCategoryPanels: () -> Unit,
    onSaveLookClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Surface View
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    // Initialize Banuba components when surface is created
                    when (context) {
                        is BanubaActivity -> context.initializeSurfaceView(surfaceView)
                        is com.TOTOMOFYP.VTOAPP.BanubaActivityCamera -> context.initializeSurfaceView(surfaceView)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top control bar - only right side controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save button (visible in camera mode)
            IconButton(
                onClick = onSaveLookClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        SoftBlushPink.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Save Look",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Gallery button (only in camera mode)
            if (!isPhotoMode) {
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }
            }
        }

            
            // New unified effects panel - always visible at bottom
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    // Main effects tabs - always visible at bottom
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        val mainEffects = listOf(
                            "Foundation" to "Foundation",
                            "Lips" to "Lips", 
                            "TeethWhitening" to "Teeth White",
                            "SoftLight" to "Soft Light",
                            "Eyes" to "Eyes"
                        )
                        
                        items(mainEffects) { (effectKey, displayName) ->
                            Button(
                                onClick = { 
                                    Log.d("BanubaActivity", "Button clicked: $effectKey")
                                    when (effectKey) {
                                        "Eyes" -> {
                                            Log.d("BanubaActivity", "Eyes button clicked - using unified structure")
                                            onSelectMainEffect(effectKey)
                                        }
                                        else -> {
                                            Log.d("BanubaActivity", "Other effect clicked: $effectKey")
                                            onSelectMainEffect(effectKey)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (effectKey) {
                                        "Eyes" -> {
                                            val eyeEffects = setOf("EyesWhitening", "Eyebags", "Eyeshadow", "Eyelashes", "Eyeliner", "Eyebrows")
                                            val hasActiveEyeEffect = activeEffects.intersect(eyeEffects).isNotEmpty()
                                            if (hasActiveEyeEffect || selectedMainEffect == "Eyes") SoftBlushPink else Color.White.copy(alpha = 0.4f)
                                        }
                                        else -> if (selectedMainEffect == effectKey || activeEffects.contains(effectKey)) SoftBlushPink else Color.White.copy(alpha = 0.4f)
                                    },
                                    contentColor = when (effectKey) {
                                        "Eyes" -> {
                                            val eyeEffects = setOf("EyesWhitening", "Eyebags", "Eyeshadow", "Eyelashes", "Eyeliner", "Eyebrows")
                                            val hasActiveEyeEffect = activeEffects.intersect(eyeEffects).isNotEmpty()
                                            if (hasActiveEyeEffect || selectedMainEffect == "Eyes") Color.White else Color.White.copy(alpha = 0.9f)
                                        }
                                        else -> if (selectedMainEffect == effectKey || activeEffects.contains(effectKey)) Color.White else Color.White.copy(alpha = 0.9f)
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(horizontal = 2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    // BOTTOM LEVEL: Adjustment panels for selected effect
                    if (selectedMainEffect != null) {
                        // Foundation adjustment panel
                        if (selectedMainEffect == "Foundation") {
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            // Skin tone selector
                            Text(
                                text = "Skin Tone",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(7) { skinToneIndex ->
                                    val isSelected = selectedFoundationSkinTone == skinToneIndex
                                    Button(
                                        onClick = { onFoundationSkinToneSelect(skinToneIndex) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                                            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Tone ${skinToneIndex + 1}",
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Intensity selector
                            Text(
                                text = "Coverage Intensity",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val activity = context as? BanubaActivity
                            val currentLevel = ((activity?.foundationPreview?.value ?: 0f) * 4).toInt()
                            
                            IntensitySelector(
                                currentLevel = currentLevel,
                                onLevelChange = onFoundationLevelChange
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCloseSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.7f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✕ Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Button(
                                    onClick = onSaveSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftBlushPink,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✓ Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    
                        if (selectedMainEffect == "Lips") {
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            // Lip shade selector
                            Text(
                                text = "Lip Shade",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val lipShades = listOf(
                                "ClassicRedGlam" to "Classic Red",
                                "WineLuxury" to "Wine Luxury",
                                "SoftNudeGlow" to "Soft Nude",
                                "MauveElegance" to "Mauve",
                                "PeachyCoralPop" to "Peachy Coral",
                                "BerryShine" to "Berry Shine", 
                                "PlumDrama" to "Plum Drama",
                                "BabyPinkGlossy" to "Baby Pink"
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(lipShades) { (shadeKey, displayName) ->
                                    val isSelected = selectedLipShade == shadeKey
                                    Button(
                                        onClick = { onLipShadeSelect(shadeKey) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                                            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = displayName,
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Intensity selector
                            Text(
                                text = "Intensity",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val activity = context as? BanubaActivity
                            val currentLevel = ((activity?.lipShadeIntensityPreview?.value ?: 0f) * 4).toInt()
                            
                            IntensitySelector(
                                currentLevel = currentLevel,
                                onLevelChange = onLipShadeIntensityChange
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCloseSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.7f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✕ Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Button(
                                    onClick = onSaveSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftBlushPink,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✓ Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    
                        if (selectedMainEffect == "TeethWhitening") {
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            // Intensity selector
                            Text(
                                text = "Teeth Whitening Intensity",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val activity = context as? BanubaActivity
                            val currentLevel = ((activity?.teethWhiteningPreview?.value ?: 0f) * 4).toInt()
                            
                            IntensitySelector(
                                currentLevel = currentLevel,
                                onLevelChange = onTeethWhiteningLevelChange
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCloseSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.7f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✕ Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Button(
                                    onClick = onSaveSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftBlushPink,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✓ Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    
                        if (selectedMainEffect == "Eyes") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                // Eye categories selector
                                Text(
                                    text = "Eye Effect",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                val eyeCategories = listOf(
                                    "EyesWhitening" to "Eyes White",
                                    "Eyebags" to "Eyebags", 
                                    "Eyeshadow" to "Eyeshadow",
                                    "Eyelashes" to "Eyelashes",
                                    "Eyeliner" to "Eyeliner",
                                    "Eyebrows" to "Eyebrows"
                                )
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(eyeCategories) { (effectKey, displayName) ->
                                        val isSelected = selectedEyeEffect == effectKey
                                        Button(
                                            onClick = { onSelectEyeEffect(effectKey) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                                                contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .padding(horizontal = 2.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = displayName,
                                                fontSize = 9.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                
                                // Adjustment controls for selected eye effect
                                selectedEyeEffect?.let { selectedEffect ->
                                    when (selectedEffect) {
                                        "EyesWhitening" -> {
                                            val activity = context as? BanubaActivity
                                            val currentLevel = ((activity?.eyesWhiteningPreview?.value ?: 0f) * 4).toInt()
                                            
                                            Text(
                                                text = "Eyes Whitening Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentLevel,
                                                onLevelChange = onEyesWhiteningLevelChange
                                            )
                                        }
                                        
                                        "Eyebags" -> {
                                            val activity = context as? BanubaActivity
                                            val currentLevel = ((activity?.eyebagsPreview?.value ?: 0f) * 4).toInt()
                                            
                                            Text(
                                                text = "Eyebags Concealer Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentLevel,
                                                onLevelChange = onEyebagsLevelChange
                                            )
                                        }
                                        
                                        "Eyeshadow" -> {
                                            val activity = context as? BanubaActivity
                                            val currentStyle = activity?.eyeshadowStylePreview?.value ?: 0
                                            val currentIntensity = ((activity?.eyeshadowIntensityPreview?.value ?: 0f) * 4).toInt()
                                            
                                            // Style selector
                                            EyeshadowStyleSelector(
                                                currentStyle = currentStyle,
                                                onStyleChange = onEyeshadowStyleChange
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = "Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentIntensity,
                                                onLevelChange = onEyeshadowIntensityChange
                                            )
                                        }
                                        
                                        "Eyelashes" -> {
                                            val activity = context as? BanubaActivity
                                            val currentStyle = activity?.eyelashStylePreview?.value ?: 0
                                            val currentIntensity = ((activity?.eyelashIntensityPreview?.value ?: 0f) * 4).toInt()
                                            
                                            // Style selector
                                            EyelashStyleSelector(
                                                currentStyle = currentStyle,
                                                onStyleChange = onEyelashStyleChange
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = "Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentIntensity,
                                                onLevelChange = onEyelashIntensityChange
                                            )
                                        }
                                        
                                        "Eyeliner" -> {
                                            val activity = context as? BanubaActivity
                                            val currentStyle = activity?.eyelinerStylePreview?.value ?: 0
                                            val currentIntensity = ((activity?.eyelinerIntensityPreview?.value ?: 0f) * 4).toInt()
                                            
                                            // Color selector
                                            EyelinerStyleSelector(
                                                currentStyle = currentStyle,
                                                onStyleChange = onEyelinerStyleChange
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = "Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentIntensity,
                                                onLevelChange = onEyelinerIntensityChange
                                            )
                                        }
                                        
                                        "Eyebrows" -> {
                                            val activity = context as? BanubaActivity
                                            val currentStyle = activity?.eyebrowStylePreview?.value ?: 0
                                            val currentIntensity = ((activity?.eyebrowIntensityPreview?.value ?: 0f) * 4).toInt()
                                            
                                            // Style selector
                                            EyebrowStyleSelector(
                                                currentStyle = currentStyle,
                                                onStyleChange = onEyebrowStyleChange
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = "Intensity",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            
                                            IntensitySelector(
                                                currentLevel = currentIntensity,
                                                onLevelChange = onEyebrowIntensityChange
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Save/Cancel buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onCloseSlider,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Gray.copy(alpha = 0.7f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text("✕ Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    
                                    Button(
                                        onClick = onSaveSlider,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SoftBlushPink,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text("✓ Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    
                        if (selectedMainEffect == "SoftLight") {
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            // Intensity selector
                            Text(
                                text = "Soft Light Intensity",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val activity = context as? BanubaActivity
                            val currentLevel = ((activity?.softlightPreview?.value ?: 0f) * 4).toInt()
                            
                            IntensitySelector(
                                currentLevel = currentLevel,
                                onLevelChange = onSoftlightLevelChange
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCloseSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.7f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✕ Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Button(
                                    onClick = onSaveSlider,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftBlushPink,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text("✓ Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        
                    } // Close selectedMainEffect != null
                } // Close Column
        }
    }
}

@Composable
fun EffectButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.8f),
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SaveMakeupDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Save Makeup Look",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Give your makeup look a name:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Look Name") },
                    placeholder = { Text("Enter a name...") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftBlushPink
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", color = Color.White)
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

