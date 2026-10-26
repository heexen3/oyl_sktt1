package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Caché local del catálogo. La fuente oficial de stock/precio será el backend. */
@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("categoryId"), Index(value = ["qrCode"], unique = true)],
)
data class ProductEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val categoryId: Long,
    val description: String,
    val price: Long,
    val presentation: String,
    val weightGrams: Int,
    val origin: String,
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val stock: Int,
    val minStock: Int,
    val active: Boolean = true,
    val qrCode: String,
    val updatedAt: Long,
)
