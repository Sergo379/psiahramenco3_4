package com.sergo379.gamedex

import com.sergo379.gamedex.data.GameCache
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.data.LibraryRepository
import com.sergo379.gamedex.data.local.GameDao
import com.sergo379.gamedex.data.local.GameEntity
import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.data.remote.dto.GameDto
import com.sergo379.gamedex.data.remote.dto.GamePageDto
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.PlayStatus
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepositoryCacheTest {
    private val entity = GameEntity(1,"Celeste","","2018-01-25",null,4.7,null,"indie","PC","Matt Makes Games","",null,"",cachedAt=123)

    @Test fun addsGameToFavorites() = runTest {
        val dao=FakeDao(listOf(entity)); val repo=LibraryRepository(dao); assertTrue(repo.toggleFavorite(1)); assertTrue(dao.get(1)!!.favorite)
    }
    @Test fun removesGameFromFavorites() = runTest {
        val dao=FakeDao(listOf(entity.copy(favorite=true))); val repo=LibraryRepository(dao); assertFalse(repo.toggleFavorite(1)); assertFalse(dao.get(1)!!.favorite)
    }
    @Test fun changesPlayStatus() = runTest {
        val dao=FakeDao(listOf(entity)); val repo=LibraryRepository(dao); repo.setStatus(1,PlayStatus.COMPLETED); assertEquals("COMPLETED",dao.get(1)!!.playStatus)
    }
    @Test fun usesCacheOnNetworkError() = runTest {
        val cache=FakeCache(mutableListOf(Game(1,"Cached"))); val repo=GameRepository(FakeApi(true),{"key"},cache)
        val result=repo.search("Cached",GameFilters(),1); assertTrue(result.fromCache); assertEquals("Cached",result.games.single().name)
    }
    @Test fun preservesCacheWhenRefreshFails() = runTest {
        val original=Game(1,"Original"); val cache=FakeCache(mutableListOf(original)); val repo=GameRepository(FakeApi(true),{"key"},cache)
        runCatching { repo.popular() }; assertEquals(original,cache.items.single())
    }
    @Test fun savesFreshResponseAndUpdateTime() = runTest {
        val cache=FakeCache(); var updated=false; val repo=GameRepository(FakeApi(false),{"key"},cache,{updated=true})
        repo.search("New",GameFilters(),1); assertEquals("New",cache.items.single().name); assertTrue(updated)
    }

    private class FakeApi(private val fail:Boolean):RawgApi {
        override suspend fun games(key:String,search:String?,genres:String?,platforms:Int?,dates:String?,ordering:String?,page:Int,pageSize:Int):GamePageDto {
            if(fail) throw IOException("offline"); return GamePageDto(1,null,listOf(GameDto(2,search ?: "New")))
        }
        override suspend fun game(id:Int,key:String):GameDto { if(fail) throw IOException("offline"); return GameDto(id,"New") }
    }
    private class FakeCache(val items:MutableList<Game> = mutableListOf()):GameCache {
        override suspend fun save(games:List<Game>){ games.forEach { g->items.removeAll{it.id==g.id};items+=g } }
        override suspend fun find(query:String,limit:Int)=items.filter{it.name.contains(query,true)}.take(limit)
        override suspend fun get(id:Int)=items.firstOrNull{it.id==id}
    }
    private class FakeDao(seed:List<GameEntity>):GameDao {
        private val data=seed.associateBy{it.id}.toMutableMap(); private val flow=MutableStateFlow(seed)
        private fun emit(){flow.value=data.values.toList()}
        override suspend fun upsert(games:List<GameEntity>){games.forEach{data[it.id]=it};emit()}
        override suspend fun get(id:Int)=data[id]
        override suspend fun search(query:String,limit:Int)=data.values.filter{it.name.contains(query,true)}.take(limit)
        override fun observeLibrary():Flow<List<GameEntity>> = flow
        override suspend fun setFavorite(id:Int,favorite:Boolean){data[id]?.let{data[id]=it.copy(favorite=favorite)};emit()}
        override suspend fun setStatus(id:Int,status:String?){data[id]?.let{data[id]=it.copy(playStatus=status)};emit()}
        override suspend fun lastUpdated()=data.values.maxOfOrNull{it.cachedAt}
    }
}
