package com.sergo379.gamedex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.InputValidator
import com.sergo379.gamedex.ui.SearchViewModel
import com.sergo379.gamedex.ui.components.GameCard
import com.sergo379.gamedex.ui.components.SourceCaption

@Composable
fun SearchScreen(viewModel: SearchViewModel, onGameClick: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var yearText by remember(state.filters.year) { mutableStateOf(state.filters.year?.toString().orEmpty()) }
    Column(Modifier.fillMaxSize()) {
        Text("Поиск", fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp, top = 20.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            label = { Text("Название игры") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (state.query.isNotEmpty()) {{ IconButton(onClick = { viewModel.onQueryChanged("") }) { Icon(Icons.Rounded.Clear, "Очистить") } }} else null,
            singleLine = true,
            supportingText = { state.error?.takeIf { state.games.isEmpty() }?.let { Text(it) } },
        )
        Text("Фильтры", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("action" to "Экшен", "adventure" to "Приключения", "role-playing-games-rpg" to "RPG").forEach { (slug, label) ->
                FilterChip(
                    selected = state.filters.genre == slug,
                    onClick = { viewModel.updateFilters(state.filters.copy(genre = if (state.filters.genre == slug) null else slug)) },
                    label = { Text(label) },
                )
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = state.filters.platform == 4, onClick = { viewModel.updateFilters(state.filters.copy(platform = if (state.filters.platform == 4) null else 4)) }, label = { Text("PC") })
            FilterChip(selected = state.filters.platform == 187, onClick = { viewModel.updateFilters(state.filters.copy(platform = if (state.filters.platform == 187) null else 187)) }, label = { Text("PlayStation 5") })
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = yearText,
                onValueChange = { value ->
                    yearText = value.take(4)
                    if (InputValidator.validateYear(yearText) == null) viewModel.updateFilters(state.filters.copy(year = yearText.toIntOrNull()))
                },
                label = { Text("Год") },
                isError = InputValidator.validateYear(yearText) != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = { yearText = ""; viewModel.clearFilters() }, modifier = Modifier.padding(start = 10.dp)) { Text("Сбросить") }
        }
        when {
            state.loading -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
            state.games.isEmpty() -> Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error ?: "Введите название игры", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.error != null && state.query.isNotBlank()) Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Повторить") }
            }
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.fromCache) item { OfflineBanner() }
                items(state.games, key = { it.id }) { game ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        GameCard(game, { onGameClick(game.id) }, modifier = Modifier.weight(1f))
                    }
                }
                if (state.hasNext) item { Button(onClick = viewModel::loadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) { if (state.loadingMore) CircularProgressIndicator() else Text("Загрузить ещё") } }
                item { SourceCaption() }
            }
        }
    }
}

