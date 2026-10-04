package com.example

import org.junit.Assert.*
import org.junit.Test
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore
import org.whispersystems.libsignal.util.KeyHelper

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testEncryptWithoutSessionThrowsEmptyKey() {
    val identityKeyPair = KeyHelper.generateIdentityKeyPair()
    val regId = KeyHelper.generateRegistrationId(false)
    val store = InMemorySignalProtocolStore(identityKeyPair, regId)
    val cipher = SessionCipher(store, SignalProtocolAddress("bob", 1))
    cipher.encrypt("hello".toByteArray())
  }
}
