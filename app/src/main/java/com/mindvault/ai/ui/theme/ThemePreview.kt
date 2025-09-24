package com.mindvault.ai.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun ThemeSample() {
    Surface {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "MindVault",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Private voice journaling with on-device AI.",
                style = MaterialTheme.typography.bodyLarge
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Today", style = MaterialTheme.typography.titleLarge)
                    Text(text = "Recorded session ready to summarize.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun MindVaultThemePreviewLight() {
    MindVaultTheme(darkTheme = false, dynamicColor = false) {
        ThemeSample()
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MindVaultThemePreviewDark() {
    MindVaultTheme(darkTheme = true, dynamicColor = false) {
        ThemeSample()
    }
}
