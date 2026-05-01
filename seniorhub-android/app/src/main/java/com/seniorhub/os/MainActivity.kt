package com.seniorhub.os

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.role.RoleManager
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.seniorhub.os.data.AdminRepository
import com.seniorhub.os.data.AppRole
import com.seniorhub.os.data.AppRoleStore
import com.seniorhub.os.data.DeviceIdentityStore
import com.seniorhub.os.data.MvpRepository
import com.seniorhub.os.ui.AdminRoute
import com.seniorhub.os.ui.AdminViewModel
import com.seniorhub.os.ui.HomeRoute
import com.seniorhub.os.ui.HomeViewModel
import com.seniorhub.os.ui.RolePickerScreen
import com.seniorhub.os.ui.theme.SeniorHubTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private var seniorFullscreen = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appRoleStore = AppRoleStore(applicationContext)
        val role = runBlocking { appRoleStore.getRoleOrNull() }
        requestedOrientation = when (role) {
            AppRole.Admin -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            AppRole.Senior -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            null -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        seniorFullscreen = role == AppRole.Senior
        if (seniorFullscreen) {
            enterSeniorFullscreen()
        }

        if (role == null || role == AppRole.Senior || role == AppRole.Admin) {
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
        }

        if (role == AppRole.Senior) {
            val commNeeded = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_CALL_LOG,
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (commNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    commNeeded.toTypedArray(),
                    1,
                )
            }
            requestDialerRoleIfNeeded()
        }

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        if (role == AppRole.Senior) {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Kiosk: neukončovat aplikaci tlačítkem Zpět.
                    }
                },
            )
        }

        when (role) {
            null -> {
                setContent {
                    SeniorHubTheme {
                        RolePickerScreen(
                            onChooseSenior = {
                                lifecycleScope.launch {
                                    appRoleStore.setRole(AppRole.Senior)
                                    recreate()
                                }
                            },
                            onChooseAdmin = {
                                lifecycleScope.launch {
                                    appRoleStore.setRole(AppRole.Admin)
                                    recreate()
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            AppRole.Admin -> {
                val adminRepository = AdminRepository(db, auth)
                setContent {
                    SeniorHubTheme {
                        val adminViewModel: AdminViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                                        return AdminViewModel(
                                            application = application as Application,
                                            auth = auth,
                                            repository = adminRepository,
                                        ) as T
                                    }
                                    throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                                }
                            },
                        )
                        AdminRoute(
                            viewModel = adminViewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            AppRole.Senior -> {
                lifecycleScope.launch {
                    val deviceId = DeviceIdentityStore(applicationContext).getOrCreateDeviceId()
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
    }

    override fun onResume() {
        super.onResume()
        if (seniorFullscreen) {
            enterSeniorFullscreen()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && seniorFullscreen) {
            enterSeniorFullscreen()
        }
    }

    companion object {
        const val EXTRA_FROM_MESSAGE_NOTIFICATION = "extra_from_message_notification"
    }

    private fun requestDialerRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) ?: return
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return
            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return
            startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else {
            val telecom = getSystemService(TelecomManager::class.java) ?: return
            if (telecom.defaultDialerPackage == packageName) return
            @Suppress("DEPRECATION")
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName,
                ),
            )
        }
    }

    private fun enterSeniorFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
