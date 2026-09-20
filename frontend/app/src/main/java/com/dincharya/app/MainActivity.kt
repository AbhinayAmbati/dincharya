package com.dincharya.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dincharya.app.app.Graph
import com.dincharya.app.ui.navigation.DincharyaApp

/**
 * The single activity of the app — everything else is Compose navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind system bars; Compose handles insets.
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            // themeModeState is a Compose state: changing the setting in
            // Settings re-themes the whole app instantly.
            val themeMode = Graph.settings.themeModeState
            DincharyaApp(themeMode = themeMode)
        }
    }

    /**
     * Android 13+ requires a runtime permission for notifications.
     * We ask once at launch — nagging on every start would be ironic for
     * an app that promises fewer, better-timed reminders.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS,
                )
            }
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 100
    }
}
