package com.TOTOMOFYP.VTOAPP.ui.wardrobe

/**
 * Enum representing categories of clothing items in the wardrobe
 */
enum class WardrobeCategory(val displayName: String) {
    ALL("All Items"),
    TOP("Tops"),
    BOTTOM("Bottoms"),
    FULL_BODY("Full Body"),
//    FOOTWEAR("Footwear"),
//    ACCESSORIES("Accessories"),
//    OUTERWEAR("Outerwear"),
    OTHER("Other");

    companion object {
        /**
         * Get a WardrobeCategory from its name, returning OTHER if not found
         */
        fun fromString(name: String?): WardrobeCategory {
            return try {
                if (name != null) valueOf(name) else OTHER
            } catch (e: IllegalArgumentException) {
    OTHER
            }
        }
    }
} 