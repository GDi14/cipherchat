package com.example.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val userId: String = "",
    val tag: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val avatarShape: String = "",
    val chatFont: String = "",
    val fontAxes: String = "",
    val bubbleColor: String = ""
)

class FirestoreChatRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun isTagAvailable(tag: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = tag.trim().removePrefix("@").lowercase()
            if (cleanTag.isBlank()) return@withContext Result.success(false)

            val doc = firestore.collection("tags").document(cleanTag).get().await()
            if (!doc.exists()) {
                Result.success(true)
            } else {
                val ownerId = doc.getString("userId")
                val currentUid = auth.currentUser?.uid
                Result.success(ownerId == null || ownerId == currentUid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(
        tag: String,
        displayName: String,
        photoUrl: String,
        avatarShape: String,
        chatFont: String,
        fontAxes: String,
        bubbleColor: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = requireUserId()
            val cleanTag = tag.trim().removePrefix("@").lowercase()

            val userData = mapOf(
                "userId" to uid,
                "tag" to cleanTag,
                "displayName" to displayName,
                "photoUrl" to photoUrl,
                "avatarShape" to avatarShape,
                "chatFont" to chatFont,
                "fontAxes" to fontAxes,
                "bubbleColor" to bubbleColor,
                "updatedAt" to System.currentTimeMillis()
            )

            val tagData = mapOf(
                "tag" to cleanTag,
                "userId" to uid
            )

            val batch = firestore.batch()
            val userRef = firestore.collection("users").document(uid)
            val tagRef = firestore.collection("tags").document(cleanTag)

            batch.set(userRef, userData)
            batch.set(tagRef, tagData)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val profile = UserProfile(
                    userId = doc.getString("userId") ?: uid,
                    tag = doc.getString("tag") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    avatarShape = doc.getString("avatarShape") ?: "",
                    chatFont = doc.getString("chatFont") ?: "",
                    fontAxes = doc.getString("fontAxes") ?: "",
                    bubbleColor = doc.getString("bubbleColor") ?: ""
                )
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
