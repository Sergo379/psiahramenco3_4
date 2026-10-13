package com.sergo379.gamedex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sergo379.gamedex.data.ApiErrorMapper
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.data.GameSearchDataSource
import com.sergo379.gamedex.data.LibraryRepository
import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.data.local.SettingsStore
import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.InputValidator
import com.sergo379.gamedex.domain.PlayStatus
import com.sergo379.gamedex.domain.RelatedGames
import com.sergo379.gamedex.domain.SearchRelevance
import com.sergo379.gamedex.domain.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val page: Int = 0,
    val filters: GameFilters = GameFilters(),
    val error: String? = null,
    val hasSearched: Boolean = false,
)

class SearchViewModel(private val repository: GameSearchDataSource) : ViewModel() {
    private val _state = MutableStateFlow(SearchScreenState())
    val state: StateFlow<SearchScreenState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var requestVersion: Long = 0

    fun onQueryChanged(value: String) {
        startFirstPage(value.take(InputValidator.MAX_QUERY_LENGTH), _state.value.filters, debounceMs = 350)
    }

    fun updateFilters(filters: GameFilters) {
        if (filters == _state.value.filters) return
        startFirstPage(_state.value.query, filters, debounceMs = 0)
    }

    fun clearFilters() = updateFilters(GameFilters())
    fun retry() = startFirstPage(_state.value.query, _state.value.filters, debounceMs = 0)
    fun submit() = startFirstPage(_state.value.query, _state.value.filters, debounceMs = 0)

    fun loadMore() {
        val current = _state.value
        if (!current.hasNext || current.loading || current.loadingMore || searchJob?.isActive == true) return
        val version = requestVersion
        val page = current.page + 1
        _state.value = current.copy(loadingMore = true, error = null)
        searchJob = viewModelScope.launch { execute(version, current.query, current.filters, page, reset = false) }
    }

    private fun startFirstPage(query: String, filters: GameFilters, debounceMs: Long) {
        searchJob?.cancel()
        val version = ++requestVersion
        val validation = if (query.isBlank()) null else InputValidator.validateQuery(query)
        _state.value = SearchScreenState(
            query = query,
            filters = filters,
            loading = query.isNotBlank() && validation == null,
            error = validation,
        )
        if (query.isBlank() || validation != null) return
        searchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            execute(version, query, filters, page = 1, reset = true)
        }
    }

    private suspend fun execute(version: Long, query: String, filters: GameFilters, page: Int, reset: Boolean) {
        try {
            val result = repository.search(query, filters, page)
            if (!isCurrent(version, query, filters)) return
            val old = if (reset) emptyList() else _state.value.games
            val merged = SearchRelevance.rank(query, old + result.games)
            _state.value = _state.value.copy(
                games = merged,
                loading = false,
                loadingMore = false,
                fromCache = result.fromCache,
                hasNext = result.hasNext,
                page = page,
                error = if (merged.isEmpty() && reset) "Ничего не найдено" else null,
                hasSearched = true,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (!isCurrent(version, query, filters)) return
            _state.value = _state.value.copy(
                loading = false,
                loadingMore = false,
                error = ApiErrorMapper.message(error),
                hasSearched = true,
            )
        }
    }

    private fun isCurrent(version: Long, query: String, filters: GameFilters): Boolean =
        version == requestVersion && _state.value.query == query && _state.value.filters == filters
}

data class DetailData(
    val game: Game,
    val fromCache: Boolean,
    val favorite: Boolean,
    val status: PlayStatus?,
    val series: List<Game> = emptyList(),
    val additions: List<Game> = emptyList(),
    val relatedLoading: Boolean = false,
)

class DetailViewModel(
    private val id: Int,
    private val repository: GameRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<DetailData>>(UiState.Loading)
    val state: StateFlow<UiState<DetailData>> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            val (game, cached) = repository.details(id)
            val local = library.item(id)
            _state.value = UiState.Success(DetailData(game, cached, local?.favorite == true, local?.status, relatedLoading = !cached), cached)
            if (!cached) {
                val related = try {
                    repository.related(id)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    RelatedGames()
                }
                val current = (_state.value as? UiState.Success)?.data ?: return@launch
                _state.value = UiState.Success(current.copy(series = related.series, additions = related.additions, relatedLoading = false), cached)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.value = UiState.Error(ApiErrorMapper.message(error))
        }
    }
    fun toggleFavorite() = viewModelScope.launch {
        val current = (_state.value as? UiState.Success)?.data ?: return@launch
        val favorite = library.toggleFavorite(id)
        _state.value = UiState.Success(current.copy(favorite = favorite), current.fromCache)
    }
    fun setStatus(status: PlayStatus?) = viewModelScope.launch {
        library.setStatus(id, status)
        val current = (_state.value as? UiState.Success)?.data ?: return@launch
        _state.value = UiState.Success(current.copy(status = status), current.fromCache)
    }
}

data class LibraryScreenState(
    val games: List<LibraryGame> = emptyList(),
    val query: String = "",
    val status: PlayStatus? = null,
)

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val status = MutableStateFlow<PlayStatus?>(null)
    val state = combine(repository.observe(), query, status) { games, q, filter ->
        LibraryScreenState(
            games.filter { it.game.name.contains(q, true) && (filter == null || it.status == filter) },
            q,
            filter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryScreenState())
    fun setQuery(value: String) { query.value = value.take(InputValidator.MAX_QUERY_LENGTH) }
    fun setStatus(value: PlayStatus?) { status.value = value }
    fun removeFavorite(id: Int) = viewModelScope.launch { repository.toggleFavorite(id) }
}

data class SettingsState(
    val savedKey: String = "",
    val input: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val valid: Boolean = false,
    val lastUpdate: Long? = null,
)

class SettingsViewModel(
    private val store: SettingsStore,
    private val api: RawgApi,
    private val builtInKey: String,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState(valid = builtInKey.isNotBlank()))
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    init {
        viewModelScope.launch { store.apiKeyFlow.collect { saved -> _state.value = _state.value.copy(savedKey = saved, valid = saved.isNotBlank() || builtInKey.isNotBlank()) } }
        viewModelScope.launch { store.lastUpdateFlow.collect { last -> _state.value = _state.value.copy(lastUpdate = last) } }
    }
    fun setInput(value: String) { _state.value = _state.value.copy(input = value.trim().take(80), message = null) }
    fun save() = viewModelScope.launch {
        val key = _state.value.input
        if (key.isBlank()) { _state.value = _state.value.copy(message = "Введите API-ключ RAWG"); return@launch }
        _state.value = _state.value.copy(saving = true)
        val result = try {
            api.games(key = key, pageSize = 1)
            store.saveApiKey(key)
            "Ключ проверен и сохранён"
        } catch (error: Throwable) { ApiErrorMapper.message(error) }
        _state.value = _state.value.copy(input = "", saving = false, message = result)
    }
}

@Suppress("UNCHECKED_CAST")
class CatalogViewModelFactory(
    private val repository: GameRepository,
    private val library: LibraryRepository,
    private val detailId: Int? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository) as T
        modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
        modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel(requireNotNull(detailId), repository, library) as T
        modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(library) as T
        else -> error("Unknown ViewModel: ${modelClass.name}")
    }
}

@Suppress("UNCHECKED_CAST")
class SettingsViewModelFactory(
    private val store: SettingsStore,
    private val api: RawgApi,
    private val builtInKey: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(store, api, builtInKey) as T
}

