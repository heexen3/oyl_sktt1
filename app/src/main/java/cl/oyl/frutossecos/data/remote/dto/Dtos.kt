package cl.oyl.frutossecos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable data class LoginRequestDto(val alias: String, val password: String, val role: String)
@Serializable data class AuthResponseDto(val token: String, val userId: Long, val name: String, val role: String)
@Serializable data class ProductDto(
    val id: Long, val name: String, val categoryId: Long, val description: String = "", val price: Long,
    val presentation: String = "", val weightGrams: Int = 0, val origin: String = "", val imageUrl: String? = null,
    val stock: Int, val minStock: Int = 0, val active: Boolean = true, val qrCode: String,
)
@Serializable data class StoreDto(
    val id: Long, val name: String, val address: String, val lat: Double, val lng: Double,
    val hours: String, val type: String,
)
