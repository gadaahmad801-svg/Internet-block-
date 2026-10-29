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
    val protectAppLockSettings: Boolean = true,
    val protectSelectedApps: Boolean = true,
    val protectInternetBlock: Boolean = false,
    val protectVolumeLock: Boolean = false,
    val protectTimer: Boolean = false,
    val protectSchedule: Boolean = false,
    val protectSecuritySettings: Boolean = true
)

class PinManager(private val settingsRepository: SettingsRepository) {

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _config = MutableStateFlow(PinProtectionConfig())
    val config: StateFlow<PinProtectionConfig> = _config.asStateFlow()

    // Session authentication to prevent annoying re-prompts on every navigation
    private var sessionAuthenticated = false

    fun isSessionAuthenticated(): Boolean = sessionAuthenticated

    fun setSessionAuthenticated(authenticated: Boolean) {
        sessionAuthenticated = authenticated
    }

    suspend fun initialize() {
        val hash = settingsRepository.pinHash.first()
        _isPinSet.value = !hash.isNullOrEmpty()
        loadConfig()
    }

    private suspend fun loadConfig() {
        _config.value = PinProtectionConfig(
            protectAppLockSettings = settingsRepository.protectAppLockSettings.first(),
            protectSelectedApps = settingsRepository.protectSelectedApps.first(),
            protectInternetBlock = settingsRepository.protectInternetBlock.first(),
            protectVolumeLock = settingsRepository.protectVolumeLock.first(),
            protectTimer = settingsRepository.protectTimer.first(),
            protectSchedule = settingsRepository.protectSchedule.first(),
            protectSecuritySettings = settingsRepository.protectSecuritySettings.first()
        )
    }

    suspend fun setPin(sixDigitPin: String, recoveryAnswer: String): Boolean {
        if (sixDigitPin.length != 6 || !sixDigitPin.all { it.isDigit() }) {
            return false
        }
        val salt = generateSalt()
        val hash = hashPin(sixDigitPin, salt)
        val recoverySalt = generateSalt()
        val recoveryHash = hashPin(recoveryAnswer.trim().lowercase(Locale.ROOT), recoverySalt)

        settingsRepository.savePin(hash = hash, salt = salt, recoveryHash = recoveryHash, recoverySalt = recoverySalt)
        _isPinSet.value = true
        sessionAuthenticated = true
        return true
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        if (enteredPin.length != 6 || !enteredPin.all { it.isDigit() }) return false
        val storedHash = settingsRepository.pinHash.first() ?: return false
        val storedSalt = settingsRepository.pinSalt.first() ?: return false
        val computedHash = hashPin(enteredPin, storedSalt)
        val matches = computedHash == storedHash
        if (matches) {
            sessionAuthenticated = true
        }
        return matches
    }

    suspend fun verifyRecoveryAnswer(enteredAnswer: String): Boolean {
        val storedRecoveryHash = settingsRepository.recoveryHash.first() ?: return false
        val storedRecoverySalt = settingsRepository.recoverySalt.first() ?: return false
        val computedHash = hashPin(enteredAnswer.trim().lowercase(Locale.ROOT), storedRecoverySalt)
        val matches = computedHash == storedRecoveryHash
        if (matches) {
            sessionAuthenticated = true
        }
        return matches
    }

    suspend fun resetPin() {
        settingsRepository.clearPin()
        _isPinSet.value = false
        sessionAuthenticated = false
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
