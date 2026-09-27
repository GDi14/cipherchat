package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.AttachmentCrypto
import com.example.model.DecryptedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CipherChat", appName)
  }

  @Test
  fun `attachment crypto encryption and decryption roundtrip`() {
    val originalData = "Super secret friend circle test payload 12345".toByteArray(Charsets.UTF_8)
    val encrypted = AttachmentCrypto.encryptBytes(originalData)
    val decrypted = AttachmentCrypto.decryptBytes(encrypted.encryptedBytes, encrypted.base64Key, encrypted.base64Iv)
    assertEquals(String(originalData, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
  }

  @Test
  fun `decrypted content model serialization and deserialization`() {
    val textContent = DecryptedContent.Text("Hello friend!")
    val json = textContent.toJsonString()
    val restored = DecryptedContent.fromJsonString(json)
    assertTrue(restored is DecryptedContent.Text)
    assertEquals("Hello friend!", (restored as DecryptedContent.Text).text)
  }
}

