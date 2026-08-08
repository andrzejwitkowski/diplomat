package pl.diplomat.infrastructure.ota

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class OtaUpdateManager(
    private val context: Context,
) {
    private val otaDir: File
        get() = File(context.cacheDir, "ota").also { it.mkdirs() }

    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun downloadAndValidate(
        url: String,
        onProgress: (Int?) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        otaDir.listFiles()?.forEach { it.delete() }
        val downloaded = download(url.trim(), File(otaDir, "download.bin"), onProgress)
        val apk = resolveApk(downloaded)
        validateApk(apk)
        apk
    }

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun download(url: String, dest: File, onProgress: (Int?) -> Unit): File {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("Download failed: HTTP $code")
            }
            val length = connection.contentLengthLong.takeIf { it > 0 }
            var lastPercent = -1
            if (length == null) onProgress(null)
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        if (length != null) {
                            val percent = ((readTotal * 100) / length).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            return dest
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveApk(downloaded: File): File {
        // APK is a ZIP; PackageManager first, nested .apk only for artifact ZIPs.
        if (context.packageManager.getPackageArchiveInfo(downloaded.absolutePath, 0) != null) {
            return moveToUpdateApk(downloaded)
        }
        if (!isZip(downloaded)) error("Not a valid APK or ZIP")
        val apk = File(otaDir, "update.apk")
        ZipInputStream(downloaded.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                    FileOutputStream(apk).use { zip.copyTo(it) }
                    zip.closeEntry()
                    downloaded.delete()
                    return apk
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("ZIP has no APK")
    }

    private fun moveToUpdateApk(downloaded: File): File {
        val apk = File(otaDir, "update.apk")
        if (downloaded.absolutePath != apk.absolutePath) {
            downloaded.copyTo(apk, overwrite = true)
            downloaded.delete()
        }
        return apk
    }

    private fun isZip(file: File): Boolean {
        if (file.length() < 4) return false
        file.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != 4) return false
            return header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        }
    }

    private fun validateApk(apk: File) {
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("Not a valid APK")
        if (archive.packageName != context.packageName) {
            error("APK package ${archive.packageName} does not match ${context.packageName}")
        }
        val newCode = versionCode(archive)
        val currentCode = versionCode(context.packageManager.getPackageInfo(context.packageName, 0))
        if (newCode <= currentCode) {
            error("versionCode $newCode is not newer than installed $currentCode")
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
}
