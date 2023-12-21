package com.zaneschepke.wireguardautotunnel

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zaneschepke.wireguardautotunnel.repository.AppDatabase
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(dbName, 3).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            execSQL(
                "INSERT INTO Settings (is_tunnel_enabled, " +
                        "is_tunnel_on_mobile_data_enabled," +
                        "trusted_network_ssids," +
                        "default_tunnel, " +
                        "is_always_on_vpn_enabled," +
                        "is_tunnel_on_ethernet_enabled," +
                        "is_shortcuts_enabled," +
                        "is_battery_saver_enabled," +
                        "is_tunnel_on_wifi_enabled)" +
                        " VALUES (" +
                        "false," +
                        "false," +
                        "'[trustedSSID1,trustedSSID2]'," +
                        "'defaultTunnel'," +
                        "false," +
                        "false," +
                        "false," +
                        "false," +
                        "false)"
            )
            execSQL(
                "INSERT INTO TunnelConfig (name, wg_quick)" +
                        " VALUES ('hello', 'hello')"
            )
            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        helper.runMigrationsAndValidate(dbName, 4, true)
        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }
}
