package com.sergo379.gamedex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sergo379.gamedex.data.ApiErrorMapper
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.data.LibraryRepository
import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.data.local.SettingsStore
import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.InputValidator
import com.sergo379.gamedex.domain.PlayStatus
import com.sergo379.gamedex.domain.UiState
import kotlinx.coroutines.Job
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

data class DetailData(val game: Game, val fromCache: Boolean, val favorite: Boolean, val status: PlayStatus?)

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
        _state.value = try {
            val (game, cached) = repository.details(id)
            val local = library.item(id)
            UiState.Success(DetailData(game, cached, local?.favorite == true, local?.status), cached)
        } catch (error: Throwable) { UiState.Error(ApiErrorMapper.message(error)) }
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

