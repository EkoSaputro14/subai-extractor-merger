package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(raw: String): SemanticVersion {
            val cleaned = raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
                .trim()
            val parts = cleaned.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return SemanticVersion(major, minor, patch)
        }
    }
}

data class GitHubReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String
) {
    val formattedSizeMb: String
        get() {
            if (sizeBytes <= 0L) return "APK"
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }
}

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkAsset: GitHubReleaseAsset?
)

data class AppUpdateUiState(
    val currentVersionName: String = "1.2.1",
    val currentVersionCode: Int = 4,
    val repoOwner: String = "EkoSaputro14",
    val repoName: String = "subai-extractor-merger",
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestRelease: GitHubReleaseInfo? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isDownloadingApk: Boolean = false,
    val lastDownloadId: Long? = null,
    val downloadedFileName: String? = null
)

object AppUpdateChecker {

    fun isNewerVersion(currentVersion: String, remoteVersion: String): Boolean {
        val current = SemanticVersion.parse(currentVersion)
        val remote = SemanticVersion.parse(remoteVersion)
        return remote > current
    }

    fun parseReleaseJson(jsonString: String): GitHubReleaseInfo {
        val root = JSONObject(jsonString)
        val tagName = root.optString("tag_name", "").trim()
        val versionName = tagName.removePrefix("v").removePrefix("V").ifBlank { "1.0.0" }
        val releaseTitle = root.optString("name", tagName).ifBlank { "Release $tagName" }
        val releaseNotes = root.optString("body", "").trim()
        val publishedAt = root.optString("published_at", "").substringBefore("T")
        val htmlUrl = root.optString("html_url", "")

        var selectedApkAsset: GitHubReleaseAsset? = null
        val assetsArray = root.optJSONArray("assets")
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.optJSONObject(i) ?: continue
                val assetName = assetObj.optString("name", "")
                val downloadUrl = assetObj.optString("browser_download_url", "")
                val sizeBytes = assetObj.optLong("size", 0L)
                val contentType = assetObj.optString("content_type", "")
                if (downloadUrl.isNotBlank() &&
                    (assetName.endsWith(".apk", ignoreCase = true) ||
                        contentType.contains("android.package-archive", ignoreCase = true))
                ) {
                    selectedApkAsset = GitHubReleaseAsset(
                        name = assetName,
                        downloadUrl = downloadUrl,
                        sizeBytes = sizeBytes,
                        contentType = contentType
                    )
                    break
                }
            }
        }

        return GitHubReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseTitle = releaseTitle,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            apkAsset = selectedApkAsset
        )
    }

    suspend fun checkLatestRelease(
        owner: String,
        repo: String,
        currentVersionName: String
    ): Result<Pair<Boolean, GitHubReleaseInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SubAI-Extractor-Android/$currentVersionName")
            }

            try {
                val code = connection.responseCode
                val body = if (code in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
                } else {
                    val errText = connection.errorStream?.let {
                        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
                    }.orEmpty()
                    if (code == 404) {
                        throw IllegalStateException("Belum ada rilis yang dipublikasikan di repositori $owner/$repo.")
                    }
                    throw IllegalStateException("GitHub API mengembalikan HTTP $code: ${errText.take(160)}")
                }

                val releaseInfo = parseReleaseJson(body)
                val hasNewer = isNewerVersion(currentVersionName, releaseInfo.versionName)
                hasNewer to releaseInfo
            } finally {
                connection.disconnect()
            }
        }
    }

    fun enqueueApkDownload(
        context: Context,
        asset: GitHubReleaseAsset,
        versionTag: String
    ): Result<Pair<Long, String>> {
        return runCatching {
            val safeFileName = asset.name.ifBlank {
                "SubAI-Extractor-Merger-${versionTag.ifBlank { "latest" }}.apk"
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: throw IllegalStateException("DownloadManager tidak tersedia di perangkat ini.")

            val request = DownloadManager.Request(Uri.parse(asset.downloadUrl))
                .setTitle("Mengunduh $safeFileName")
                .setDescription("Mengunduh pembaruan SubAI Extractor & Merger ($versionTag)")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName)

            val downloadId = downloadManager.enqueue(request)
            downloadId to safeFileName
        }
    }

    fun openUrlInBrowser(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
