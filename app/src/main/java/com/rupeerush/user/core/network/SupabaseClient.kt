package com.rupeerush.user.core.network

import com.rupeerush.user.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseClient {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    var userToken: String? = null
    var currentUserId: String? = null

    val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    fun authHeaders(): Map<String, String> {
        val map = mutableMapOf(
            "apikey" to anonKey,
            "Content-Type" to "application/json"
        )
        userToken?.let {
            map["Authorization"] = "Bearer $it"
        }
        return map
    }

    suspend fun callRpc(functionName: String, bodyJson: String): String {
        val url = "$supabaseUrl/rest/v1/rpc/$functionName"
        val response = httpClient.post(url) {
            headers {
                authHeaders().forEach { (k, v) -> header(k, v) }
            }
            contentType(ContentType.Application.Json)
            setBody(bodyJson)
        }
        return response.bodyAsText()
    }
}
