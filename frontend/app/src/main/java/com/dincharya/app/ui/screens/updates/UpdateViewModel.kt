package com.dincharya.app.ui.screens.updates

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dincharya.app.app.APP_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The latest release, as far as GitHub's public API reports it.
 */
data class LatestRelease(
    /** e.g. "v0.2.1" (tag, without a leading 'v' on the number). */
    val version: String,

    /** Human release name, e.g. "Dincharya v0.2.1 — Habits". */
    val name: String,

    /** Markdown release notes body. */
    val notes: String,

    /** Direct download URL of the release APK (first .apk asset). */
    val apkUrl: String,

    /** Human page URL for the release. */
    val pageUrl: String,
)

/**
 * ViewModel for the update-check screen.
 *
 * The app is local-first and offline; this screen is the ONE place that
 * talks to the network — it asks GitHub for the latest published release
 * and shows the notes. Nothing happens without a tap: the user checks, the
 * user presses Update, and even then Android's own installer has the final
 * word. No data ever leaves the phone; only version info comes in.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface State {
        /** Idle — nothing checked yet. */
        object Idle : State

        /** A request is in flight. */
        object Checking : State

        /** Got the latest release; [updateAvailable] says if it's newer than this build. */
        data class Result(
            val currentVersion: String,
            val latest: LatestRelease,
            val updateAvailable: Boolean,
        ) : State

        /** Offline / GitHub unreachable. */
        data class Error(val message: String) : State
    }

    sealed interface InstallState {
        /** No update in progress. */
        object Idle : InstallState

        /** APK download in progress, [percent] of the way there. */
        data class Downloading(val percent: Int) : InstallState

        /** Download finished and the system installer has been opened —
         *  the rest is in Android's (and the user's) hands. */
        object AwaitingInstall : InstallState

        /** The download or installer hand-off failed. */
        data class Failed(val message: String) : InstallState
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val _install = MutableStateFlow<InstallState>(InstallState.Idle)
    val install: StateFlow<InstallState> = _install

    fun check() {
        _state.value = State.Checking
        viewModelScope.launch {
            try {
                val latest = withContext(Dispatchers.IO) { fetchLatestRelease() }
                _state.value = State.Result(
                    currentVersion = APP_VERSION,
                    latest = latest,
                    updateAvailable = isNewer(latest.version, APP_VERSION),
                )
            } catch (e: IOException) {
                _state.value = State.Error("Could not reach GitHub — check your connection.")
            } catch (e: Exception) {
                _state.value = State.Error("Unexpected response from GitHub.")
            }
        }
    }

    /**
     * Download the release APK into the app's private cache and hand it to
     * Android's package installer. The first time, Android asks the user to
     * allow installs from Dincharya once — deliberately one more consent
     * step, in keeping with "the user stays in charge".
     */
    fun downloadAndInstall(apkUrl: String) {
        if (_install.value is InstallState.Downloading) return
        _install.value = InstallState.Downloading(0)
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { downloadApk(apkUrl) }
                _install.value = InstallState.AwaitingInstall
                launchInstaller(file)
            } catch (e: Exception) {
                _install.value = InstallState.Failed(
                    "Download failed — check your connection and try again."
                )
            }
        }
    }

    /** Stream the APK to cache/updates/, reporting progress along the way. */
    private fun downloadApk(apkUrl: String): File {
        val dir = File(getApplication<Application>().cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "dincharya-update.apk")
        if (target.exists()) target.delete()

        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Dincharya-Android")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var done = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _install.value = InstallState.Downloading(percent)
                            }
                        }
                    }
                }
            }
            if (total <= 0) _install.value = InstallState.Downloading(100)
        } finally {
            connection.disconnect()
        }
        return target
    }

    /** Open Android's installer for the downloaded file. */
    private fun launchInstaller(file: File) {
        val app = getApplication<Application>()
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android-package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
            .onFailure { _install.value = InstallState.Failed("Could not open the installer.") }
    }

    /** Plain numeric compare of "v0.2.1" vs "0.2.0" style versions. */
    private fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.trim()
            .removePrefix("v")
            .split('-', '.')
            .mapNotNull { it.toIntOrNull() }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i) ?: 0
            val y = b.getOrNull(i) ?: 0
            if (x != y) return x > y
        }
        return false
    }

    private fun fetchLatestRelease(): LatestRelease {
        val url = URL("https://api.github.com/repos/AbhinayAmbati/dincharya/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Dincharya-Android")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            LatestRelease(
                version = json.optString("tag_name", ""),
                name = json.optString("name", ""),
                notes = json.optString("body", ""),
                apkUrl = json.optJSONArray("assets")
                    ?.let { assets ->
                        (0 until assets.length())
                            .map { assets.getJSONObject(it) }
                            .firstOrNull { it.optString("name").endsWith(".apk") }
                            ?.optString("browser_download_url")
                    } ?: json.optString("html_url", ""),
                pageUrl = json.optString("html_url", ""),
            )
        } finally {
            connection.disconnect()
        }
    }
}
