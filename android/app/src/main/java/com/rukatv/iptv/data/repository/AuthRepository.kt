package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.Credentials
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.remote.UrlBuilder
import com.rukatv.iptv.data.remote.XtreamApi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AuthRepository(
    private val api: XtreamApi,
    private val store: CredentialsStore
) {
    val credentialsFlow = store.credentials

    suspend fun login(host: String, username: String, password: String): Result<Credentials> {
        return runCatching {
            val resp = api.authenticate(username, password)
            if (resp.user_info?.auth == 1) {
                val c = Credentials(host, username, password)
                store.save(c)
                c
            } else {
                throw IllegalArgumentException(resp.user_info?.message ?: "Credenciales inválidas")
            }
        }
    }

    suspend fun logout() = store.clear()

    companion object {
        fun buildApi(host: String): XtreamApi {
            val base = UrlBuilder.apiBase(host).removeSuffix("player_api.php")
            return Retrofit.Builder()
                .baseUrl(if (base.endsWith("/")) base else "$base/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(XtreamApi::class.java)
        }
    }
}
