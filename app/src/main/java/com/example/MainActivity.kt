package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.SettingsRepository
import com.example.network.NetworkInfoManager
import com.example.pin.PinManager
import com.example.schedule.ScheduleManager
import com.example.timer.TimerManager
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.SultanBlockTheme
import com.example.ui.theme.SultanDarkBg

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = applicationContext
                val settingsRepo = SettingsRepository(context)
                val protectionRepo = com.example.data.ProtectionPreferencesRepository(context)
                val networkInfoManager = NetworkInfoManager(context)
                val scheduleManager = ScheduleManager(context, settingsRepo)
                val timerManager = TimerManager(context, settingsRepo)
                val pinManager = PinManager(settingsRepo)

                return MainViewModel(
                    networkInfoManager = networkInfoManager,
                    settingsRepository = settingsRepo,
                    protectionPreferencesRepository = protectionRepo,
                    scheduleManager = scheduleManager,
                    timerManager = timerManager,
                    pinManager = pinManager
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SultanBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SultanDarkBg
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAllPermissions(this)
        viewModel.loadApps(this)
    }
}
