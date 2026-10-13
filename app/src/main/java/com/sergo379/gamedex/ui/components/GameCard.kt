package com.sergo379.gamedex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.ui.theme.ElevatedSurface

@Composable
fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color by animateColorAsState(ElevatedSurface, label = "card")
    Card(
        modifier = modifier.width(164.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.78f)) {
            if (game.backgroundImage != null) {
                AsyncImage(
                    model = game.backgroundImage,
                    contentDescription = "Обложка ${game.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(Icons.Rounded.SportsEsports, null, modifier = Modifier.align(Alignment.Center))
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(game.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFC857), modifier = Modifier.height(16.dp))
                Text(" %.1f  ·  %s".format(game.rating, game.year), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SourceCaption(modifier: Modifier = Modifier) {
    Text("Источник данных: RAWG Video Games Database", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

