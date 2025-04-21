package com.zaneschepke.wireguardautotunnel.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) =
        db.run {
            beginTransaction()
            try {
                execSQL(Queries.createDefaultSettings())
                Timber.i("Bootstrapping settings data")
                setTransactionSuccessful()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                endTransaction()
            }
        }
}
