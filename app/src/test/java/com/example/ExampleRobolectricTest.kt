package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.VolumeStateManager
import com.example.vpn.VpnStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SULTAN BLOCK", appName)
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
}
