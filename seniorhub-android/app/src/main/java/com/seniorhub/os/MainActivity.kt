package com.seniorhub.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.os.data.DeviceIdentityStore
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.ui.HomeRoute
import com.seniorhub.os.ui.HomeViewModel
import com.seniorhub.os.ui.theme.SeniorHubTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val deviceId = DeviceIdentityStore(applicationContext).getOrCreateDeviceId()
            val repository = MvpRepository(
                db = FirebaseFirestore.getInstance(),
                auth = FirebaseAuth.getInstance(),
                deviceId = deviceId,
            )
            setContent {
                SeniorHubTheme {
                    val viewModel: HomeViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                                    return HomeViewModel(repository) as T
                                }
                                throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                            }
                        },
                    )
                    HomeRoute(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
