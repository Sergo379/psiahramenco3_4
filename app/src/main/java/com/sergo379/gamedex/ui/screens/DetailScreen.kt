package com.sergo379.gamedex.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sergo379.gamedex.domain.UiState
import com.sergo379.gamedex.ui.DetailViewModel
import com.sergo379.gamedex.ui.components.SourceCaption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val current = state) {
        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> MessageState("Карточка недоступна", current.message, viewModel::refresh)
        is UiState.Empty -> MessageState("Нет данных", current.message, viewModel::refresh)
        is UiState.Success -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(340.dp)) {
                    AsyncImage(current.data.backgroundImage, "Обложка ${current.data.name}", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    IconButton(onClick = onBack, modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                }
            }
            if (current.fromCache) item { OfflineBanner() }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(current.data.name, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("★ %.1f  ·  %s%s".format(current.data.rating, current.data.year, current.data.metacritic?.let { "  ·  Metacritic $it" } ?: ""), color = MaterialTheme.colorScheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (current.data.genres + current.data.platforms).take(8).forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
                    if (current.data.description.isNotBlank()) Text(current.data.description)
                    if (current.data.developers.isNotEmpty()) Text("Разработчик: ${current.data.developers.joinToString()}")
                    if (current.data.publishers.isNotEmpty()) Text("Издатель: ${current.data.publishers.joinToString()}")
                    current.data.website?.let { url -> Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Text("Официальный сайт") } }
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://rawg.io/games/${current.data.id}"))) }, modifier = Modifier.fillMaxWidth()) { Text("Открыть источник RAWG") }
                    SourceCaption(Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }
}
