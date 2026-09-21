package com.dincharya.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dincharya.app.app.Graph
import com.dincharya.app.app.SharedInbox
import com.dincharya.app.ui.navigation.DincharyaApp

/**
 * The single activity of the app — everything else is Compose navigation.
 *
 * It also fronts three entry points from OUTSIDE the app:
 *  - the launcher (normal),
 *  - the "Add task" app shortcut (EXTRA_OPEN),
 *  - text shared from any other app (ACTION_SEND) — the shared text seeds
 *    the title of a new task.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind system bars; Compose handles insets.
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        absorbIntent(intent)

        setContent {
            // themeModeState is a Compose state: changing the setting in
            // Settings re-themes the whole app instantly.
            val themeMode = Graph.settings.themeModeState
            DincharyaApp(themeMode = themeMode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Shares and shortcuts arriving while the app is already open.
        absorbIntent(intent)
    }

    /** Route outside-origin intents into the shared inbox for the UI to consume. */
    private fun absorbIntent(intent: Intent?) {
        intent ?: return
        when {
            // Share sheet: "Share → Dincharya".
            intent.action == Intent.ACTION_SEND &&
                intent.type?.startsWith("text/") == true -> {
                SharedInbox.offerTaskTitle(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
            // Launcher long-press shortcut / widget "+" button.
            intent.getStringExtra(EXTRA_OPEN) == OPEN_ADD_TASK -> {
                SharedInbox.requestAddTask()
            }
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

    companion object {
        const val EXTRA_OPEN = "dincharya.open"
        const val OPEN_ADD_TASK = "add"
        private const val REQUEST_NOTIFICATIONS = 100
    }
}
