package com.sergo379.gamedex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sergo379.gamedex.domain.UiState
import com.sergo379.gamedex.ui.HomeViewModel
import com.sergo379.gamedex.ui.components.GameCard
import com.sergo379.gamedex.ui.components.SourceCaption

@Composable
fun HomeScreen(viewModel: HomeViewModel, onGameClick: (Int) -> Unit, onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> MessageState("Не удалось загрузить каталог", current.message, viewModel::refresh)
        is UiState.Empty -> MessageState("Каталог пуст", current.message, viewModel::refresh)
        is UiState.Success -> {
            val games = current.data
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("GAMEDEX", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("Discover. Track. Play.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, "Обновить") }
                        IconButton(onClick = onSettingsClick) { Icon(Icons.Rounded.Settings, "Настройки") }
                    }
                }
                if (current.fromCache) item { OfflineBanner() }
                games.firstOrNull()?.let { hero ->
                    item {
                        Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(310.dp).clip(RoundedCornerShape(28.dp))) {
                            AsyncImage(hero.backgroundImage, "Обложка ${hero.name}", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0D18)))))
                            Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                                Text("РЕКОМЕНДУЕМ", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                                Text(hero.name, fontSize = 30.sp, fontWeight = FontWeight.Black)
                                Text("★ %.1f  ·  %s".format(hero.rating, hero.year), modifier = Modifier.padding(vertical = 8.dp))
                                Button(onClick = { onGameClick(hero.id) }) { Text("Подробнее") }
                            }
                        }
                    }
                }
                item { SectionTitle("Популярное сейчас") }
                item {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(games.drop(1).take(10), key = { it.id }) { GameCard(it, { onGameClick(it.id) }) }
                    }
                }
                item { SectionTitle("Высокооценённые") }
                item {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(games.sortedByDescending { it.rating }.take(10), key = { it.id }) { GameCard(it, { onGameClick(it.id) }) }
                    }
                }
                item {
                    Button(onClick = onSearchClick, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) { Text("Найти игру") }
                }
                item { SourceCaption(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            }
        }
    }
}

@Composable
fun OfflineBanner() {
    Text(
        "Офлайн · показаны сохранённые данные",
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary.copy(alpha = .1f)).padding(12.dp),
    )
}

@Composable
fun SectionTitle(text: String) { Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp)) }

@Composable
fun MessageState(title: String, message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = retry) { Text("Повторить") }
    }
}

