package com.sergo379.gamedex.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergo379.gamedex.domain.AnalyticsPoint
import com.sergo379.gamedex.domain.AnalyticsStats
import com.sergo379.gamedex.domain.Trend
import com.sergo379.gamedex.ui.AnalyticsViewModel
import com.sergo379.gamedex.ui.theme.Cyan
import com.sergo379.gamedex.ui.theme.ElevatedSurface
import com.sergo379.gamedex.ui.theme.Pink
import com.sergo379.gamedex.ui.theme.Violet
import java.text.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun AnalyticsHub(onWeekly: () -> Unit, onMonthly: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Аналитика релизов", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Количество игр с датой релиза в RAWG для каждого завершённого календарного дня.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PeriodCard("Последние 7 дней", "Детальный недельный тренд", Icons.Rounded.DateRange, onWeekly)
        PeriodCard("Последние 30 дней", "Полная месячная динамика", Icons.Rounded.CalendarMonth, onMonthly)
        Text("Текущий день не включается, поэтому сравнение не искажено незавершённым периодом.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PeriodCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = ElevatedSurface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.width(42.dp))
            Column(Modifier.padding(start = 16.dp)) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(subtitle) }
        }
    }
}

@Composable
fun WeeklyAnalyticsScreen(viewModel: AnalyticsViewModel, onBack: () -> Unit) = AnalyticsPeriodScreen(7, viewModel, onBack)

@Composable
fun MonthlyAnalyticsScreen(viewModel: AnalyticsViewModel, onBack: () -> Unit) = AnalyticsPeriodScreen(30, viewModel, onBack)

@Composable
private fun AnalyticsPeriodScreen(days: Int, viewModel: AnalyticsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                Column(Modifier.weight(1f)) { Text("Релизы · $days дней", fontSize = 26.sp, fontWeight = FontWeight.Black); Text("RAWG Video Games Database") }
                IconButton(onClick = { viewModel.refresh(true) }) { Icon(Icons.Rounded.Refresh, "Обновить") }
            }
        }
        if (state.loading && state.points.isEmpty()) item { Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        state.error?.let { message -> item { MessageState("Аналитика недоступна", message) { viewModel.refresh(true) } } }
        if (state.points.isNotEmpty()) {
            if (state.fromCache) item { OfflineBanner() }
            if (state.failedDates > 0 || state.stats?.complete == false) item {
                Text("Неполные данные: доступно ${state.stats?.knownValues ?: 0} из $days дней. Пропуски не заменены нулями.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp))
            }
            item { AnalyticsChart(state.points, Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(if (days == 7) 280.dp else 330.dp)) }
            state.stats?.let { stats -> item { StatsGrid(stats) } }
            item {
                val period = "${state.points.first().date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} — ${state.points.last().date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("Период: $period")
                    Text("Последнее обновление: ${state.updatedAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "нет данных"}")
                    Text("Источник: RAWG Video Games Database", color = Cyan)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsChart(points: List<AnalyticsPoint>, modifier: Modifier = Modifier) {
    var selected by remember(points) { mutableIntStateOf(points.indexOfLast { it.count != null }.coerceAtLeast(0)) }
    val progress by animateFloatAsState(1f, label = "chart")
    val values = points.mapNotNull { it.count }
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Card(modifier, colors = CardDefaults.cardColors(containerColor = ElevatedSurface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            points.getOrNull(selected)?.let { Text("${it.date.format(DateTimeFormatter.ofPattern("dd MMM"))}: ${it.count?.toString() ?: "нет данных"}", fontWeight = FontWeight.Bold) }
            Canvas(
                Modifier.fillMaxWidth().weight(1f).pointerInput(points) {
                    detectTapGestures { offset -> selected = ((offset.x / size.width) * (points.size - 1)).roundToInt().coerceIn(points.indices) }
                },
            ) {
                repeat(5) { line ->
                    val y = size.height * line / 4f
                    drawLine(Color.White.copy(alpha = .08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val step = if (points.size <= 1) size.width else size.width / (points.size - 1)
                var segment = Path()
                var active = false
                points.forEachIndexed { index, point ->
                    val count = point.count
                    if (count == null) { if (active) drawPath(segment, Cyan, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)); segment = Path(); active = false; return@forEachIndexed }
                    val x = index * step
                    val y = size.height - (count / max.toFloat()) * size.height * progress
                    if (!active) { segment.moveTo(x, y); active = true } else segment.lineTo(x, y)
                    drawCircle(if (index == selected) Pink else Violet, if (index == selected) 7.dp.toPx() else 4.dp.toPx(), Offset(x, y))
                }
                if (active) drawPath(segment, Cyan, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: AnalyticsStats) {
    val trend = when (stats.trend) { Trend.UP -> "Рост ↑"; Trend.DOWN -> "Падение ↓"; Trend.FLAT -> "Без изменений"; Trend.UNKNOWN -> "Нет данных" }
    val cards = listOf(
        "Минимум" to (stats.minimum?.toString() ?: "—"),
        "Максимум" to (stats.maximum?.toString() ?: "—"),
        "Среднее" to (stats.average?.let { "%.1f".format(it) } ?: "—"),
        "Изменение" to (stats.delta?.let { "%+d".format(it) } ?: "—"),
        "Проценты" to (stats.percentage?.let { "%+.1f%%".format(it) } ?: "не определено"),
        "Тенденция" to trend,
    )
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) } } }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = ElevatedSurface)) { Column(Modifier.padding(14.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
}
