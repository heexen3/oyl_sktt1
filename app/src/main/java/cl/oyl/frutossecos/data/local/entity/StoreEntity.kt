package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.oyl.frutossecos.domain.model.StoreType

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val hours: String,
    val type: StoreType,
)
