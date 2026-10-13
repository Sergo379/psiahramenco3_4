package com.sergo379.gamedex.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "analytics")
data class AnalyticsEntity(
    @PrimaryKey val date: String,
    val count: Int,
    val fetchedAt: Long,
    val source: String = "RAWG",
)

@Dao
interface AnalyticsDao {
    @Upsert suspend fun upsert(values: List<AnalyticsEntity>)
    @Query("SELECT * FROM analytics WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun between(from: String, to: String): List<AnalyticsEntity>
}

