package com.rupeerush.user.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.rupeerush.user.core.network.SupabaseClient
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class InstallDetectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return

            // Verify first install time from PackageManager
            try {
                val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                val installTimeMs = packageInfo.firstInstallTime

                CoroutineScope(Dispatchers.IO).launch {
                    notifyServerInstallation(packageName, installTimeMs)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Ignore
            }
        }
    }

    private suspend fun notifyServerInstallation(packageName: String, installTimeMs: Long) {
        try {
            val url = "${SupabaseClient.supabaseUrl}/functions/v1/verify-task-install"
            val body = JSONObject().apply {
                put("package_name", packageName)
                put("installed_timestamp_ms", installTimeMs)
            }.toString()

            SupabaseClient.httpClient.post(url) {
                headers {
                    SupabaseClient.authHeaders().forEach { (k, v) -> header(k, v) }
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (_: Exception) {}
    }
}
