package com.sergo379.gamedex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.domain.LibrarySortMode
import com.sergo379.gamedex.domain.PlayStatus
import com.sergo379.gamedex.ui.LibraryViewModel
import com.sergo379.gamedex.ui.components.RatingGold
import com.sergo379.gamedex.ui.components.ratingText

@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = { viewModel.setStatus(if (state.status == status) null else status) },
                    label = { Text(status.label) },
                )
            }
        }
        SortSelector(state.sort, viewModel::setSort)
        if (state.games.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (state.hasLibraryItems) "Ничего не найдено" else "Библиотека пока пуста", fontWeight = FontWeight.Bold)
                Text(
                    if (state.hasLibraryItems) "Измените поиск или фильтр статуса."
                    else "Добавьте игру в избранное, выберите статус или поставьте оценку.",
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(state.games, key = { it.game.id }) { item ->
                    LibraryItemCard(item, onGameClick, viewModel::removeFavorite)
                }
            }
        }
    }
}

@Composable
private fun SortSelector(selected: LibrarySortMode, onSelect: (LibrarySortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Сортировка: ${selected.label}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == selected) "✓ ${mode.label}" else mode.label) },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryGame,
    onGameClick: (Int) -> Unit,
    onRemoveFavorite: (Int) -> Unit,
) {
    Card(
        onClick = { onGameClick(item.game.id) },
        modifier = Modifier.fillMaxWidth().height(132.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.game.backgroundImage,
                contentDescription = "Обложка ${item.game.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(104.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(item.game.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    item.userRatingHalfStars?.let { "Моя оценка: ★ ${ratingText(it)}" } ?: "Моя оценка: —",
                    color = if (item.userRatingHalfStars == null) MaterialTheme.colorScheme.onSurfaceVariant else RatingGold,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(item.status?.label ?: if (item.favorite) "Избранное" else "Оценена", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.favorite) {
                IconButton(onClick = { onRemoveFavorite(item.game.id) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, "Убрать из избранного")
                }
            }
        }
    }
}
