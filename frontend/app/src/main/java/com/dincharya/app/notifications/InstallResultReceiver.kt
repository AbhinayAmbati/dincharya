package com.dincharya.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/**
 * Receives the verdict of a PackageInstaller session started by the update
 * screen (see UpdateViewModel.installWithSession).
 *
 * The interesting case is PENDING_USER_ACTION: Android hands back an intent
 * that shows its own install-confirmation UI. We simply launch it — the
 * user confirms, and the system does the rest. This path works even on ROMs
 * whose "installer" never answers the ordinary share-an-APK intent.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirmIntent?.let { runCatching { context.startActivity(it) } }
            }
            // STATUS_SUCCESS: the update kills this old process anyway.
            // Failures: the update screen already offers the browser fallback.
        }
    }
}
