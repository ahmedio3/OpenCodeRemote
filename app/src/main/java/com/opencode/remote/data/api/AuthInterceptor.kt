package com.opencode.remote.data.api

import android.util.Base64
import com.opencode.remote.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val config = runBlocking {
            settingsDataStore.getConfig().first()
        }

        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        if (config.username.isNotBlank() && config.password.isNotBlank()) {
            val credentials = "${config.username}:${config.password}"
            val basicAuth = "Basic " + Base64.encodeToString(
                credentials.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            builder.header("Authorization", basicAuth)
        }

        return chain.proceed(builder.build())
    }
}
