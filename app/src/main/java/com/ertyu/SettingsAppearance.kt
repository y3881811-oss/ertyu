package com.ertyu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ertyu.ui.AccentPalette

@Composable
fun SettingsAppearance(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()

    Text("主题", style = MaterialTheme.typography.titleSmall)
    listOf(0 to "跟随系统", 1 to "浅色", 2 to "深色").forEach { (mode, label) ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable { vm.setThemeMode(mode) }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = settings.themeMode == mode, onClick = { vm.setThemeMode(mode) })
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("强调色", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AccentPalette.names.indices.forEach { index ->
            val color = AccentPalette.colorOf(index)
            val selected = settings.accentColor == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Spacer(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { vm.setAccentColor(index) }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = AccentPalette.names[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
