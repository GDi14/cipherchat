package com.example.data.repository

import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.remote.FirestoreChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testTagAvailabilityAndProfileCreation() = runBlocking {
        val uid = signInTestUser("alice_rule@test.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = FirestoreChatRepository(context, firestore, auth)

        val testTag = "alice${System.currentTimeMillis() % 100000}"

        // Check unique tag
        val available = repo.isTagAvailable(testTag).getOrNull()
        assertTrue(available == true)

        // Save profile
        val saveResult = repo.saveUserProfile(
            tag = testTag,
            displayName = "Alice Rule",
            photoUrl = "",
            avatarShape = "flower",
            chatFont = "roboto_flex",
            fontAxes = "",
            bubbleColor = "#FF5C9D"
        )
        if (saveResult.isFailure) {
            println("SAVE ERROR: " + saveResult.exceptionOrNull())
            saveResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(saveResult.isSuccess)

        // Read back profile
        val profile = repo.getUserProfile(uid).getOrNull()
        assertNotNull(profile)
        assertTrue(profile?.tag == testTag)

        // Sign out -> ensure requireUserId fails
        auth.signOut()
        val unauthSave = repo.saveUserProfile(
            tag = "alice",
            displayName = "Fail",
            photoUrl = "",
            avatarShape = "flower",
            chatFont = "roboto_flex",
            fontAxes = "",
            bubbleColor = "#FF5C9D"
        )
        assertFalse(unauthSave.isSuccess)
    }
}
