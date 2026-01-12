package com.TOTOMOFYP.VTOAPP

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.TOTOMOFYP.VTOAPP.ui.makeup.MakeupStorage
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.banuba.sdk.input.CameraDevice
import com.banuba.sdk.input.CameraDeviceConfigurator
import com.banuba.sdk.input.CameraInput
import com.banuba.sdk.output.SurfaceOutput
import com.banuba.sdk.output.FrameOutput
import com.banuba.sdk.output.IOutput
import com.banuba.sdk.player.Player
import com.banuba.sdk.player.PlayerTouchListener
import com.banuba.sdk.frame.FramePixelBuffer
import com.banuba.sdk.frame.FramePixelBufferFormat
import com.TOTOMOFYP.VTOAPP.ui.theme.VTOAppTheme
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import org.json.JSONObject

/**
 * Real-time face AR activity using Banuba SDK for live camera makeup
 */
class BanubaActivityCamera : ComponentActivity() {

    private companion object {
        private const val TAG = "BanubaActivityCamera"

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
        CameraDevice(requireNotNull(applicationContext), this@BanubaActivityCamera)
    }

    private lateinit var surfaceOutput: SurfaceOutput
    private lateinit var frameOutput: FrameOutput
    
    // Capture state
    private var isCapturing = mutableStateOf(false)
    private var captureCallback: ((Bitmap?) -> Unit)? = null

    internal var activeEffects = mutableStateOf(setOf<String>())
    private var isFrontCamera = mutableStateOf(true)
    private var isPhotoMode = mutableStateOf(false)
    
    // Makeup save functionality
    private val makeupStorage = MakeupStorage()
    private var showSaveDialog = mutableStateOf(false)
    private var saveDialogName = mutableStateOf("")
    private var isSaving = mutableStateOf(false)
    
    // Camera-specific UI state
    private var showMakeupPanel = mutableStateOf(false)
    private var capturedImageBitmap = mutableStateOf<Bitmap?>(null)
    private var showCaptureResult = mutableStateOf(false)
    
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
    
    // Helper function to save currently open category to session before switching
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            VTOAppTheme {
                // Create reactive derived states for intensity values
                val currentFoundationLevel by remember { derivedStateOf { 
                    val level = (foundationCategoryIntensityPreview.value * 4).toInt()
                    Log.d(TAG, "Foundation UI level: preview=${foundationCategoryIntensityPreview.value}, level=$level")
                    level
                } }
                val currentTeethWhiteningLevel by remember { derivedStateOf { 
                    val level = (teethWhiteningCategoryIntensityPreview.value * 4).toInt()
                    Log.d(TAG, "TeethWhitening UI level: preview=${teethWhiteningCategoryIntensityPreview.value}, level=$level")
                    level
                } }
                val currentSoftLightLevel by remember { derivedStateOf { 
                    val level = (softLightCategoryIntensityPreview.value * 4).toInt()
                    Log.d(TAG, "SoftLight UI level: preview=${softLightCategoryIntensityPreview.value}, level=$level")
                    level
                } }
                if (showCaptureResult.value) {
                    // Show capture result screen
                    CaptureResultScreen(
                        capturedImage = capturedImageBitmap.value,
                        onSaveClick = { saveCapturedImage() },
                        onBackClick = { 
                            showCaptureResult.value = false
                            capturedImageBitmap.value = null
                        }
                    )
                } else {
                    // Show camera screen
                    CameraMakeupScreen(
                        showMakeupPanel = showMakeupPanel.value,
                        onToggleMakeupPanel = { showMakeupPanel.value = !showMakeupPanel.value },
                        onCaptureClick = { captureImage() },
                        activeEffects = activeEffects.value,
                        selectedMainEffect = selectedMainEffect.value,
                        onSelectMainEffect = { effectName -> selectMainEffect(effectName) },
                        selectedEyeEffect = selectedEyeEffect.value,
                        onSelectEyeEffect = { effectPath -> selectEyeEffect(effectPath) },
                        selectedLipShade = selectedLipShade.value,
                        onLipShadeSelect = { shadeName -> selectLipShade(shadeName) },
                        selectedFoundationSkinTone = selectedFoundationSkinTone.value,
                        onFoundationSkinToneSelect = { skinToneIndex -> selectFoundationSkinTone(skinToneIndex) },
                        onFoundationLevelChange = { level -> updateFoundationLevel(level) },
                        onTeethWhiteningLevelChange = { level -> updateTeethWhiteningLevel(level) },
                        onSoftlightLevelChange = { level -> updateSoftlightLevel(level) },
                        onEyesWhiteningLevelChange = { level -> updateEyesWhiteningLevel(level) },
                        onEyebagsLevelChange = { level -> updateEyebagsLevel(level) },
                        onEyeshadowStyleChange = { style -> updateEyeshadowStyle(style) },
                        onEyeshadowIntensityChange = { level -> updateEyeshadowIntensity(level) },
                        onEyelashStyleChange = { style -> updateEyelashStyle(style) },
                        onEyelashIntensityChange = { level -> updateEyelashIntensity(level) },
                        onEyelinerStyleChange = { style -> updateEyelinerStyle(style) },
                        onEyelinerIntensityChange = { level -> updateEyelinerIntensity(level) },
                        onEyebrowStyleChange = { style -> updateEyebrowStyle(style) },
                        onEyebrowIntensityChange = { level -> updateEyebrowIntensity(level) },
                        onLipShadeIntensityChange = { level -> updateLipShadeIntensity(level) },
                        onFoundationCategoryIntensityChange = { level -> updateFoundationCategoryIntensity(level) },
                        onTeethWhiteningCategoryIntensityChange = { level -> updateTeethWhiteningCategoryIntensity(level) },
                        onSoftLightCategoryIntensityChange = { level -> updateSoftLightCategoryIntensity(level) },
                        // Current intensity values - using derived states
                        currentFoundationLevel = currentFoundationLevel,
                        currentTeethWhiteningLevel = currentTeethWhiteningLevel,
                        currentSoftLightLevel = currentSoftLightLevel,
                        currentEyesWhiteningLevel = (eyesWhiteningPreview.value * 4).toInt(),
                        currentEyebagsLevel = (eyebagsPreview.value * 4).toInt(),
                        currentEyeshadowStyle = eyeshadowStylePreview.value,
                        currentEyeshadowIntensity = (eyeshadowIntensityPreview.value * 4).toInt(),
                        currentEyelashStyle = eyelashStylePreview.value,
                        currentEyelashIntensity = (eyelashIntensityPreview.value * 4).toInt(),
                        currentEyelinerStyle = eyelinerStylePreview.value,
                        currentEyelinerIntensity = (eyelinerIntensityPreview.value * 4).toInt(),
                        currentEyebrowStyle = eyebrowStylePreview.value,
                        currentEyebrowIntensity = (eyebrowIntensityPreview.value * 4).toInt(),
                        currentLipIntensity = (lipShadeIntensityPreview.value * 4).toInt(),
                        // Save/cancel callbacks
                        onSaveChanges = { saveSliderChanges() },
                        onCancelChanges = { closeSliderWithoutSaving() }
                    )
                }
                
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



    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanubaSDK() {
        try {
            Log.d(TAG, "=== Starting Banuba SDK Setup ===")
            
            // Check if surface and output are ready
            Log.d(TAG, "Checking prerequisites:")
            Log.d(TAG, "  surfaceView initialized: ${::surfaceView.isInitialized}")
            Log.d(TAG, "  surfaceOutput initialized: ${::surfaceOutput.isInitialized}")
            
            if (::surfaceView.isInitialized) {
                Log.d(TAG, "  surfaceView dimensions: ${surfaceView.width}x${surfaceView.height}")
                Log.d(TAG, "  surfaceView holder: ${surfaceView.holder}")
            }
            
            // Configure camera for highest available quality according to Banuba SDK docs
            Log.d(TAG, "Configuring camera device...")
            cameraDevice.configurator
                .setLens(CameraDeviceConfigurator.LensSelector.FRONT) // Front camera for makeup
                .setVideoCaptureSize(CameraDeviceConfigurator.FHD_CAPTURE_SIZE) // Use HD for video (1280x720)
                .setImageCaptureSize(CameraDeviceConfigurator.QHD_CAPTURE_SIZE) // Use HD for image capture (1280x720)
                .commit() // Must call commit() to apply settings per SDK docs
            Log.d(TAG, "Camera device configured successfully")

            // Start camera device as per SDK documentation
            Log.d(TAG, "Starting camera device...")
            cameraDevice.start()
            Log.d(TAG, "Camera device started successfully")

            // Connect input and output to player according to SDK docs structure
            Log.d(TAG, "Connecting input/output to player...")
            val cameraInput = CameraInput(cameraDevice)
            Log.d(TAG, "CameraInput created: $cameraInput")
            Log.d(TAG, "SurfaceOutput: $surfaceOutput")
            Log.d(TAG, "FrameOutput: $frameOutput")
            
            // Use both SurfaceOutput for display and FrameOutput for capture
            player.use(cameraInput, arrayOf(surfaceOutput, frameOutput))
            Log.d(TAG, "Player connected to input/outputs successfully")

            // Set touch listener for face interaction as per SDK docs
            Log.d(TAG, "Setting touch listener...")
            surfaceView.setOnTouchListener(PlayerTouchListener(applicationContext, player))
            Log.d(TAG, "Touch listener set successfully")

            Log.d(TAG, "=== Banuba SDK Setup Complete ===")
            Log.d(TAG, "Banuba Player API configured successfully with HD resolution (1280x720)")
        } catch (e: Exception) {
            Log.e(TAG, "=== Banuba SDK Setup Failed ===", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error cause: ${e.cause}")
            e.printStackTrace()
        }
    }

    internal fun initializeSurfaceView(surfaceView: SurfaceView) {
        Log.d(TAG, "initializeSurfaceView() called")
        Log.d(TAG, "SurfaceView dimensions: ${surfaceView.width}x${surfaceView.height}")
        
        this.surfaceView = surfaceView
        surfaceOutput = SurfaceOutput(surfaceView.holder)
        
        // Initialize FrameOutput for image capture
        frameOutput = FrameOutput(object : FrameOutput.IFramePixelBufferProvider {
            override fun onFrame(output: IOutput, framePixelBuffer: FramePixelBuffer?) {
                if (isCapturing.value && framePixelBuffer != null) {
                    Log.d(TAG, "FrameOutput received frame for capture")
                    
                    // Convert FramePixelBuffer to Bitmap
                    val bitmap = convertFramePixelBufferToBitmap(framePixelBuffer)
                    
                    // Reset capture state
                    isCapturing.value = false
                    
                    // Call the capture callback
                    captureCallback?.invoke(bitmap)
                    captureCallback = null
                }
            }
        })
        
        // Set FrameOutput format to RGBA for easier conversion to Bitmap
        frameOutput.setFormat(FramePixelBufferFormat.BPC8_RGBA)
        // Note: setOrientation may not be available in all SDK versions
        
        Log.d(TAG, "SurfaceOutput and FrameOutput created successfully")
        
        // Only setup Banuba SDK if permissions are granted
        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions granted, calling setupBanubaSDK()")
            setupBanubaSDK()
        } else {
            Log.d(TAG, "Permissions not granted, waiting for permission request result")
            // Check each permission again for detailed status
            REQUIRED_PERMISSIONS.forEach { permission ->
                val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "  $permission: ${if (granted) "GRANTED" else "DENIED"}")
            }
        }
    }
    
    private fun convertFramePixelBufferToBitmap(framePixelBuffer: FramePixelBuffer): Bitmap? {
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

    private fun openGallery() {
        // Stub function - gallery functionality not needed for live camera mode
    }
    
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
        
        lifecycleScope.launch {
            try {
                // Use the already captured bitmap instead of capturing again
                val bitmap = capturedImageBitmap.value
                if (bitmap != null) {
                    makeupStorage.saveMakeupLook(saveDialogName.value.trim(), bitmap)
                        .onSuccess {
                            runOnUiThread {
                                Toast.makeText(this@BanubaActivityCamera, "Makeup look saved!", Toast.LENGTH_SHORT).show()
                                showSaveDialog.value = false
                                saveDialogName.value = ""
                                // Go back to camera view after successful save
                                showCaptureResult.value = false
                                capturedImageBitmap.value = null
                            }
                        }
                        .onFailure { exception ->
                            runOnUiThread {
                                Toast.makeText(this@BanubaActivityCamera, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@BanubaActivityCamera, "No captured image to save", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving makeup look", e)
                runOnUiThread {
                    Toast.makeText(this@BanubaActivityCamera, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSaving.value = false
            }
        }
    }
    
    private fun captureCurrentFrame(): Bitmap? {
        return try {
            Log.d(TAG, "Attempting to capture frame using Banuba FrameOutput")
            
            if (!::frameOutput.isInitialized) {
                Log.e(TAG, "FrameOutput not initialized")
                Toast.makeText(this, "Camera not ready for capture", Toast.LENGTH_SHORT).show()
                return null
            }
            
            // Set capture state and callback for async capture
            isCapturing.value = true
            captureCallback = { bitmap ->
                // Ensure UI updates happen on the main thread
                runOnUiThread {
                    if (bitmap != null) {
                        capturedImageBitmap.value = bitmap
                        showCaptureResult.value = true
                        Log.d(TAG, "Image captured successfully using Banuba FrameOutput")
                    } else {
                        Toast.makeText(this@BanubaActivityCamera, "Failed to capture image", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to capture image - bitmap is null")
                    }
                }
            }
            
            Log.d(TAG, "Capture request initiated - waiting for next frame from FrameOutput")
            return null // Return null since we're handling async capture
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture frame", e)
            isCapturing.value = false
            captureCallback = null
            null
        }
    }
    
    private fun captureImage() {
        Log.d(TAG, "Capture button clicked")
        
        if (isCapturing.value) {
            Log.w(TAG, "Capture already in progress, ignoring request")
            return
        }
        
        // Use the new Banuba FrameOutput capture method
        captureCurrentFrame()
    }
    
    private fun saveCapturedImage() {
        val bitmap = capturedImageBitmap.value
        if (bitmap != null) {
            showSaveDialog.value = true
        } else {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }
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
        
        Log.d(TAG, "onRequestPermissionsResult() called")
        Log.d(TAG, "Request code: $requestCode")
        
        permissions.forEachIndexed { index, permission ->
            val result = if (index < grantResults.size) grantResults[index] else -1
            val granted = result == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permission result: $permission = ${if (granted) "GRANTED" else "DENIED"} (code: $result)")
        }

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted after request, starting camera")
                startCamera()
            } else {
                val notGrantedPermission = permissions.zip(grantResults.toTypedArray())
                    .firstOrNull { (_, result) -> result != PackageManager.PERMISSION_GRANTED }
                    ?.first

                Log.e(TAG, "Permission denied: $notGrantedPermission")
                Toast.makeText(
                    this,
                    "Permission denied: $notGrantedPermission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    
    // Main effect selection function (like selectEyeEffect but for main effects)
    private fun selectMainEffect(effectName: String) {
        // Save current main effect to session memory before switching
        selectedMainEffect.value?.let { currentEffect ->
            if (currentEffect != effectName) {
                saveToSessionMemory(currentEffect)
            }
        }
        
        selectedMainEffect.value = effectName
        
        // Special handling for Eyes category
        if (effectName == "Eyes") {
            // Auto-select EyesWhitening as default if no session data
            if (selectedEyeEffect.value == null && sessionMemory.keys.none { it.startsWith("Eyes") }) {
                selectedEyeEffect.value = "EyesWhitening"
                Log.d(TAG, "Auto-selected EyesWhitening for Eyes category")
            }
            
            // Load any saved eye effect from session memory
            selectedEyeEffect.value?.let { loadFromSessionMemory(it) }
        } else {
            // Load from session memory if available for regular effects
            loadFromSessionMemory(effectName)
            
            // Initialize preview values with current saved values if no session data exists
            if (!sessionMemory.containsKey(effectName)) {
                when (effectName) {
                    "Foundation" -> {
                        foundationCategoryIntensityPreview.value = foundationCategoryIntensity.value
                        foundationSkinTonePreview.value = foundationSkinTone.value
                        isFoundationCategoryExpanded.value = true
                    }
                    "TeethWhitening" -> {
                        teethWhiteningCategoryIntensityPreview.value = teethWhiteningCategoryIntensity.value
                        isTeethWhiteningCategoryExpanded.value = true
                    }
                    "SoftLight" -> {
                        softLightCategoryIntensityPreview.value = softLightCategoryIntensity.value
                        isSoftLightCategoryExpanded.value = true
                    }
                    "Lips" -> {
                        lipShadeIntensityPreview.value = lipShadeIntensity.value
                        isLipsCategoryExpanded.value = true
                    }
                }
            } else {
                // Set category expanded states when loading from session memory
                when (effectName) {
                    "Foundation" -> isFoundationCategoryExpanded.value = true
                    "TeethWhitening" -> isTeethWhiteningCategoryExpanded.value = true
                    "SoftLight" -> isSoftLightCategoryExpanded.value = true
                    "Lips" -> isLipsCategoryExpanded.value = true
                }
            }
            
            // Add effect to active effects for regular effects
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add(effectName)
            activeEffects.value = currentActive.toSet()
        }
        
        // Load effects with preview
        loadCombinedEffectsWithPreview(activeEffects.value)
        
        Log.d(TAG, "Selected main effect: $effectName")
    }
    
    private fun toggleMainEffectPanel() {
        isMainEffectPanelExpanded.value = !isMainEffectPanelExpanded.value
    }
    
    private fun selectEyeEffect(effectPath: String) {
        // Save current eye effect to session memory before switching
        selectedEyeEffect.value?.let { currentEffect ->
            if (currentEffect != effectPath) {
                saveToSessionMemory(currentEffect)
            }
        }
        
        selectedEyeEffect.value = effectPath
        
        // Load from session memory if available
        loadFromSessionMemory(effectPath)
        
        // Handle the effect like normal toggleEffect but within eyes category
        val adjustableEffects = listOf("EyesWhitening", "Eyebags", "Eyeshadow", "Eyelashes", "Eyeliner", "Eyebrows")
        
        if (adjustableEffects.contains(effectPath)) {
            // Save original state before opening adjustment controls
            originalActiveEffects = activeEffects.value.toSet()
            
            // Add effect to active if not already active
            val currentActive = activeEffects.value.toMutableSet()
            if (!currentActive.contains(effectPath)) {
                currentActive.add(effectPath)
                activeEffects.value = currentActive.toSet()
            }
            
            // Initialize preview values with current saved values
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
                    // Add eyebrows preview when implemented
                }
            }
            
            // DON'T set currentSliderEffect for eye effects - they use inline controls
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
            // Save current settings to session memory before closing
            saveToSessionMemory("Foundation")
            
            // Just close the panel, keep preview changes and active effects
            isFoundationCategoryExpanded.value = false
            selectedFoundationSkinTone.value = null
        } else {
            // Save current category settings before switching (if any category is open)
            saveCurrentCategoryToSession()
            
            // Opening Foundation panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isFoundationCategoryExpanded.value = true
            
            // Load from session memory if available, otherwise use defaults
            loadFromSessionMemory("Foundation")
            
            // Add Foundation to active effects for preview
            val currentActive = activeEffects.value.toMutableSet()
            currentActive.add("Foundation")
            activeEffects.value = currentActive.toSet()
            
            // Auto-select skin tone 3 (medium) as default if no skin tone selected and no session data
            if (selectedFoundationSkinTone.value == null && !sessionMemory.containsKey("Foundation")) {
                selectedFoundationSkinTone.value = 3
            }
            
            loadCombinedEffectsWithPreview(activeEffects.value)
            Log.d(TAG, "Opened Foundation adjustment panel")
        }
    }
    
    private fun toggleLipsCategory() {
        if (isLipsCategoryExpanded.value) {
            // Save current settings to session memory before closing
            selectedLipShade.value?.let { saveToSessionMemory(it) }
            
            // Just close the panel, keep preview changes and active effects
            isLipsCategoryExpanded.value = false
            selectedLipShade.value = null
        } else {
            // Save current category settings before switching (if any category is open)
            saveCurrentCategoryToSession()
            
            // Opening Lips panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isLipsCategoryExpanded.value = true
            
            // Auto-select Classic Red Glam as default if no shade selected and no session data
            if (selectedLipShade.value == null && sessionMemory.isEmpty()) {
                selectedLipShade.value = "ClassicRedGlam"
            }
            
            // Load from session memory if available
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
            // Save current settings to session memory before closing
            saveToSessionMemory("TeethWhitening")
            
            // Just close the panel, keep preview changes
            isTeethWhiteningCategoryExpanded.value = false
        } else {
            // Save current category settings before switching (if any category is open)
            saveCurrentCategoryToSession()
            
            // Opening Teeth Whitening panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isTeethWhiteningCategoryExpanded.value = true
            
            // Load from session memory if available, otherwise use defaults
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
            // Save current settings to session memory before closing
            saveToSessionMemory("SoftLight")
            
            // Just close the panel, keep preview changes
            isSoftLightCategoryExpanded.value = false
        } else {
            // Save current category settings before switching (if any category is open)
            saveCurrentCategoryToSession()
            
            // Opening Soft Light panel - close others but keep their preview changes
            closeAllCategoryPanelsWithoutChangingPreview()
            
            isSoftLightCategoryExpanded.value = true
            
            // Load from session memory if available, otherwise use defaults
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
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0) - same as lips
        foundationCategoryIntensityPreview.value = newValue
        
        Log.d(TAG, "Foundation intensity updated: level=$level, newValue=$newValue")
        
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
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0) - same as lips
        teethWhiteningCategoryIntensityPreview.value = newValue
        
        Log.d(TAG, "TeethWhitening intensity updated: level=$level, newValue=$newValue")
        
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
        val newValue = level * 0.25f // Convert level (0-4) to value (0.0-1.0) - same as lips
        softLightCategoryIntensityPreview.value = newValue
        
        Log.d(TAG, "SoftLight intensity updated: level=$level, newValue=$newValue")
        
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
        
        // Save ALL category intensities (regardless of which specific effect is selected)
        foundationCategoryIntensity.value = foundationCategoryIntensityPreview.value
        foundationSkinTone.value = foundationSkinTonePreview.value
        teethWhiteningCategoryIntensity.value = teethWhiteningCategoryIntensityPreview.value
        softLightCategoryIntensity.value = softLightCategoryIntensityPreview.value
        lipShadeIntensity.value = lipShadeIntensityPreview.value
        
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
                    // Use category intensity instead of old preview
                    teethWhiteningStrength.value = teethWhiteningCategoryIntensityPreview.value
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (teethWhiteningCategoryIntensityPreview.value > 0f) {
                        currentActive.add("TeethWhitening")
                    } else {
                        currentActive.remove("TeethWhitening")
                    }
                    activeEffects.value = currentActive.toSet()
                    
                    loadCombinedEffects(activeEffects.value)
                    Log.d(TAG, "Saved teeth whitening changes and closed panel")
                }
                "SoftLight" -> {
                    // Use category intensity instead of old preview
                    softlightStrength.value = softLightCategoryIntensityPreview.value
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (softLightCategoryIntensityPreview.value > 0f) {
                        currentActive.add("SoftLight")
                    } else {
                        currentActive.remove("SoftLight")
                    }
                    activeEffects.value = currentActive.toSet()
                    
                    loadCombinedEffects(activeEffects.value)
                    Log.d(TAG, "Saved soft light changes and closed panel")
                }
                "Foundation" -> {
                    // Use category intensity instead of old preview
                    foundationStrength.value = foundationCategoryIntensityPreview.value
                    foundationSkinTone.value = foundationSkinTonePreview.value ?: 3
                    
                    val currentActive = activeEffects.value.toMutableSet()
                    if (foundationCategoryIntensityPreview.value > 0f) {
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
        
        // Restore original activeEffects state (before the session started)
        activeEffects.value = originalActiveEffects
        loadCombinedEffects(activeEffects.value)
        
        // Close all panels including makeup panel
        closeAllCategoryPanels()
        selectedEyeEffect.value = null
        showMakeupPanel.value = false
        
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
        Log.d(TAG, "onStart() called")
        
        // Check each permission individually for detailed logging
        REQUIRED_PERMISSIONS.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")
        }
        
        if (!allPermissionsGranted()) {
            Log.d(TAG, "Requesting permissions: ${REQUIRED_PERMISSIONS.joinToString()}")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        } else {
            Log.d(TAG, "All permissions granted, starting camera")
            startCamera()
        }
    }

    private fun startCamera() {
        // If surface is already initialized, setup Banuba SDK
        if (::surfaceView.isInitialized && ::surfaceOutput.isInitialized) {
            setupBanubaSDK()
            Log.d(TAG, "Camera started after permission grant")
        } else {
            Log.d(TAG, "Surface not ready, camera will start when surface is initialized")
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
        try {
            player.play()
            Log.d(TAG, "Player resumed successfully")
            
            // Double-check if everything is set up correctly
            Log.d(TAG, "Post-resume state check:")
            Log.d(TAG, "  All permissions granted: ${allPermissionsGranted()}")
            Log.d(TAG, "  SurfaceView initialized: ${::surfaceView.isInitialized}")
            Log.d(TAG, "  SurfaceOutput initialized: ${::surfaceOutput.isInitialized}")
            
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
            // Release Banuba SDK resources
            cameraDevice.close()
            surfaceOutput.close()
            if (::frameOutput.isInitialized) {
                frameOutput.close()
            }
            player.close()
            
            Log.d(TAG, "Banuba SDK resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Banuba SDK resources", e)
        }
        super.onDestroy()
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
                    containerColor = com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
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

@Composable
fun CameraMakeupScreen(
    showMakeupPanel: Boolean,
    onToggleMakeupPanel: () -> Unit,
    onCaptureClick: () -> Unit,
    activeEffects: Set<String>,
    selectedMainEffect: String?,
    onSelectMainEffect: (String) -> Unit,
    selectedEyeEffect: String?,
    onSelectEyeEffect: (String) -> Unit,
    selectedLipShade: String?,
    onLipShadeSelect: (String) -> Unit,
    selectedFoundationSkinTone: Int?,
    onFoundationSkinToneSelect: (Int) -> Unit,
    onFoundationLevelChange: (Int) -> Unit,
    onTeethWhiteningLevelChange: (Int) -> Unit,
    onSoftlightLevelChange: (Int) -> Unit,
    onEyesWhiteningLevelChange: (Int) -> Unit,
    onEyebagsLevelChange: (Int) -> Unit,
    onEyeshadowStyleChange: (Int) -> Unit,
    onEyeshadowIntensityChange: (Int) -> Unit,
    onEyelashStyleChange: (Int) -> Unit,
    onEyelashIntensityChange: (Int) -> Unit,
    onEyelinerStyleChange: (Int) -> Unit,
    onEyelinerIntensityChange: (Int) -> Unit,
    onEyebrowStyleChange: (Int) -> Unit,
    onEyebrowIntensityChange: (Int) -> Unit,
    onLipShadeIntensityChange: (Int) -> Unit,
    onFoundationCategoryIntensityChange: (Int) -> Unit,
    onTeethWhiteningCategoryIntensityChange: (Int) -> Unit,
    onSoftLightCategoryIntensityChange: (Int) -> Unit,
    // Current intensity values
    currentFoundationLevel: Int = 2,
    currentTeethWhiteningLevel: Int = 2,
    currentSoftLightLevel: Int = 2,
    currentEyesWhiteningLevel: Int = 2,
    currentEyebagsLevel: Int = 2,
    currentEyeshadowStyle: Int = 0,
    currentEyeshadowIntensity: Int = 2,
    currentEyelashStyle: Int = 0,
    currentEyelashIntensity: Int = 2,
    currentEyelinerStyle: Int = 0,
    currentEyelinerIntensity: Int = 2,
    currentEyebrowStyle: Int = 0,
    currentEyebrowIntensity: Int = 2,
    currentLipIntensity: Int = 2,
    // Save/cancel callbacks
    onSaveChanges: () -> Unit = {},
    onCancelChanges: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Surface View
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    // Initialize Banuba components when surface is created
                    when (context) {
                        is BanubaActivityCamera -> context.initializeSurfaceView(surfaceView)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (!showMakeupPanel) {
            // Default camera controls: capture button center + makeup icon right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty space for left alignment
                Spacer(modifier = Modifier.weight(1f))
                
                // Capture button (center)
                FloatingActionButton(
                    onClick = onCaptureClick,
                    modifier = Modifier.size(64.dp),
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Makeup icon (right)
                FloatingActionButton(
                    onClick = onToggleMakeupPanel,
                    modifier = Modifier.size(48.dp),
                    containerColor = SoftBlushPink.copy(alpha = 0.9f),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Makeup",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Back button - positioned at top
            IconButton(
                onClick = onToggleMakeupPanel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Makeup panel - same container background as original
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                // Makeup effects tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val mainEffects = listOf(
                        "Foundation" to "Foundation",
                        "Lips" to "Lips", 
                        "TeethWhitening" to "Teeth White",
                        "SoftLight" to "Soft Light",
                        "Eyes" to "Eyes"
                    )
                    
                    items(mainEffects) { (effectKey, displayName) ->
                        val isSelected = when (effectKey) {
                            "Eyes" -> {
                                val eyeEffects = setOf("EyesWhitening", "Eyebags", "Eyeshadow", "Eyelashes", "Eyeliner", "Eyebrows")
                                activeEffects.intersect(eyeEffects).isNotEmpty() || selectedMainEffect == "Eyes"
                            }
                            else -> selectedMainEffect == effectKey || activeEffects.contains(effectKey)
                        }
                        
                        Button(
                            onClick = { onSelectMainEffect(effectKey) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.4f),
                                contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // Show effect-specific controls
                if (selectedMainEffect != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedMainEffect) {
                        "Foundation" -> {
                            FoundationControls(
                                selectedSkinTone = selectedFoundationSkinTone,
                                onSkinToneSelect = onFoundationSkinToneSelect,
                                onIntensityChange = onFoundationCategoryIntensityChange,
                                currentIntensity = currentFoundationLevel
                            )
                        }
                        "Eyes" -> {
                            EyeEffectsControls(
                                selectedEyeEffect = selectedEyeEffect,
                                onSelectEyeEffect = onSelectEyeEffect,
                                activeEffects = activeEffects,
                                onEyesWhiteningLevelChange = onEyesWhiteningLevelChange,
                                onEyebagsLevelChange = onEyebagsLevelChange,
                                onEyeshadowStyleChange = onEyeshadowStyleChange,
                                onEyeshadowIntensityChange = onEyeshadowIntensityChange,
                                onEyelashStyleChange = onEyelashStyleChange,
                                onEyelashIntensityChange = onEyelashIntensityChange,
                                onEyelinerStyleChange = onEyelinerStyleChange,
                                onEyelinerIntensityChange = onEyelinerIntensityChange,
                                onEyebrowStyleChange = onEyebrowStyleChange,
                                onEyebrowIntensityChange = onEyebrowIntensityChange,
                                // Current intensity levels and styles
                                currentEyesWhiteningLevel = currentEyesWhiteningLevel,
                                currentEyebagsLevel = currentEyebagsLevel,
                                currentEyeshadowStyle = currentEyeshadowStyle,
                                currentEyeshadowIntensity = currentEyeshadowIntensity,
                                currentEyelashStyle = currentEyelashStyle,
                                currentEyelashIntensity = currentEyelashIntensity,
                                currentEyelinerStyle = currentEyelinerStyle,
                                currentEyelinerIntensity = currentEyelinerIntensity,
                                currentEyebrowStyle = currentEyebrowStyle,
                                currentEyebrowIntensity = currentEyebrowIntensity
                            )
                        }
                        "Lips" -> {
                            LipControls(
                                selectedLipShade = selectedLipShade,
                                onLipShadeSelect = onLipShadeSelect,
                                onIntensityChange = onLipShadeIntensityChange,
                                currentIntensity = currentLipIntensity
                            )
                        }
                        "TeethWhitening" -> {
                            SimpleIntensityControl(
                                title = "Teeth Whitening Intensity",
                                onIntensityChange = onTeethWhiteningCategoryIntensityChange,
                                currentIntensity = currentTeethWhiteningLevel
                            )
                        }
                        "SoftLight" -> {
                            SimpleIntensityControl(
                                title = "Soft Light Intensity",
                                onIntensityChange = onSoftLightCategoryIntensityChange,
                                currentIntensity = currentSoftLightLevel
                            )
                        }
                    }
                    
                    // Save and Cancel buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onCancelChanges,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = onSaveChanges,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftBlushPink
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun CaptureResultScreen(
    capturedImage: Bitmap?,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Display captured image
        if (capturedImage != null) {
            Image(
                bitmap = capturedImage.asImageBitmap(),
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback if no image
            Text(
                "No image captured",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Top bar with back and save buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Button(
                onClick = onSaveClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftBlushPink
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}



@Composable
fun FoundationControls(
    selectedSkinTone: Int?,
    onSkinToneSelect: (Int) -> Unit,
    onIntensityChange: (Int) -> Unit,
    currentIntensity: Int = 2
) {
    Column {
        Text(
            text = "Skin Tone",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(7) { skinToneIndex ->
                val isSelected = selectedSkinTone == skinToneIndex
                Button(
                    onClick = { onSkinToneSelect(skinToneIndex) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tone ${skinToneIndex + 1}",
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        Text(
            text = "Intensity",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        CameraIntensitySelector(
            currentLevel = currentIntensity,
            onLevelChange = onIntensityChange
        )
    }
}



@Composable
fun SimpleIntensityControl(
    title: String,
    onIntensityChange: (Int) -> Unit,
    currentIntensity: Int = 2
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        CameraIntensitySelector(
            currentLevel = currentIntensity,
            onLevelChange = onIntensityChange
        )
    }
}



@Composable
fun EyeEffectsControls(
    selectedEyeEffect: String?,
    onSelectEyeEffect: (String) -> Unit,
    activeEffects: Set<String>,
    onEyesWhiteningLevelChange: (Int) -> Unit,
    onEyebagsLevelChange: (Int) -> Unit,
    onEyeshadowStyleChange: (Int) -> Unit,
    onEyeshadowIntensityChange: (Int) -> Unit,
    onEyelashStyleChange: (Int) -> Unit,
    onEyelashIntensityChange: (Int) -> Unit,
    onEyelinerStyleChange: (Int) -> Unit,
    onEyelinerIntensityChange: (Int) -> Unit,
    onEyebrowStyleChange: (Int) -> Unit,
    onEyebrowIntensityChange: (Int) -> Unit,
    // Current intensity levels
    currentEyesWhiteningLevel: Int = 2,
    currentEyebagsLevel: Int = 2,
    currentEyeshadowStyle: Int = 0,
    currentEyeshadowIntensity: Int = 2,
    currentEyelashStyle: Int = 0,
    currentEyelashIntensity: Int = 2,
    currentEyelinerStyle: Int = 0,
    currentEyelinerIntensity: Int = 2,
    currentEyebrowStyle: Int = 0,
    currentEyebrowIntensity: Int = 2
) {
    Column {
        // Eye effect buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            val eyeEffects = listOf(
                "EyesWhitening" to "Eyes White",
                "Eyebags" to "Eye Bags",
                "Eyeshadow" to "Eyeshadow",
                "Eyelashes" to "Eyelashes",
                "Eyeliner" to "Eyeliner",
                "Eyebrows" to "Eyebrows"
            )
            
            items(eyeEffects) { (effectKey, displayName) ->
                val isSelected = selectedEyeEffect == effectKey || activeEffects.contains(effectKey)
                Button(
                    onClick = { onSelectEyeEffect(effectKey) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        // Show controls for selected eye effect
        selectedEyeEffect?.let { effect ->
            when (effect) {
                "EyesWhitening", "Eyebags" -> {
                    SimpleIntensityControl(
                        title = "${effect.replace("EyesWhitening", "Eyes Whitening").replace("Eyebags", "Eye Bags")} Intensity",
                        onIntensityChange = if (effect == "EyesWhitening") onEyesWhiteningLevelChange else onEyebagsLevelChange,
                        currentIntensity = if (effect == "EyesWhitening") currentEyesWhiteningLevel else currentEyebagsLevel
                    )
                }
                "Eyeshadow" -> {
                    Column {
                        Text(
                            text = "Eyeshadow Style",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CameraEyeshadowStyleSelector(
                            currentStyle = currentEyeshadowStyle,
                            onStyleChange = onEyeshadowStyleChange
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleIntensityControl(
                            title = "Eyeshadow Intensity",
                            onIntensityChange = onEyeshadowIntensityChange,
                            currentIntensity = currentEyeshadowIntensity
                        )
                    }
                }
                "Eyelashes" -> {
                    Column {
                        Text(
                            text = "Eyelash Style",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CameraEyelashStyleSelector(
                            currentStyle = currentEyelashStyle,
                            onStyleChange = onEyelashStyleChange
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleIntensityControl(
                            title = "Eyelash Intensity",
                            onIntensityChange = onEyelashIntensityChange,
                            currentIntensity = currentEyelashIntensity
                        )
                    }
                }
                "Eyeliner" -> {
                    Column {
                        Text(
                            text = "Eyeliner Style",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CameraEyelinerStyleSelector(
                            currentStyle = currentEyelinerStyle,
                            onStyleChange = onEyelinerStyleChange
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleIntensityControl(
                            title = "Eyeliner Intensity",
                            onIntensityChange = onEyelinerIntensityChange,
                            currentIntensity = currentEyelinerIntensity
                        )
                    }
                }
                "Eyebrows" -> {
                    Column {
                        Text(
                            text = "Eyebrow Style",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CameraEyebrowStyleSelector(
                            currentStyle = currentEyebrowStyle,
                            onStyleChange = onEyebrowStyleChange
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleIntensityControl(
                            title = "Eyebrow Intensity",
                            onIntensityChange = onEyebrowIntensityChange,
                            currentIntensity = currentEyebrowIntensity
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun LipControls(
    selectedLipShade: String?,
    onLipShadeSelect: (String) -> Unit,
    onIntensityChange: (Int) -> Unit,
    currentIntensity: Int = 2
) {
    Column {
        Text(
            text = "Lip Shades",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            val lipShades = listOf(
                "ClassicRedGlam" to "Classic Red",
                "WineLuxury" to "Wine",
                "SoftNudeGlow" to "Nude",
                "MauveElegance" to "Mauve",
                "PeachyCoralPop" to "Peach",
                "BerryShine" to "Berry",
                "PlumDrama" to "Plum",
                "BabyPinkGlossy" to "Pink"
            )
            
            items(lipShades) { (shadeKey, displayName) ->
                val isSelected = selectedLipShade == shadeKey
                Button(
                    onClick = { onLipShadeSelect(shadeKey) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SoftBlushPink else Color.White.copy(alpha = 0.3f),
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        SimpleIntensityControl(
            title = "Lip Intensity",
            onIntensityChange = onIntensityChange,
            currentIntensity = currentIntensity
        )
    }
}



@Composable
fun CameraEyebrowStyleSelector(
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
fun CameraEyeshadowStyleSelector(
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
fun CameraEyelashStyleSelector(
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
fun CameraEyelinerStyleSelector(
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
fun CameraIntensitySelector(
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
