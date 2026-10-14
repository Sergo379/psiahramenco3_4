package com.sergo379.gamedex

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sergo379.gamedex.data.local.AnalyticsEntity
import com.sergo379.gamedex.data.local.AppDatabase
import com.sergo379.gamedex.data.local.GameEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Before fun deleteOldDatabase() {
        context.deleteDatabase(TEST_DB)
        context.deleteDatabase(PERSISTENCE_DB)
    }

    @After fun cleanUp() {
        context.deleteDatabase(TEST_DB)
        context.deleteDatabase(PERSISTENCE_DB)
    }

    @Test fun migrate1To2PreservesLibraryStatusAndAnalytics() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO games (id,name,description,rating,genres,platforms,developers,publishers,screenshots,favorite,playStatus,cachedAt) " +
                    "VALUES (1,'Favorite','',4.2,'','','','','',1,'COMPLETED',111)",
            )
            execSQL(
                "INSERT INTO games (id,name,description,rating,genres,platforms,developers,publishers,screenshots,favorite,playStatus,cachedAt) " +
                    "VALUES (2,'Cached only','',3.0,'','','','','',0,NULL,222)",
            )
            execSQL("INSERT INTO analytics (date,count,fetchedAt,source) VALUES ('2026-10-01',7,333,'RAWG')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
        migrated.query("SELECT favorite, playStatus, userRatingHalfStars, addedAt FROM games WHERE id=1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals("COMPLETED", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertEquals(111L, cursor.getLong(3))
        }
        migrated.query("SELECT addedAt FROM games WHERE id=2").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        migrated.query("SELECT count, fetchedAt, source FROM analytics WHERE date='2026-10-01'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7, cursor.getInt(0))
            assertEquals(333L, cursor.getLong(1))
            assertEquals("RAWG", cursor.getString(2))
        }
        migrated.close()
    }

    @Test fun ratingAndAddedAtSurviveDatabaseRestartWithoutDuplicates() = runBlocking {
        var database = openPersistenceDatabase()
        val entity = GameEntity(
            id = 9,
            name = "Hollow Knight",
            description = "",
            released = null,
            backgroundImage = null,
            rating = 4.4,
            metacritic = null,
            genres = "",
            platforms = "",
            developers = "",
            publishers = "",
            website = null,
            screenshots = "",
            cachedAt = 10,
        )
        database.gameDao().upsert(listOf(entity))
        database.gameDao().setUserRating(9, 9, 500)
        database.gameDao().upsert(listOf(entity.copy(name = "Hollow Knight updated", userRatingHalfStars = 9, addedAt = 500)))
        database.analyticsDao().upsert(listOf(AnalyticsEntity("2026-10-02", 3, 600)))
        database.close()

        database = openPersistenceDatabase()
        val restored = database.gameDao().get(9)
        assertEquals(9, restored?.userRatingHalfStars)
        assertEquals(500L, restored?.addedAt)
        assertEquals("Hollow Knight updated", restored?.name)
        assertEquals(1, database.gameDao().search("Hollow Knight", 10).size)
        database.gameDao().setUserRating(9, null, 700)
        assertNull(database.gameDao().get(9)?.userRatingHalfStars)
        assertEquals(500L, database.gameDao().get(9)?.addedAt)
        assertFalse(database.analyticsDao().between("2026-10-02", "2026-10-02").isEmpty())
        database.close()
    }

    private fun openPersistenceDatabase() = Room.databaseBuilder(context, AppDatabase::class.java, PERSISTENCE_DB)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val PERSISTENCE_DB = "persistence-test.db"
    }
}
