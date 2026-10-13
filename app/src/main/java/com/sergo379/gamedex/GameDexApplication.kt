package com.sergo379.gamedex

import android.app.Application
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.data.AnalyticsRepository
import com.sergo379.gamedex.data.LibraryRepository
import com.sergo379.gamedex.data.local.AppDatabase
import com.sergo379.gamedex.data.local.RoomGameCache
import com.sergo379.gamedex.data.local.SettingsStore
import com.sergo379.gamedex.data.remote.RawgServiceFactory

class GameDexApplication : Application() {
    val graph by lazy { AppGraph(this) }
}

class AppGraph(application: Application) {
    val database = AppDatabase.create(application)
    val settings = SettingsStore(application)
    val api = RawgServiceFactory.create()
    val games = GameRepository(
        api = api,
        apiKey = { settings.apiKey().ifBlank { BuildConfig.RAWG_API_KEY } },
        cache = RoomGameCache(database.gameDao()),
        onUpdated = settings::markUpdated,
    )
    val library = LibraryRepository(database.gameDao())
    val analytics = AnalyticsRepository(
        api = api,
        dao = database.analyticsDao(),
        apiKey = { settings.apiKey().ifBlank { BuildConfig.RAWG_API_KEY } },
    )
}
