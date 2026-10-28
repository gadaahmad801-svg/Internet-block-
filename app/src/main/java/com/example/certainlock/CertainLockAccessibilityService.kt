package com.example.certainlock

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class CertainLockAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "CertainLockA11y"
        var isServiceConnected: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        Log.i(TAG, "CertainLockAccessibilityService connected and monitoring window states.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (CertainLockManager.shouldRedirect(packageName, this)) {
                // Instantly navigate back to the real Android Launcher Home Screen
                val success = performGlobalAction(GLOBAL_ACTION_HOME)
                if (!success) {
                    // Fallback to home intent if global action is delayed by OEM
                    CertainLockManager.returnToHomeViaIntent(this)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "CertainLockAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        isServiceConnected = false
        super.onDestroy()
    }
}
