package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseChatRepository
import com.example.data.local.dao.UserProfileDao
import com.example.data.signal.SignalProtocolManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    data class GeneratingKeys(val stageText: String) : OnboardingUiState()
    object Success : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val userProfileDao: UserProfileDao,
    private val signalProtocolManager: SignalProtocolManager,
    private val chatRepository: FirebaseChatRepository
) : ViewModel() {

    private val validInviteCodes = setOf(
        "CIPHER2026",
        "SECRET77",
        "FRIENDS101",
        "ALPHA88",
        "OMEGA99",
        "SAFECHAT"
    )

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun completeOnboarding(name: String, inviteCode: String) {
        val trimmedName = name.trim()
        val trimmedCode = inviteCode.trim().uppercase()

        if (trimmedName.isBlank()) {
            _uiState.value = OnboardingUiState.Error("Please enter your name or nickname.")
            return
        }

        if (trimmedCode !in validInviteCodes) {
            _uiState.value = OnboardingUiState.Error(
                "Invalid invite code. Valid circle codes include CIPHER2026, SECRET77, FRIENDS101."
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.GeneratingKeys("Generating Signal cryptographic keypairs...")
                val userId = "user_${UUID.randomUUID().toString().take(8)}"

                val bundle = signalProtocolManager.createIdentity(
                    userId = userId,
                    displayName = trimmedName,
                    inviteCode = trimmedCode
                )

                _uiState.value = OnboardingUiState.GeneratingKeys("Registering device on secure network...")
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    null
                }

                try {
                    chatRepository.publishUserIdentity(bundle, fcmToken)
                    chatRepository.startListening(userId)
                    chatRepository.updatePresence(true)
                } catch (cloudErr: Exception) {
                    android.util.Log.w("Onboarding", "Cloud registration deferred until Firebase connection: ${cloudErr.message}")
                }

                _uiState.value = OnboardingUiState.Success
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(
                    "Setup failed: ${e.localizedMessage ?: "Cryptographic initialization error"}"
                )
            }
        }
    }

    class Factory(
        private val userProfileDao: UserProfileDao,
        private val signalProtocolManager: SignalProtocolManager,
        private val chatRepository: FirebaseChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userProfileDao, signalProtocolManager, chatRepository) as T
        }
    }
}
