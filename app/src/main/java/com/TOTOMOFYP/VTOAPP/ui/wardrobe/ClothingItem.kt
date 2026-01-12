package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import java.util.Date
import org.json.JSONObject

/**
 * Data class representing a clothing item in the wardrobe
 * Updated to match server format with AI analysis results
 */
data class ClothingItem(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "", // image_url from server
    val imageName: String = "", // image_name from server
    val category: WardrobeCategory = WardrobeCategory.OTHER,
    val color: String = "",
    val description: String = "", // From AI analysis
    val pattern: String = "", // From AI analysis
    val style: String = "", // From AI analysis
    val occasion: String = "", // From AI analysis
    val timestamp: Date = Date(),
    val displayName: String = "" // Smart generated name like "Casual 1"
) {
    /**
     * Convert the ClothingItem to a Map for Firestore storage
     * (Legacy format - server now handles storage)
     */
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "image_url" to imageUrl,
        "image_name" to imageName,
        "metadata" to mapOf(
            "category" to category.name.lowercase(),
            "color" to color,
            "description" to description,
            "pattern" to pattern,
            "style" to style,
            "occasion" to occasion
        ),
        "timestamp" to timestamp,
        "display_name" to displayName
    )

    companion object {
        /**
         * Create a ClothingItem from a Firestore document map (server format)
         */
        fun fromMap(map: Map<String, Any>): ClothingItem {
            // Handle metadata - can be Map or JSON string
            val metadata = when (val metaData = map["metadata"]) {
                is Map<*, *> -> metaData as Map<String, Any>
                is String -> {
                    try {
                        org.json.JSONObject(metaData).let { json ->
                            mapOf(
                                "category" to json.optString("category", "other"),
                                "color" to json.optString("color", ""),
                                "description" to json.optString("description", ""),
                                "pattern" to json.optString("pattern", ""),
                                "style" to json.optString("style", ""),
                                "occasion" to json.optString("occasion", "")
                            )
                        }
                    } catch (e: Exception) {
                        emptyMap()
                    }
                }
                else -> emptyMap()
            }
            
            // Parse timestamp - can be Date, Timestamp, or ISO string
            val timestamp = when (val ts = map["timestamp"]) {
                is Date -> ts
                is com.google.firebase.Timestamp -> ts.toDate()
                is String -> {
                    try {
                        Date.from(java.time.Instant.parse(ts))
                    } catch (e: Exception) {
                        Date()
                    }
                }
                else -> Date()
            }
            
            // Parse category
            val categoryName = metadata["category"] as? String ?: "other"
            val category = when (categoryName.lowercase()) {
                "all" -> WardrobeCategory.ALL
                "top", "upper" -> WardrobeCategory.TOP
                "bottom" -> WardrobeCategory.BOTTOM
                "full_body" -> WardrobeCategory.FULL_BODY
                else -> WardrobeCategory.OTHER
            }
            
            return ClothingItem(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                imageUrl = map["image_url"] as? String ?: (map["imageUrl"] as? String ?: ""),
                imageName = map["image_name"] as? String ?: "",
                category = category,
                color = metadata["color"] as? String ?: "",
                description = metadata["description"] as? String ?: "",
                pattern = metadata["pattern"] as? String ?: "",
                style = metadata["style"] as? String ?: "",
                occasion = metadata["occasion"] as? String ?: "",
                timestamp = timestamp,
                displayName = map["display_name"] as? String ?: ""
            )
        }
    }
} 