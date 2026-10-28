package com.example.pin

import android.content.Context
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

data class PinProtectionConfig(
    val protectSettings: Boolean = true,
    val protectInternetBlock: Boolean = false,
    val protectVolumeLock: Boolean = false,
    val protectSchedule: Boolean = false,
    val protectTimer: Boolean = false,
    val protectCertainLock: Boolean = false
)

class PinManager(private val settingsRepository: SettingsRepository) {

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _config = MutableStateFlow(PinProtectionConfig())
    val config: StateFlow<PinProtectionConfig> = _config.asStateFlow()

    suspend fun initialize() {
        val hash = settingsRepository.pinHash.first()
        _isPinSet.value = !hash.isNullOrEmpty()
        loadConfig()
    }

    private suspend fun loadConfig() {
        _config.value = PinProtectionConfig(
            protectSettings = settingsRepository.protectSettings.first(),
            protectInternetBlock = settingsRepository.protectInternetBlock.first(),
            protectVolumeLock = settingsRepository.protectVolumeLock.first(),
            protectSchedule = settingsRepository.protectSchedule.first(),
            protectTimer = settingsRepository.protectTimer.first(),
            protectCertainLock = settingsRepository.protectCertainLock.first()
        )
    }

    suspend fun setPin(pin: String, recoveryAnswer: String): String {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        val recoverySalt = generateSalt()
        val recoveryHash = hashPin(recoveryAnswer.trim().lowercase(Locale.ROOT), recoverySalt)

        settingsRepository.savePin(hash = hash, salt = salt, recoveryHash = recoveryHash, recoverySalt = recoverySalt)
        _isPinSet.value = true
        return recoveryAnswer
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val storedHash = settingsRepository.pinHash.first() ?: return false
        val storedSalt = settingsRepository.pinSalt.first() ?: return false
        val computedHash = hashPin(enteredPin, storedSalt)
        return computedHash == storedHash
    }

    suspend fun verifyRecoveryAnswer(enteredAnswer: String): Boolean {
        val storedRecoveryHash = settingsRepository.recoveryHash.first() ?: return false
        val storedRecoverySalt = settingsRepository.recoverySalt.first() ?: return false
        val computedHash = hashPin(enteredAnswer.trim().lowercase(Locale.ROOT), storedRecoverySalt)
        return computedHash == storedRecoveryHash
    }

    suspend fun resetPin() {
        settingsRepository.clearPin()
        _isPinSet.value = false
    }

    suspend fun updateConfig(newConfig: PinProtectionConfig) {
        _config.value = newConfig
        settingsRepository.savePinConfig(newConfig)
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt$pin".toByteArray(Charsets.UTF_8)
        val digest = md.digest(combined)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
