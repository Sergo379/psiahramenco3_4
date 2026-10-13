package com.sergo379.gamedex.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sergo379.gamedex.ui.SettingsViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, firstRun: Boolean, onContinue: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (firstRun) "Подключите RAWG" else "Настройки", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(
            "GameDex получает реальные сведения об играх из RAWG. Бесплатный персональный ключ сохраняется только на устройстве.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setInput,
            label = { Text("RAWG API-ключ") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            if (state.saving) CircularProgressIndicator() else Text("Проверить и сохранить")
        }
        OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://rawg.io/apidocs"))) }, modifier = Modifier.fillMaxWidth()) { Text("Получить бесплатный ключ") }
        state.message?.let { Text(it, modifier = Modifier.padding(12.dp), color = if (state.valid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error) }
        Text("Статус API: ${if (state.valid) "ключ настроен" else "ключ отсутствует"}")
        Text("Последнее обновление: ${state.lastUpdate?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "ещё не выполнялось"}")
        Text("Источник: RAWG Video Games Database", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
        if (state.valid) Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text(if (firstRun) "Открыть GameDex" else "Готово") }
    }
}
