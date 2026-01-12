package com.TOTOMOFYP.VTOAPP.ui.outfits

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.TOTOMOFYP.VTOAPP.repositories.ApiClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Class for managing outfit storage operations with Firebase
 */
class OutfitStorage {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val firestore = FirebaseFirestore.getInstance()
    private val apiClient = ApiClient()
    private val TAG = "OutfitStorage"
    
    // In-memory cache of outfits to reduce network calls
    private val outfitsCache = ConcurrentHashMap<String, List<Outfit>>()
    
    /**
     * Uploads an outfit image to FastAPI server with Gemini analysis and saves to Firebase
     */
    suspend fun saveOutfit(
        userId: String,
        uri: Uri,
        name: String,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting outfit save process with FastAPI endpoint")
            
            // Convert URI to temporary file for FastAPI upload
            val tempFile = createTempFileFromUri(context, uri)
                ?: return@withContext Result.failure(Exception("Failed to create temporary file from URI"))
            
            try {
                // Upload to FastAPI server with Gemini analysis
                val uploadResult = apiClient.uploadOutfit(userId, tempFile.absolutePath, name)
                
                if (!uploadResult.success) {
                    return@withContext Result.failure(Exception(uploadResult.message))
                }
                
                Log.d(TAG, "FastAPI upload successful: ${uploadResult.message}")
                
                // Parse metadata from FastAPI response
                val metadata = try {
                    if (!uploadResult.metadata.isNullOrEmpty()) {
                        val metadataJson = JSONObject(uploadResult.metadata)
                        OutfitMetadata(
                            description = metadataJson.optString("description", ""),
                            occasion = metadataJson.optString("occasion", ""),
                            color = metadataJson.optString("color", ""),
                            style = metadataJson.optString("style", "")
                        )
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON: ${e.message}")
                    null
                }
                
                // Clear cache for this user since we have new data
                outfitsCache.remove(userId)
                
                // Return the item ID from FastAPI response
                return@withContext Result.success(uploadResult.itemId ?: "")
                
            } finally {
                // Clean up temporary file
                tempFile.delete()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving outfit via FastAPI", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Helper method to convert URI to temporary file
     */
    private suspend fun createTempFileFromUri(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            
            val tempFile = File.createTempFile("outfit_upload", ".png", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from URI", e)
            null
        }
    }
    
    /**
     * Retrieves all outfits for a user from Firestore with caching
     */
    suspend fun getOutfits(userId: String): Result<List<Outfit>> = withContext(Dispatchers.IO) {
        try {
            // Return cached outfits if available
            val cachedOutfits = outfitsCache[userId]
            if (cachedOutfits != null) {
                Log.d(TAG, "Returning cached outfits for user $userId")
                return@withContext Result.success(cachedOutfits)
            }
            
            val outfitsList = mutableListOf<Outfit>()
            
            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("outfits")
                .get()
                .await()
            
            for (document in querySnapshot.documents) {
                Log.d(TAG, "Processing document: ${document.id}")
                Log.d(TAG, "Document data: ${document.data}")
                
                // Use document.id if no "id" field exists (FastAPI saves by document ID)
                val id = document.getString("id") ?: document.id
                val name = document.getString("outfit_name") ?: document.getString("name") ?: ""
                val imageUrl = document.getString("image_url") ?: document.getString("imageUrl") ?: ""
                val timestamp = document.getString("timestamp") ?: ""
                val date = document.getLong("date") ?: 0L
                // Default to false if isFavorite field doesn't exist (new outfits from FastAPI)
                val isFavorite = document.getBoolean("isFavorite") ?: false
                
                Log.d(TAG, "Parsed outfit: id=$id, name=$name, imageUrl=$imageUrl, timestamp=$timestamp")
                
                // Parse metadata if available
                val metadata = try {
                    val metadataMap = document.get("metadata") as? Map<String, Any>
                    if (metadataMap != null) {
                        OutfitMetadata(
                            description = metadataMap["description"]?.toString() ?: "",
                            occasion = metadataMap["occasion"]?.toString() ?: "",
                            color = metadataMap["color"]?.toString() ?: "",
                            style = metadataMap["style"]?.toString() ?: ""
                        )
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata for outfit $id: ${e.message}")
                    null
                }
                
                // Use timestamp if available, otherwise fall back to date
                val displayDate = if (timestamp.isNotEmpty()) {
                    try {
                        // Assume timestamp is ISO format from FastAPI
                        val instant = java.time.Instant.parse(timestamp)
                        java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date.from(instant))
                    } catch (e: Exception) {
                        // Fallback to original timestamp or current date
                        java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                    }
                } else {
                    java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(date))
                }
                
                val outfit = Outfit(
                    id = id,
                    name = name,
                    imageUrl = imageUrl,
                    date = displayDate,
                    isFavorite = isFavorite,
                    metadata = metadata,
                    timestamp = timestamp
                )
                
                outfitsList.add(outfit)
            }
            
            Log.d(TAG, "Retrieved ${outfitsList.size} outfits for user $userId")
            
            // Cache the results
            outfitsCache[userId] = outfitsList
            
            return@withContext Result.success(outfitsList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting outfits", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Deletes an outfit from both Firestore and Storage
     */
    suspend fun deleteOutfit(userId: String, outfitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete from Firestore first
            firestore.collection("users")
                .document(userId)
                .collection("outfits")
                .document(outfitId)
                .delete()
                .await()
            
            // Then delete the image from Storage
            val storagePath = "users/$userId/outfits/$outfitId.jpg"
            storage.reference.child(storagePath).delete().await()
            
            Log.d(TAG, "Outfit completely deleted: $outfitId")
            
            // Clear cache for this user
            outfitsCache.remove(userId)
            
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting outfit", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Toggles the favorite status of an outfit
     */
    suspend fun toggleFavorite(userId: String, outfitId: String, isFavorite: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("outfits")
                .document(outfitId)
            
            // Check if document exists and update or create the isFavorite field
            val document = docRef.get().await()
            if (document.exists()) {
                // Document exists, update the isFavorite field
                docRef.update("isFavorite", isFavorite).await()
                Log.d(TAG, "Updated existing outfit favorite status: $outfitId to $isFavorite")
            } else {
                Log.w(TAG, "Outfit document $outfitId does not exist, cannot update favorite status")
                return@withContext Result.failure(Exception("Outfit not found"))
            }
            
            // Clear cache for this user to reflect changes
            outfitsCache.remove(userId)
            
            return@withContext Result.success(isFavorite)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating outfit favorite status for $outfitId", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Clear cache for a specific user to force fresh data retrieval
     */
    fun clearCache(userId: String) {
        outfitsCache.remove(userId)
        Log.d(TAG, "Cleared cache for user: $userId")
    }
} 