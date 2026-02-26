package com.devlosoft.megaposmobile

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.devlosoft.megaposmobile.core.dataphone.DataphoneManager
import com.devlosoft.megaposmobile.core.session.InactivityManager
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.usecase.LogoutUseCase
import com.devlosoft.megaposmobile.presentation.navigation.Login
import com.devlosoft.megaposmobile.presentation.navigation.NavGraph
import com.devlosoft.megaposmobile.ui.theme.MegaPosMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inactivityManager: InactivityManager

    @Inject
    lateinit var logoutUseCase: LogoutUseCase

    @Inject
    lateinit var stationStatus: StationStatus

    @Inject
    lateinit var serverConfigDao: ServerConfigDao

    @Inject
    lateinit var dataphoneManager: DataphoneManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataphoneManager.setActivity(this)
        enableEdgeToEdge()
        setContent {
            MegaPosMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val lifecycleOwner = LocalLifecycleOwner.current

                    // Observe lifecycle for pause/resume to handle background timeout
                    LaunchedEffect(lifecycleOwner) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            // This block runs when STARTED, stops when below STARTED
                        }
                    }

                    // Handle ON_PAUSE and ON_RESUME for background timeout
                    LaunchedEffect(lifecycleOwner) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            // App is in foreground
                            inactivityManager.onAppForeground()
                        }
                    }

                    // Observe logoutEvent for automatic logout
                    LaunchedEffect(Unit) {
                        inactivityManager.logoutEvent.collectLatest {
                            // Execute automatic logout
                            stationStatus.close()
                            logoutUseCase().collect { }
                            navController.navigate(Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    NavGraph(
                        navController = navController,
                        inactivityManager = inactivityManager,
                        serverConfigDao = serverConfigDao
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataphoneManager.setActivity(this)
    }

    override fun onPause() {
        super.onPause()
        inactivityManager.onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataphoneManager.clearActivity()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dataphoneManager.handleActivityResult(requestCode, resultCode, data)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            inactivityManager.resetTimer()
        }
        return super.dispatchTouchEvent(ev)
    }
}
