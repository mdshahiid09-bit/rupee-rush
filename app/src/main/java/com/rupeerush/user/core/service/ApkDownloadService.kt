package com.rupeerush.user.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ApkDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val NOTIF_ID = 2001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apkUrl = intent?.getStringExtra("apk_url") ?: return START_NOT_STICKY
        val expectedSha256 = intent.getStringExtra("sha256")

        startForegroundNotification()

        serviceScope.launch {
            downloadAndVerify(apkUrl, expectedSha256)
        }

        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notif = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle("Downloading Rupee Rush Update")
            .setContentText("Connecting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notif)
    }

    private suspend fun downloadAndVerify(downloadUrl: String, expectedSha256: String?) {
        val apkFile = File(cacheDir, "apks/update.apk")
        apkFile.parentFile?.mkdirs()

        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            val digest = MessageDigest.getInstance("SHA-256")

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)
                digest.update(data, 0, count)

                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    updateNotification(progress)
                }
            }

            output.flush()
            output.close()
            input.close()

            // Verify SHA-256 Checksum if provided
            if (!expectedSha256.isNullOrBlank()) {
                val calculatedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (!calculatedSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                    apkFile.delete()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    return
                }
            }

            // Handoff to Android Package Installer
            installApk(apkFile)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } catch (e: Exception) {
            apkFile.delete()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateNotification(progress: Int) {
        val notif = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle("Downloading Rupee Rush Update")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notif)
    }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(installIntent)
    }
}
