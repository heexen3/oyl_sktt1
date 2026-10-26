package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.oyl.frutossecos.domain.model.Role

/** Perfil local. La contraseña nunca se guarda en texto plano: solo hash PBKDF2 + sal. */
@Entity(
    tableName = "users",
    indices = [Index(value = ["alias"], unique = true), Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val alias: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val avatarId: Int? = null,
    val avatarUri: String? = null,
    val passwordHash: String,
    val salt: String,
    val role: Role,
    val notificationsEnabled: Boolean = true,
)
