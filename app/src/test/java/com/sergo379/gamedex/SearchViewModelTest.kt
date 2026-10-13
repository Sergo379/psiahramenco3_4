package com.sergo379.gamedex

import com.sergo379.gamedex.data.GameSearchDataSource
import com.sergo379.gamedex.data.PageResult
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.ui.SearchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun staleOlderResponseCannotOverwriteNewerQuery() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource(ignoreCancellation = true)
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("risk")
        advanceTimeBy(350); runCurrent()
        viewModel.onQueryChanged("hollow knight")
        advanceTimeBy(350); runCurrent()
        source.requests[1].result.complete(page(Game(2, "Hollow Knight")))
        runCurrent()
        source.requests[0].result.complete(page(Game(1, "Risk of Rain")))
        runCurrent()
        assertEquals("hollow knight", viewModel.state.value.query)
        assertEquals(listOf("Hollow Knight"), viewModel.state.value.games.map(Game::name))
    }

    @Test fun rapidTypingRunsOnlyLatestRequest() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource()
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("r")
        advanceTimeBy(100)
        viewModel.onQueryChanged("risk")
        advanceTimeBy(100)
        viewModel.onQueryChanged("risk of rain")
        advanceTimeBy(350); runCurrent()
        assertEquals(listOf("risk of rain"), source.requests.map(Request::query))
        source.requests.single().result.complete(page(Game(1, "Risk of Rain")))
    }

    @Test fun filterChangeCancelsOldRequestAndClearsOldResults() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource(ignoreCancellation = true)
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("risk of rain")
        advanceTimeBy(350); runCurrent()
        val pc = GameFilters(platform = 4)
        viewModel.updateFilters(pc)
        runCurrent()
        assertTrue(viewModel.state.value.games.isEmpty())
        source.requests[1].result.complete(page(Game(2, "Risk of Rain 2")))
        runCurrent()
        source.requests[0].result.complete(page(Game(9, "Old result")))
        runCurrent()
        assertEquals(pc, viewModel.state.value.filters)
        assertEquals(listOf("Risk of Rain 2"), viewModel.state.value.games.map(Game::name))
    }

    @Test fun clearingQueryDuringLoadKeepsResultsEmpty() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource(ignoreCancellation = true)
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("elden ring")
        advanceTimeBy(350); runCurrent()
        viewModel.onQueryChanged("")
        source.requests.single().result.complete(page(Game(8, "Elden Ring")))
        runCurrent()
        assertEquals("", viewModel.state.value.query)
        assertTrue(viewModel.state.value.games.isEmpty())
        assertFalse(viewModel.state.value.loading)
    }

    @Test fun concurrentPaginationRequestsAreRejected() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource()
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("risk of rain")
        advanceTimeBy(350); runCurrent()
        source.requests.single().result.complete(page(Game(1, "Risk of Rain"), hasNext = true))
        runCurrent()
        viewModel.loadMore()
        viewModel.loadMore()
        runCurrent()
        assertEquals(listOf(1, 2), source.requests.map(Request::page))
        source.requests[1].result.complete(page(Game(2, "Risk of Rain 2")))
    }

    @Test fun paginationDoesNotDuplicateGames() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource()
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("risk of rain")
        advanceTimeBy(350); runCurrent()
        source.requests[0].result.complete(page(Game(1, "Risk of Rain"), hasNext = true))
        runCurrent(); viewModel.loadMore(); runCurrent()
        source.requests[1].result.complete(page(Game(1, "Risk of Rain"), Game(2, "Risk of Rain 2")))
        runCurrent()
        assertEquals(listOf(1, 2), viewModel.state.value.games.map(Game::id))
    }

    @Test fun emptyMessageAppearsOnlyAfterCompletedRequest() = runTest(mainDispatcherRule.dispatcher) {
        val source = ControlledSource()
        val viewModel = SearchViewModel(source)
        viewModel.onQueryChanged("unknown game")
        assertTrue(viewModel.state.value.loading)
        assertEquals(null, viewModel.state.value.error)
        advanceTimeBy(350); runCurrent()
        source.requests.single().result.complete(page())
        runCurrent()
        assertEquals("Ничего не найдено", viewModel.state.value.error)
        assertTrue(viewModel.state.value.hasSearched)
    }

    private fun page(vararg games: Game, hasNext: Boolean = false) = PageResult(games.toList(), hasNext, false)

    private data class Request(
        val query: String,
        val filters: GameFilters,
        val page: Int,
        val result: CompletableDeferred<PageResult> = CompletableDeferred(),
    )

    private class ControlledSource(private val ignoreCancellation: Boolean = false) : GameSearchDataSource {
        val requests = mutableListOf<Request>()
        override suspend fun search(query: String, filters: GameFilters, page: Int): PageResult {
            val request = Request(query, filters, page)
            requests += request
            return if (ignoreCancellation) withContext(NonCancellable) { request.result.await() } else request.result.await()
        }
    }
}
