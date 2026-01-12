package com.TOTOMOFYP.VTOAPP.ui.wardrobe

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of WardrobeRepository
 */
@Singleton
class FirestoreWardrobeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : WardrobeRepository {

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_WARDROBE_ITEMS = "wardrobeItems"
        private const val COLLECTION_OUTFITS = "outfits"
        private const val COLLECTION_FAVORITES = "favorites"
    }

    /**
     * Gets the current user ID or throws an exception if not logged in
     */
    val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    /**
     * Gets the reference to the current user's wardrobe items collection
     */
    private val userWardrobeRef
        get() = firestore.collection(COLLECTION_USERS)
            .document(currentUserId)
            .collection(COLLECTION_WARDROBE_ITEMS)

    override fun getClothingItems(category: WardrobeCategory): Flow<List<ClothingItem>> = callbackFlow {
        // Always get all items and filter in app to avoid Firestore indexing issues
        val query = userWardrobeRef
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val allItems = snapshot?.documents?.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    val item = ClothingItem.fromMap(data)
                    item.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            // Filter by category if not ALL
            val filteredItems = if (category == WardrobeCategory.ALL) {
                allItems
            } else {
                allItems.filter { it.category == category }
            }

            trySend(filteredItems)
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun addClothingItem(clothingItem: ClothingItem): Result<String> = try {
        val docRef = userWardrobeRef
            .add(clothingItem.toMap())
            .await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteClothingItem(itemId: String): Result<Unit> = try {
        userWardrobeRef
            .document(itemId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getClothingItemById(itemId: String): ClothingItem? = try {
        val doc = userWardrobeRef
            .document(itemId)
            .get()
            .await()
        
        if (doc.exists() && doc.data != null) {
            val item = ClothingItem.fromMap(doc.data!!)
            item.copy(id = doc.id)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    override suspend fun updateClothingItem(clothingItem: ClothingItem): Result<Unit> = try {
        userWardrobeRef
            .document(clothingItem.id)
            .update(clothingItem.toMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateClothingItem(
        itemId: String,
        displayName: String,
        description: String,
        color: String,
        pattern: String,
        style: String,
        occasion: String
    ): Unit {
        userWardrobeRef
            .document(itemId)
            .update(
                mapOf(
                    "display_name" to displayName,
                    "metadata.description" to description,
                    "metadata.color" to color,
                    "metadata.pattern" to pattern,
                    "metadata.style" to style,
                    "metadata.occasion" to occasion
                )
            )
            .await()
    }

    suspend fun updateClothingItemDisplayName(itemId: String, displayName: String): Unit {
        try {
            // Use set with merge to create the field if it doesn't exist
            userWardrobeRef
                .document(itemId)
                .set(mapOf("display_name" to displayName), com.google.firebase.firestore.SetOptions.merge())
                .await()
            android.util.Log.d("FirestoreRepository", "Successfully updated display_name to '$displayName' for item $itemId")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to update display_name for item $itemId", e)
            throw e
        }
    }
} 