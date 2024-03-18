package com.zaneschepke.wireguardautotunnel.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig

@Database(
    entities = [Settings::class, TunnelConfig::class],
    version = 6,
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(
                from = 3,
                to = 4,
            ),
            AutoMigration(
                from = 4,
                to = 5,
            ),
            AutoMigration(
                from = 5,
                to = 6,
            ),
        ],
    exportSchema = true,
)
@TypeConverters(DatabaseListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingsDao

    abstract fun tunnelConfigDoa(): TunnelConfigDao
}
