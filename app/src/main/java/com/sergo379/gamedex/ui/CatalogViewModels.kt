package com.sergo379.gamedex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sergo379.gamedex.data.ApiErrorMapper
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.InputValidator
import com.sergo379.gamedex.domain.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Game>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Game>>> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = try {
            val page = repository.popular()
            if (page.games.isEmpty()) UiState.Empty("В каталоге пока нет данных")
            else UiState.Success(page.games, page.fromCache)
        } catch (error: Throwable) { UiState.Error(ApiErrorMapper.message(error)) }
    }
}

data class SearchScreenState(
    val query: String = "",
    val games: List<Game> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val fromCache: Boolean = false,
    val hasNext: Boolean = false,
    val page: Int = 1,
    val filters: GameFilters = GameFilters(),
    val error: String? = null,
)

class SearchViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchScreenState())
    val state: StateFlow<SearchScreenState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(value: String) {
        _state.value = _state.value.copy(query = value, error = InputValidator.validateQuery(value))
        searchJob?.cancel()
        if (value.isBlank()) {
            _state.value = _state.value.copy(games = emptyList(), loading = false, error = null)
            return
        }
        searchJob = viewModelScope.launch { delay(450); performSearch(reset = true) }
    }

    fun updateFilters(filters: GameFilters) {
        _state.value = _state.value.copy(filters = filters)
        if (_state.value.query.isNotBlank()) performSearch(reset = true)
    }

    fun clearFilters() = updateFilters(GameFilters())
    fun retry() = performSearch(reset = true)
    fun loadMore() { if (_state.value.hasNext && !_state.value.loadingMore) performSearch(reset = false) }

    private fun performSearch(reset: Boolean) {
        val current = _state.value
        val validation = InputValidator.validateQuery(current.query)
        if (validation != null) {
            _state.value = current.copy(error = validation)
            return
        }
        viewModelScope.launch {
            val page = if (reset) 1 else current.page + 1
            _state.value = current.copy(
                loading = reset,
                loadingMore = !reset,
                error = null,
                games = if (reset) emptyList() else current.games,
            )
            _state.value = try {
                val result = repository.search(current.query, current.filters, page)
                val old = if (reset) emptyList() else current.games
                _state.value.copy(
                    games = (old + result.games).distinctBy(Game::id),
                    loading = false,
                    loadingMore = false,
                    fromCache = result.fromCache,
                    hasNext = result.hasNext,
                    page = page,
                    error = if (result.games.isEmpty() && reset) "Ничего не найдено" else null,
                )
            } catch (error: Throwable) {
                _state.value.copy(loading = false, loadingMore = false, error = ApiErrorMapper.message(error))
            }
        }
    }
}

class DetailViewModel(private val id: Int, private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Game>>(UiState.Loading)
    val state: StateFlow<UiState<Game>> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        _state.value = try {
            val (game, cached) = repository.details(id)
            UiState.Success(game, cached)
        } catch (error: Throwable) { UiState.Error(ApiErrorMapper.message(error)) }
    }
}

@Suppress("UNCHECKED_CAST")
class CatalogViewModelFactory(
    private val repository: GameRepository,
    private val detailId: Int? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository) as T
        modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
        modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel(requireNotNull(detailId), repository) as T
        else -> error("Unknown ViewModel: ${modelClass.name}")
    }
}

