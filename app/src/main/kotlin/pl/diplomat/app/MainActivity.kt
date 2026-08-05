package pl.diplomat.app

import android.Manifest
import android.content.pm.PackageManager
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
import pl.diplomat.presentation.whitelist.WhitelistRoute
import pl.diplomat.app.ui.theme.DiplomatTheme

class MainActivity : ComponentActivity() {

    private val requestContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* picker will prompt again if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureContactsPermission()
        enableEdgeToEdge()

        val viewModel = (application as DiplomatApplication).whitelistViewModel

        setContent {
            DiplomatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WhitelistRoute(viewModel = viewModel)
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
}
