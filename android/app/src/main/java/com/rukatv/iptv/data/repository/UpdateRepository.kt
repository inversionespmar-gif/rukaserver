package com.rukatv.iptv.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val releaseNotes: String? = null,
    val forceUpdate: Boolean = false
)

class UpdateRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(UpdateInfo::class.java)

    suspend fun checkUpdate(hostUrl: String, currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = if (hostUrl.startsWith("http")) hostUrl else "http://$hostUrl"
            val cleanUrl = "${baseUrl.trimEnd('/')}/api/version"
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "RukaTV-App/1.0")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (!bodyString.isNullOrEmpty()) {
                    val info = jsonAdapter.fromJson(bodyString)
                    if (info != null && info.versionCode > currentVersionCode) {
                        val finalApkUrl = if (info.apkUrl.startsWith("http")) {
                            info.apkUrl
                        } else {
                            val hostBase = baseUrl.trimEnd('/')
                            "$hostBase/${info.apkUrl.trimStart('/')}"
                        }
                        return@withContext info.copy(apkUrl = finalApkUrl)
                    }
                }
            }
            null
        }.getOrNull()
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RukaTV-Updater/1.0")
                .header("Accept", "*/*")
                .build()
            val response = downloadClient.newCall(request).execute()

            if (!response.isSuccessful) {
                android.util.Log.e("UpdateRepo", "Download failed HTTP code: ${response.code}")
                // Fallback to system browser if HTTP request failed (e.g. 404 or redirect issue)
                withContext(Dispatchers.Main) {
                    openInBrowser(context, apkUrl)
                }
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val apkFile = File(context.externalCacheDir ?: context.cacheDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportedTime = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val now = System.currentTimeMillis()

                        if (contentLength > 0 && (now - lastReportedTime > 100)) {
                            lastReportedTime = now
                            val progress = totalRead.toFloat() / contentLength.toFloat()
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                installApk(context, apkFile)
            }
            true
        }.getOrElse { error ->
            android.util.Log.e("UpdateRepo", "Download error: ${error.message}", error)
            withContext(Dispatchers.Main) {
                openInBrowser(context, apkUrl)
            }
            false
        }
    }

    private fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun installApk(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
