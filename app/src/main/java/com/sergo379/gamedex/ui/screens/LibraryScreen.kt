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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergo379.gamedex.domain.PlayStatus
import com.sergo379.gamedex.ui.LibraryViewModel
import com.sergo379.gamedex.ui.components.GameCard

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onGameClick: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Text("Моя библиотека", fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(20.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            label = { Text("Поиск в библиотеке") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            singleLine = true,
        )
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayStatus.entries.forEach { status ->
                FilterChip(selected = state.status == status, onClick = { viewModel.setStatus(if (state.status == status) null else status) }, label = { Text(status.label) })
            }
        }
        if (state.games.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Библиотека пока пуста", fontWeight = FontWeight.Bold)
                Text("Добавьте игру в избранное или выберите статус прохождения.")
            }
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(state.games, key = { it.game.id }) { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GameCard(item.game, { onGameClick(item.game.id) }, Modifier.weight(1f))
                    Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.status?.label ?: "Избранное")
                        if (item.favorite) IconButton(onClick = { viewModel.removeFavorite(item.game.id) }) { Icon(Icons.Rounded.DeleteOutline, "Убрать из избранного") }
                    }
                }
            }
        }
    }
}

