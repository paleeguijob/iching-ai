package com.iching.ai.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.strings.LocalAppLanguage
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing

/** Settings > Language. Choosing swaps the language app-wide, which also returns the app to Home. */
@Composable
fun LanguageSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val strings = LocalStrings.current
    val selected = LocalAppLanguage.current
    PaperBackground(showMountains = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.common.back, tint = ZenColors.InkBlack)
                }
                Text(strings.settings.language, style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.padding(horizontal = ZenSpacing.screen, vertical = ZenSpacing.xs)) {
                ZenCard(contentPadding = PaddingValues(0.dp)) {
                    LanguageRow(strings.onboarding.languageThai, selected == AppLanguage.TH) { viewModel.setLanguage(AppLanguage.TH) }
                    ZenCardDivider()
                    LanguageRow(strings.onboarding.languageEnglish, selected == AppLanguage.EN) { viewModel.setLanguage(AppLanguage.EN) }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = ZenColors.CinnabarRed)
    }
}
