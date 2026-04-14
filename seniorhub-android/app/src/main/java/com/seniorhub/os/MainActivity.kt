package com.seniorhub.os

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.seniorhub.os.data.DeviceIdentityStore
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.ui.HomeRoute
import com.seniorhub.os.ui.HomeViewModel
import com.seniorhub.os.ui.theme.SeniorHubTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0,
                )
            }
        }
        lifecycleScope.launch {
            val deviceId = DeviceIdentityStore(applicationContext).getOrCreateDeviceId()
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val repository = MvpRepository(
                db = db,
                auth = auth,
                deviceId = deviceId,
            )
            (application as SeniorHubApp).setMessagingDeps(db, auth, deviceId)
            repository.bootstrapDevice()
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                repository.registerFcmToken(token)
            }
            setContent {
                SeniorHubTheme {
                    val viewModel: HomeViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                                    return HomeViewModel(
                                        application = application as Application,
                                        repository = repository,
                                    ) as T
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
