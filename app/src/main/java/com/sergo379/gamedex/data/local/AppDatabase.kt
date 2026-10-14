package com.sergo379.gamedex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GameEntity::class, AnalyticsEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE games ADD COLUMN userRatingHalfStars INTEGER")
                database.execSQL("ALTER TABLE games ADD COLUMN addedAt INTEGER")
                // v1 did not record membership time. cachedAt is used only as a best-effort
                // ordering fallback for existing library rows, never presented as an exact date.
                database.execSQL(
                    "UPDATE games SET addedAt = cachedAt " +
                        "WHERE addedAt IS NULL AND (favorite = 1 OR playStatus IS NOT NULL)",
                )
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "gamedex.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}

