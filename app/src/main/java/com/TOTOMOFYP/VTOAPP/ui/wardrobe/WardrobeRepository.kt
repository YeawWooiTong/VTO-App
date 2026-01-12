package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing clothing items in the wardrobe
 */
interface WardrobeRepository {
    /**
     * Get all clothing items for the current user as a Flow
     * 
     * @param category Optional category filter
     * @return Flow of clothing items
     */
    fun getClothingItems(category: WardrobeCategory = WardrobeCategory.ALL): Flow<List<ClothingItem>>
    
    /**
     * Add a new clothing item to the wardrobe
     * 
     * @param clothingItem The clothing item to add
     * @return Result containing the ID of the added item or an exception
     */
    suspend fun addClothingItem(clothingItem: ClothingItem): Result<String>
    
    /**
     * Delete a clothing item from the wardrobe
     * 
     * @param itemId ID of the item to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteClothingItem(itemId: String): Result<Unit>
    
    /**
     * Get a specific clothing item by ID
     * 
     * @param itemId ID of the item to retrieve
     * @return The clothing item or null if not found
     */
    suspend fun getClothingItemById(itemId: String): ClothingItem?
    
    /**
     * Update an existing clothing item
     * 
     * @param clothingItem The updated clothing item
     * @return Result indicating success or failure
     */
    suspend fun updateClothingItem(clothingItem: ClothingItem): Result<Unit>
} 