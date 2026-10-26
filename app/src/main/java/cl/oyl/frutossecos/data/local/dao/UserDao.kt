package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.oyl.frutossecos.data.local.entity.UserEntity
import cl.oyl.frutossecos.domain.model.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE alias = :alias COLLATE NOCASE LIMIT 1")
    suspend fun findByAlias(alias: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE (alias = :login COLLATE NOCASE OR email = :login COLLATE NOCASE) AND role = :role LIMIT 1")
    suspend fun findForLogin(login: String, role: Role): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    suspend fun countByRole(role: Role): Int

    @Query("SELECT id FROM users WHERE role = :role")
    suspend fun idsByRole(role: Role): List<Long>
}
