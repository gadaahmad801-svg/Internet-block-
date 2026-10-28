package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.VolumeStateManager
import com.example.certainlock.CertainLockManager
import com.example.data.ProtectionPreferencesRepository
import com.example.schedule.ScheduleItem
import com.example.timer.TimerState
import com.example.vpn.VpnStateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Certain Ahmad", appName)
    }

    @Test
    fun testVpnStateManager() {
        VpnStateManager.updateActive(false)
        assertFalse(VpnStateManager.isVpnActive.value)

        VpnStateManager.updateActive(true)
        assertTrue(VpnStateManager.isVpnActive.value)

        VpnStateManager.resetCounters()
        assertEquals(0L, VpnStateManager.blockedPackets.value)

        VpnStateManager.incrementBlocked(5, 1024)
        assertEquals(5L, VpnStateManager.blockedPackets.value)
        assertEquals(1024L, VpnStateManager.blockedBytes.value)

        VpnStateManager.updateActive(false)
        assertFalse(VpnStateManager.isVpnActive.value)
    }

    @Test
    fun testVolumeStateManager() {
        VolumeStateManager.updateLockActive(false)
        assertFalse(VolumeStateManager.isLockActive.value)

        VolumeStateManager.updateLockActive(true)
        assertTrue(VolumeStateManager.isLockActive.value)

        VolumeStateManager.resetSuppression()
        assertEquals(0L, VolumeStateManager.suppressionCount.value)

        VolumeStateManager.incrementSuppression()
        assertEquals(1L, VolumeStateManager.suppressionCount.value)
    }

    @Test
    fun testCertainLockManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CertainLockManager.updateActiveState(false)
        assertFalse(CertainLockManager.isLockActive.value)

        CertainLockManager.setLockedPackages(setOf("com.instagram.android", "com.openai.chatgpt"))
        assertTrue(CertainLockManager.lockedPackages.value.contains("com.instagram.android"))

        // When inactive, should not redirect
        assertFalse(CertainLockManager.shouldRedirect("com.instagram.android", context))

        // When active, should redirect locked package
        CertainLockManager.updateActiveState(true)
        assertTrue(CertainLockManager.shouldRedirect("com.instagram.android", context))

        // Should not redirect unlocked package
        assertFalse(CertainLockManager.shouldRedirect("com.android.chrome", context))
    }

    @Test
    fun testScheduleItemFormatting() {
        val schedule = ScheduleItem(
            title = "Sleep Focus",
            startHour = 23,
            startMinute = 30,
            endHour = 7,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        )
        assertEquals("23:30", schedule.formatStartTime())
        assertEquals("07:00", schedule.formatEndTime())
        assertEquals("Weekdays", schedule.formatDays())
    }

    @Test
    fun testTimerState() {
        val now = System.currentTimeMillis()
        val timer = TimerState(
            isActive = true,
            durationMinutes = 60,
            startTimestamp = now,
            endTimestamp = now + (60 * 60 * 1000)
        )
        assertTrue(timer.remainingMillis() > 0)
        assertTrue(timer.formatRemaining().contains(":"))
    }

    @Test
    fun testProtectionPreferencesRepository() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProtectionPreferencesRepository(context)

        repo.setInternetBlockEnabled(true)
        assertTrue(repo.isInternetBlockEnabled.first())

        repo.setVolumeLockEnabled(true)
        assertTrue(repo.isVolumeLockEnabled.first())

        repo.setAppLockEnabled(true)
        assertTrue(repo.isAppLockEnabled.first())

        val pkgs = setOf("com.facebook.katana", "com.twitter.android")
        repo.setLockedAppPackages(pkgs)
        assertEquals(pkgs, repo.lockedAppPackages.first())

        repo.setLockMediaOnly(true)
        assertTrue(repo.isLockMediaOnly.first())

        val states = repo.toggleStates.first()
        assertTrue(states.isInternetBlockEnabled)
        assertTrue(states.isVolumeLockEnabled)
        assertTrue(states.isAppLockEnabled)
        assertTrue(states.isLockMediaOnly)
        assertEquals(pkgs, states.lockedAppPackages)

        repo.clearAllToggles()
        val clearedStates = repo.toggleStates.first()
        assertFalse(clearedStates.isInternetBlockEnabled)
        assertFalse(clearedStates.isVolumeLockEnabled)
        assertFalse(clearedStates.isAppLockEnabled)
    }
}
