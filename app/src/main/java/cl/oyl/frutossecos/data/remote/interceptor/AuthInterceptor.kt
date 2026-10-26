package cl.oyl.frutossecos.data.remote.interceptor

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/** Guarda en memoria el JWT de la sesión activa (lo actualiza el repositorio de autenticación). */
@Singleton
class TokenHolder @Inject constructor() {
    @Volatile var token: String? = null
}

/** Agrega "Authorization: Bearer <jwt>" cuando hay sesión. */
class AuthInterceptor @Inject constructor(private val tokenHolder: TokenHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenHolder.token
        val request = if (token.isNullOrBlank()) chain.request()
        else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}
