package com.TOTOMOFYP.VTOAPP.ui.makeup

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.Date

class MakeupStorage {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "MakeupStorage"
        private const val USERS_COLLECTION = "users"
        private const val MAKEUP_SUBCOLLECTION = "makeup_looks"
    }
    
    suspend fun saveMakeupLook(name: String, image: Bitmap): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val lookId = UUID.randomUUID().toString()
            
            // Upload image to Firebase Storage
            val imageUrl = uploadImageToStorage(image, lookId)
            
            // Create makeup look object
            val makeupLook = MakeupLook(
                id = lookId,
                userId = userId,
                name = name,
                imageUrl = imageUrl,
                dateCreated = Date(),
                isPublic = false
            )
            
            // Save to Firestore using user subcollection structure: /users/{userId}/makeup_looks/{lookId}
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MAKEUP_SUBCOLLECTION)
                .document(lookId)
                .set(makeupLook)
                .await()
            
            Log.d(TAG, "Makeup look saved successfully: $lookId")
            Result.success(lookId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save makeup look", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadImageToStorage(image: Bitmap, lookId: String): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val imageBytes = baos.toByteArray()
        
        // Use the specified path structure: /users/{userId}/makeup/
        val storageRef = storage.reference
            .child("users")
            .child(userId)
            .child("makeup")
            .child("$lookId.jpg")
        
        storageRef.putBytes(imageBytes).await()
        return storageRef.downloadUrl.await().toString()
    }
    
    suspend fun getUserMakeupLooks(): Result<List<MakeupLook>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Query from user's subcollection: /users/{userId}/makeup_looks/
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MAKEUP_SUBCOLLECTION)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val makeupLooks = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(MakeupLook::class.java)
            }
            
            Log.d(TAG, "Retrieved ${makeupLooks.size} makeup looks for user: $userId")
            Result.success(makeupLooks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get makeup looks", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteMakeupLook(lookId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Get the look from user's subcollection: /users/{userId}/makeup_looks/{lookId}
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MAKEUP_SUBCOLLECTION)
                .document(lookId)
                .get()
                .await()
            
            val look = doc.toObject(MakeupLook::class.java)
            if (look == null) {
                return Result.failure(Exception("Makeup look not found"))
            }
            
            // Delete from Storage (using the new path structure)
            if (look.imageUrl.isNotEmpty()) {
                try {
                    storage.getReferenceFromUrl(look.imageUrl).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete image from storage: ${e.message}")
                    // Continue with Firestore deletion even if storage deletion fails
                }
            }
            
            // Delete from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MAKEUP_SUBCOLLECTION)
                .document(lookId)
                .delete()
                .await()
            
            Log.d(TAG, "Makeup look deleted successfully: $lookId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete makeup look", e)
            Result.failure(e)
        }
    }
}