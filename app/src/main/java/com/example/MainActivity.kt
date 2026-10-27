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
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.SultanBlockTheme
import com.example.ui.theme.SultanDarkBg

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val networkInfoManager = NetworkInfoManager(applicationContext)
                val settingsRepo = SettingsRepository(applicationContext)
                return MainViewModel(networkInfoManager, settingsRepo) as T
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
    }
}
