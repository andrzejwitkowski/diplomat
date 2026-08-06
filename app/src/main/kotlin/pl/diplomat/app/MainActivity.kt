package pl.diplomat.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import pl.diplomat.app.ui.theme.DiplomatTheme
import pl.diplomat.infrastructure.service.DiplomatForegroundService
import pl.diplomat.presentation.DiplomatApp

class MainActivity : ComponentActivity() {

    private val requestContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* picker will prompt again if denied */ }

    private val requestNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* foreground service still runs; alerts need this on API 33+ */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureContactsPermission()
        ensureNotificationsPermission()
        DiplomatForegroundService.startSafely(this)
        enableEdgeToEdge()

        val application = application as DiplomatApplication

        setContent {
            DiplomatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DiplomatApp(
                        dashboardViewModel = application.dashboardViewModel,
                        whitelistViewModel = application.whitelistViewModel,
                    )
                }
            }
        }
    }

    private fun ensureContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun ensureNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
