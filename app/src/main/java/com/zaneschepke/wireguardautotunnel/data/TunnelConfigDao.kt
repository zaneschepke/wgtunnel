package com.zaneschepke.wireguardautotunnel.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface TunnelConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(t: TunnelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(t: List<TunnelConfig>)

    @Query("SELECT * FROM TunnelConfig WHERE id=:id")
    suspend fun getById(id: Long): TunnelConfig?

    @Query("SELECT * FROM TunnelConfig")
    suspend fun getAll(): List<TunnelConfig>

    @Delete
    suspend fun delete(t: TunnelConfig)

    @Query("SELECT COUNT('id') FROM TunnelConfig")
    suspend fun count(): Long

    @Query("SELECT * FROM tunnelconfig")
    fun getAllFlow(): Flow<MutableList<TunnelConfig>>
}
