package com.dincharya.app.ui.screens.updates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The latest release, as far as GitHub's public API reports it.
 */
data class LatestRelease(
    /** e.g. "v0.2.0" (tag, without a leading 'v' on the number). */
    val version: String,

    /** Human release name, e.g. "Dincharya v0.2.0 — Habits". */
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
 * and shows the notes, so the user can decide whether to download. Nothing
 * is ever downloaded or installed automatically.
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

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

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

    companion object {
        /** Keep in sync with app/build.gradle.kts. */
        const val APP_VERSION = "0.2.0"
    }
}
