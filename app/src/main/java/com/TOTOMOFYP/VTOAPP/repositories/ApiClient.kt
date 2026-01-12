package com.TOTOMOFYP.VTOAPP.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    
    // Production ngrok URLs (commented for local development)
    private val segmentationApiUrl = "https://a6f51b34253c.ngrok-free.app/segment"
    private val categorizationApiUrl = "https://19252b3ff006.ngrok-free.app/categorize"
    private val outfitUploadApiUrl = "https://19252b3ff006.ngrok-free.app/upload_outfit"
    // Extract host and port from apiUrl properly
    private val apiHost: String
    private val apiPort: Int

    init {
        try {
            // Parse URL properly with java.net.URL
            val url = java.net.URL(segmentationApiUrl)
            apiHost = url.host
            apiPort = if (url.port == -1) {
                // Use default ports if not specified
                if (url.protocol == "https") 443 else 80
            } else {
                url.port
            }
            Log.d(TAG, "Initialized ApiClient with host: $apiHost, port: $apiPort")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing segmentation API URL", e)
            throw RuntimeException("Invalid segmentation API URL: ${e.message}", e)
        }
    }

    /**
     * Check if the API server is reachable
     * @return true if the server is reachable, false otherwise
     */
    suspend fun isServerReachable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Checking if server at $apiHost:$apiPort is reachable...")
            val socket = Socket()
            // Set a timeout of 5 seconds
            socket.connect(InetSocketAddress(apiHost, apiPort), 5000)
            socket.close()
            Log.d(TAG, "Server is reachable")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Server is not reachable: ${e.message}", e)
            false
        }
    }

    /**
     * Check if the API endpoint is responding
     * @return true if the endpoint is responding, false otherwise
     */
    suspend fun isApiEndpointResponding(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Checking if API endpoint is responding...")
            val apiBaseUrl = segmentationApiUrl.substringBeforeLast("/")
            val request = Request.Builder()
                .url("$apiBaseUrl/")
                .get()
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                val isResponding = response.isSuccessful
                Log.d(TAG, "API endpoint is${if (isResponding) "" else " not"} responding")
                isResponding
            }
        } catch (e: Exception) {
            Log.e(TAG, "API endpoint check failed: ${e.message}", e)
            false
        }
    }

    // New automatic segmentation method (no bounding box required)
    suspend fun segmentImageAutomatic(
        context: Context,
        imagePath: String
    ): Bitmap = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting automatic segmentation request. Validating file at $imagePath")
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file not found at $imagePath")
                throw IOException("Image file not found at $imagePath")
            }
            
            Log.d(TAG, "File exists, size: ${file.length()} bytes")
            Log.d(TAG, "Creating request body for automatic segmentation")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(segmentationApiUrl)
                .post(requestBody)
                .build()

            Log.d(TAG, "Request built, sending to $segmentationApiUrl")

            return@withContext withTimeoutOrNull(180000) {
                suspendCoroutine { continuation ->
                    Log.d(TAG, "Executing automatic segmentation request...")
                    okHttpClient.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            val errorMsg = when (e) {
                                is SocketTimeoutException -> "Connection timed out. Please check if the API server is running and accessible."
                                else -> "API call failed: ${e.message}"
                            }
                            Log.e(TAG, errorMsg, e)
                            continuation.resumeWithException(IOException(errorMsg, e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                val errorMsg = "API call failed with code ${response.code}: ${response.message}"
                                Log.e(TAG, errorMsg)
                                continuation.resumeWithException(IOException(errorMsg))
                                return
                            }

                            try {
                                Log.d(TAG, "Received successful response, parsing JSON...")
                                val jsonResponse = response.body?.string()
                                val jsonObject = JSONObject(jsonResponse ?: "{}")
                                
                                if (jsonObject.has("mask")) {
                                    Log.d(TAG, "Mask found in response")
                                    val base64Image = jsonObject.getString("mask")
                                    Log.d(TAG, "Base64 image received, length: ${base64Image.length}")
                                    
                                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                    Log.d(TAG, "Base64 decoded, bytes length: ${imageBytes.size}")
                                    
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    Log.d(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}")
                                    
                                    continuation.resume(bitmap)
                                } else if (jsonObject.has("error")) {
                                    val errorMsg = "Segmentation failed: ${jsonObject.getString("error")}"
                                    Log.e(TAG, errorMsg)
                                    continuation.resumeWithException(IOException(errorMsg))
                                } else {
                                    val errorMsg = "Segmentation failed: Unexpected API response format"
                                    Log.e(TAG, errorMsg)
                                    continuation.resumeWithException(IOException(errorMsg))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing API response", e)
                                continuation.resumeWithException(e)
                            }
                        }
                    })
                }
            } ?: throw IOException("Segmentation request timed out after 3 minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during automatic segmentation process", e)
            throw e
        }
    }

    // Data class for categorization response
    data class CategorizationResult(
        val success: Boolean,
        val message: String,
        val itemId: String? = null,
        val metadata: String? = null
    )

    // Data class for outfit upload response
    data class OutfitUploadResult(
        val success: Boolean,
        val message: String,
        val itemId: String? = null,
        val outfitName: String? = null,
        val imageUrl: String? = null,
        val metadata: String? = null
    )

    // Categorization API method
    suspend fun categorizeClothingWithAI(
        userId: String,
        imagePath: String
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING AI CATEGORIZATION REQUEST ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Using full AI categorization endpoint")
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file not found at $imagePath")
                throw IOException("Image file not found at $imagePath")
            }
            
            Log.d(TAG, "File exists, size: ${file.length()} bytes")
            Log.d(TAG, "Creating request body for categorization")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$categorizationApiUrl/$userId")
                .post(requestBody)
                .build()

            Log.d(TAG, "Request built, sending to AI categorization endpoint: $categorizationApiUrl/$userId")

            return@withContext withTimeoutOrNull(180000) {
                suspendCoroutine { continuation ->
                    Log.d(TAG, "Executing categorization request...")
                    okHttpClient.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            val errorMsg = when (e) {
                                is SocketTimeoutException -> "Categorization request timed out"
                                else -> "Categorization API call failed: ${e.message}"
                            }
                            Log.e(TAG, errorMsg, e)
                            continuation.resumeWithException(IOException(errorMsg, e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                val errorMsg = "Categorization API call failed with code ${response.code}: ${response.message}"
                                Log.e(TAG, errorMsg)
                                continuation.resumeWithException(IOException(errorMsg))
                                return
                            }

                            try {
                                Log.d(TAG, "Received successful categorization response")
                                val responseBody = response.body?.string() ?: "{}"
                                val jsonObject = JSONObject(responseBody)
                                
                                when {
                                    jsonObject.has("message") -> {
                                        Log.d(TAG, "=== AI CATEGORIZATION SUCCESSFUL ===")
                                        val message = jsonObject.getString("message")
                                        val itemId = jsonObject.optString("item_id", null)
                                        val metadata = jsonObject.optString("metadata", "{}")
                                        Log.d(TAG, "Server response: $message")
                                        Log.d(TAG, "Item ID: $itemId")
                                        Log.d(TAG, "AI metadata: $metadata")
                                        
                                        continuation.resume(
                                            CategorizationResult(
                                                success = true,
                                                message = message,
                                                itemId = itemId,
                                                metadata = metadata
                                            )
                                        )
                                    }
                                    jsonObject.has("error") -> {
                                        val errorMsg = "Categorization failed: ${jsonObject.getString("error")}"
                                        Log.e(TAG, errorMsg)
                                        continuation.resume(
                                            CategorizationResult(
                                                success = false,
                                                message = errorMsg
                                            )
                                        )
                                    }
                                    else -> {
                                        val errorMsg = "Categorization failed: Unexpected API response format"
                                        Log.e(TAG, errorMsg)
                                        continuation.resume(
                                            CategorizationResult(
                                                success = false,
                                                message = errorMsg
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing categorization API response", e)
                                continuation.resume(
                                    CategorizationResult(
                                        success = false,
                                        message = "Error parsing server response: ${e.message}"
                                    )
                                )
                            }
                        }
                    })
                }
            } ?: CategorizationResult(
                success = false,
                message = "Categorization request timed out after 3 minutes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during categorization process", e)
            return@withContext CategorizationResult(
                success = false,
                message = "Network error: ${e.message}"
            )
        }
    }

    /**
     * Convert a bitmap to a base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    /**
     * Get the segmentation API URL for logging purposes
     */
    fun getSegmentationApiUrl(): String {
        return segmentationApiUrl
    }
    
    /**
     * Get the categorization API URL for logging purposes
     */
    fun getCategorizationApiUrl(): String {
        return categorizationApiUrl
    }

    /**
     * Upload outfit to FastAPI server with Gemini analysis
     */
    suspend fun uploadOutfit(
        userId: String,
        imagePath: String,
        outfitName: String
    ): OutfitUploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING OUTFIT UPLOAD REQUEST ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Outfit Name: $outfitName")
            Log.d(TAG, "Using outfit upload endpoint: $outfitUploadApiUrl")
            
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file not found at $imagePath")
                throw IOException("Image file not found at $imagePath")
            }
            
            Log.d(TAG, "File exists, size: ${file.length()} bytes")
            Log.d(TAG, "Creating multipart request body")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/png".toMediaTypeOrNull())
                )
                .addFormDataPart("outfit_name", outfitName)
                .build()

            val request = Request.Builder()
                .url("$outfitUploadApiUrl/$userId")
                .post(requestBody)
                .build()

            Log.d(TAG, "Request built, sending to outfit upload endpoint: $outfitUploadApiUrl/$userId")

            return@withContext withTimeoutOrNull(180000) {
                suspendCoroutine { continuation ->
                    Log.d(TAG, "Executing outfit upload request...")
                    okHttpClient.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            val errorMsg = when (e) {
                                is SocketTimeoutException -> "Outfit upload request timed out"
                                else -> "Outfit upload API call failed: ${e.message}"
                            }
                            Log.e(TAG, errorMsg, e)
                            continuation.resumeWithException(IOException(errorMsg, e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                val errorMsg = "Outfit upload API call failed with code ${response.code}: ${response.message}"
                                Log.e(TAG, errorMsg)
                                continuation.resumeWithException(IOException(errorMsg))
                                return
                            }

                            try {
                                Log.d(TAG, "Received successful outfit upload response")
                                val responseBody = response.body?.string() ?: "{}"
                                val jsonObject = JSONObject(responseBody)
                                
                                when {
                                    jsonObject.has("message") -> {
                                        Log.d(TAG, "=== OUTFIT UPLOAD SUCCESSFUL ===")
                                        val message = jsonObject.getString("message")
                                        val itemId = jsonObject.optString("item_id", null)
                                        val outfitName = jsonObject.optString("outfit_name", null)
                                        val imageUrl = jsonObject.optString("image_url", null)
                                        val metadata = jsonObject.optString("metadata", "{}")
                                        
                                        Log.d(TAG, "Server response: $message")
                                        Log.d(TAG, "Item ID: $itemId")
                                        Log.d(TAG, "Outfit Name: $outfitName")
                                        Log.d(TAG, "Image URL: $imageUrl")
                                        Log.d(TAG, "AI metadata: $metadata")
                                        
                                        continuation.resume(
                                            OutfitUploadResult(
                                                success = true,
                                                message = message,
                                                itemId = itemId,
                                                outfitName = outfitName,
                                                imageUrl = imageUrl,
                                                metadata = metadata
                                            )
                                        )
                                    }
                                    jsonObject.has("error") -> {
                                        val errorMsg = "Outfit upload failed: ${jsonObject.getString("error")}"
                                        Log.e(TAG, errorMsg)
                                        continuation.resume(
                                            OutfitUploadResult(
                                                success = false,
                                                message = errorMsg
                                            )
                                        )
                                    }
                                    else -> {
                                        val errorMsg = "Outfit upload failed: Unexpected API response format"
                                        Log.e(TAG, errorMsg)
                                        continuation.resume(
                                            OutfitUploadResult(
                                                success = false,
                                                message = errorMsg
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing outfit upload API response", e)
                                continuation.resume(
                                    OutfitUploadResult(
                                        success = false,
                                        message = "Error parsing server response: ${e.message}"
                                    )
                                )
                            }
                        }
                    })
                }
            } ?: OutfitUploadResult(
                success = false,
                message = "Outfit upload request timed out after 3 minutes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during outfit upload process", e)
            return@withContext OutfitUploadResult(
                success = false,
                message = "Network error: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "ApiClient"
    }
} 