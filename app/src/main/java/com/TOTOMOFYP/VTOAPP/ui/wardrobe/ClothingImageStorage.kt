package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import org.json.JSONObject

// Data class to hold clothing image information - updated to match server format
data class ClothingImageItem(
    val id: String,
    val imageUrl: String,
    val category: WardrobeCategory,
    val color: String = "",
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val pattern: String = "",
    val style: String = "",
    val occasion: String = "",
    val imageName: String = "",
    val displayName: String = ""
)

class ClothingImageStorage {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ClothingImageStorage"

    // Local upload removed - server handles upload and storage via /categorize/{user_id} endpoint
    
    /**
     * Deletes a clothing item from Firestore
     * Note: Server manages Firebase Storage, so we only delete Firestore document
     */
    fun deleteClothingItem(
        userId: String,
        clothingId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Delete from Firestore - server manages storage cleanup
        firestore.collection("users")
            .document(userId)
            .collection("wardrobeItems")
            .document(clothingId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Clothing item deleted from Firestore: $clothingId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document from Firestore", e)
                onFailure(e)
            }
    }
    
    // Get clothing images for a specific category from Firestore
    fun getClothingImages(
        userId: String,
        category: WardrobeCategory,
        onSuccess: (List<ClothingImageItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Always get all items and filter in the app to avoid Firestore indexing issues
        val query = firestore.collection("users")
            .document(userId)
            .collection("wardrobeItems")
        
        query.get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Retrieved ${documents.size()} documents from Firestore for category: ${category.name}")
                val items = mutableListOf<ClothingImageItem>()
                for (document in documents) {
                    try {
                        val id = document.id // Use document ID as item ID
                        val imageUrl = document.getString("image_url") ?: ""
                        val imageName = document.getString("image_name") ?: ""
                        // Handle both Firestore Timestamp and ISO string formats
                        val timestamp = try {
                            // Try Firestore Timestamp first
                            document.getTimestamp("timestamp")?.toDate()?.time
                        } catch (e: Exception) {
                            // Fallback to ISO string format from server
                            val timestampString = document.getString("timestamp")
                            if (timestampString != null) {
                                try {
                                    java.time.Instant.parse(timestampString).toEpochMilli()
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to parse timestamp string: $timestampString")
                                    System.currentTimeMillis()
                                }
                            } else {
                                System.currentTimeMillis()
                            }
                        } ?: System.currentTimeMillis()
                        
                        // Handle metadata - could be string or object from Firestore
                        val metadata = try {
                            // Try to get as object first (direct object from Firestore)
                            val metadataMap = document.get("metadata") as? Map<String, Any>
                            if (metadataMap != null) {
                                org.json.JSONObject(metadataMap)
                            } else {
                                // Fallback to string format
                                val metadataJson = document.getString("metadata") ?: "{}"
                                org.json.JSONObject(metadataJson)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing metadata", e)
                            org.json.JSONObject() // Empty object as fallback
                        }
                        
                        // Extract fields from metadata
                        val description = metadata.optString("description", "")
                        val categoryName = metadata.optString("category", "other")
                        val color = metadata.optString("color", "")
                        val pattern = metadata.optString("pattern", "")
                        val style = metadata.optString("style", "")
                        val occasion = metadata.optString("occasion", "")
                        
                        Log.d(TAG, "Document ${document.id}: category='$categoryName', requested filter='${category.name.lowercase()}'")
                        
                        // Convert category string to WardrobeCategory enum
                        val wardrobeCategory = try {
                            when (categoryName.lowercase()) {
                                "all" -> WardrobeCategory.ALL
                                "top", "upper" -> WardrobeCategory.TOP
                                "bottom" -> WardrobeCategory.BOTTOM
                                "full_body" -> WardrobeCategory.FULL_BODY
                                else -> WardrobeCategory.OTHER
                            }
                        } catch (e: Exception) {
                            WardrobeCategory.OTHER // Default to OTHER if category can't be parsed
                        }
                        
                        val displayName = document.getString("display_name") ?: ""
                        
                        val item = ClothingImageItem(
                            id = id,
                            imageUrl = imageUrl,
                            category = wardrobeCategory,
                            color = color,
                            uploadTimestamp = timestamp,
                            description = description,
                            pattern = pattern,
                            style = style,
                            occasion = occasion,
                            imageName = imageName,
                            displayName = displayName
                        )
                        Log.d(TAG, "Parsed item: $id, category: ${wardrobeCategory.name}, color: $color")
                        Log.d(TAG, "Image URL: $imageUrl")
                        Log.d(TAG, "Description: $description")
                        Log.d(TAG, "Metadata: ${metadata.toString()}")
                        items.add(item)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document", e)
                    }
                }
                
                // Filter by category if not ALL
                val filteredItems = if (category == WardrobeCategory.ALL) {
                    items
                } else {
                    items.filter { item ->
                        val matches = item.category == category
                        Log.d(TAG, "Item ${item.id}: category=${item.category.name}, requested=${category.name}, matches=$matches")
                        matches
                    }
                }
                
                // Sort by newest first
                val sortedItems = filteredItems.sortedByDescending { it.uploadTimestamp }
                Log.d(TAG, "Final filtered items list size: ${sortedItems.size} (from ${items.size} total)")
                sortedItems.forEachIndexed { index, item -> 
                    Log.d(TAG, "Filtered Item $index: ${item.id} - category: ${item.category.name}")
                }
                onSuccess(sortedItems)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents", exception)
                onFailure(exception)
            }
    }
} 