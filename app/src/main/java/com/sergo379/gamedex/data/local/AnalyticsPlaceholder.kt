package com.sergo379.gamedex.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics")
data class AnalyticsEntity(@PrimaryKey val date: String, val count: Int, val fetchedAt: Long, val source: String)

@Dao
interface AnalyticsDao

