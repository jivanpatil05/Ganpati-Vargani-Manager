package com.ganpati.vargani.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganpati.vargani.data.local.room.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    fun observeByMobile(mobile: String): Flow<UserEntity?>

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
