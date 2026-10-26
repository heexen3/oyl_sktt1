package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.oyl.frutossecos.domain.model.Role

/** Sesión activa: un único registro con id = 1. */
@Entity(
    tableName = "session",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("userId")],
)
data class SessionEntity(
    @PrimaryKey val id: Int = SINGLE_SESSION_ID,
    val userId: Long,
    val role: Role,
    val token: String?,
    val loggedAt: Long,
) {
    companion object { const val SINGLE_SESSION_ID = 1 }
}
