package cl.oyl.frutossecos.data.remote.api

import cl.oyl.frutossecos.data.remote.dto.AuthResponseDto
import cl.oyl.frutossecos.data.remote.dto.LoginRequestDto
import cl.oyl.frutossecos.data.remote.dto.ProductDto
import cl.oyl.frutossecos.data.remote.dto.StoreDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato REST (base /api/v1). Se completará cuando exista el backend (Fase 9). */
interface OylApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequestDto): AuthResponseDto
    @GET("products") suspend fun getProducts(
        @Query("category") category: Long? = null,
        @Query("q") query: String? = null,
        @Query("active") active: Boolean? = null,
    ): List<ProductDto>
    @GET("products/{id}") suspend fun getProduct(@Path("id") id: Long): ProductDto
    @GET("products/by-qr/{code}") suspend fun getProductByQr(@Path("code") code: String): ProductDto
    @GET("stores") suspend fun getStores(): List<StoreDto>
}
