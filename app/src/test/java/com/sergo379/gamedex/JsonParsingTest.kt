package com.sergo379.gamedex

import com.sergo379.gamedex.data.ApiErrorMapper
import com.sergo379.gamedex.data.remote.dto.GameDto
import com.sergo379.gamedex.data.remote.dto.GamePageDto
import java.net.SocketTimeoutException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class JsonParsingTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test fun parsesGameList() {
        val page = json.decodeFromString<GamePageDto>("""{"count":1,"results":[{"id":7,"name":"Hades","rating":4.5}]}""")
        assertEquals(1, page.count); assertEquals("Hades", page.results.single().name)
    }
    @Test fun parsesGameDetails() {
        val game = json.decodeFromString<GameDto>("""{"id":1,"name":"Portal","description_raw":"Puzzle","released":"2007-10-10","genres":[{"id":2,"name":"Puzzle"}]}""")
        assertEquals("Puzzle", game.descriptionRaw); assertEquals("2007-10-10", game.released)
    }
    @Test fun parsesHistoricalCount() {
        assertEquals(42, json.decodeFromString<GamePageDto>("""{"count":42,"results":[]}""").count)
    }
    @Test fun parsesEmptyResults() {
        assertTrue(json.decodeFromString<GamePageDto>("""{"count":0,"results":[]}""").results.isEmpty())
    }
    @Test fun acceptsMissingOptionalFields() {
        val game = json.decodeFromString<GameDto>("""{"id":9,"name":"Minimal"}""")
        assertNull(game.backgroundImage); assertTrue(game.platforms.isEmpty())
    }
    @Test(expected = SerializationException::class) fun rejectsMalformedJson() {
        json.decodeFromString<GamePageDto>("{not-json")
    }
    @Test fun mapsApiAndNetworkErrors() {
        val unauthorized = HttpException(Response.error<Unit>(401, "denied".toResponseBody()))
        assertTrue(ApiErrorMapper.message(unauthorized).contains("API-ключ"))
        assertTrue(ApiErrorMapper.message(SocketTimeoutException()).contains("вовремя"))
    }
}

