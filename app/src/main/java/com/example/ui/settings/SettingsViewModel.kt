package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.cloud.CloudinaryStorageManager
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.LocalUserProfileEntity
import com.example.data.updates.GithubRelease
import com.example.data.updates.UpdateCheckState
import com.example.data.updates.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userProfileDao: UserProfileDao,
    private val updateManager: UpdateManager
) : ViewModel() {

    val userProfile: StateFlow<LocalUserProfileEntity?> = userProfileDao.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val updateState: StateFlow<UpdateCheckState> = updateManager.updateState

    private val _repoInput = MutableStateFlow(updateManager.getSavedRepository())
    val repoInput: StateFlow<String> = _repoInput.asStateFlow()

    private val _needsInstallPermission = MutableStateFlow(false)
    val needsInstallPermission: StateFlow<Boolean> = _needsInstallPermission.asStateFlow()

    // Cloudinary Inputs
    private val _cloudName = MutableStateFlow("")
    val cloudName: StateFlow<String> = _cloudName.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _apiSecret = MutableStateFlow("")
    val apiSecret: StateFlow<String> = _apiSecret.asStateFlow()

    fun loadCloudinaryCredentials(context: Context) {
        val manager = CloudinaryStorageManager(context)
        _cloudName.value = manager.cloudName
        _apiKey.value = manager.apiKey
        _apiSecret.value = manager.apiSecret
    }

    fun saveCloudinaryCredentials(context: Context, name: String, key: String, secret: String) {
        val manager = CloudinaryStorageManager(context)
        manager.setCredentials(name, key, secret)
        _cloudName.value = name
        _apiKey.value = key
        _apiSecret.value = secret
    }

    fun updateRepoInput(newRepo: String) {
        _repoInput.value = newRepo
        updateManager.saveRepository(newRepo)
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(_repoInput.value)
        }
    }

    fun downloadAndInstall(release: GithubRelease) {
        if (!updateManager.hasInstallPermission()) {
            _needsInstallPermission.value = true
            return
        }
        viewModelScope.launch {
            updateManager.downloadAndInstall(release)
        }
    }

    fun dismissInstallPermissionDialog() {
        _needsInstallPermission.value = false
    }

    fun getAppVersion(): String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    class Factory(
        private val userProfileDao: UserProfileDao,
        private val updateManager: UpdateManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userProfileDao, updateManager) as T
        }
    }
}
