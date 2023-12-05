package com.zaneschepke.wireguardautotunnel.repository

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig

@Database(entities = [Settings::class, TunnelConfig::class], version = 3, autoMigrations = [
    AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)
], exportSchema = true)
@TypeConverters(DatabaseListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingsDoa
    abstract fun tunnelConfigDoa() : TunnelConfigDao
}