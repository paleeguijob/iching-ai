package com.iching.ai.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.PaperBackground
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing

/**
 * First launch, before the disclaimer: Thai or English. The text is the same in both languages on purpose,
 * so a seeker can read it whichever one they speak.
 */
@Composable
fun LanguagePickScreen(onPick: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current.onboarding
    PaperBackground(showMountains = true) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = ZenSpacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CinnabarSeal(text = "易", size = 56.dp, tilt = 0f)
            Spacer(Modifier.height(ZenSpacing.lg))
            Text(strings.languageTitle, style = MaterialTheme.typography.headlineMedium, color = ZenColors.InkBlack, textAlign = TextAlign.Center)
            Spacer(Modifier.height(ZenSpacing.xl))
            ZenPrimaryButton(strings.languageThai, onClick = { onPick(AppLanguage.TH) }, modifier = Modifier.fillMaxWidth(), showSeal = false)
            Spacer(Modifier.height(ZenSpacing.md))
            ZenPrimaryButton(strings.languageEnglish, onClick = { onPick(AppLanguage.EN) }, modifier = Modifier.fillMaxWidth(), showSeal = false)
            Spacer(Modifier.height(ZenSpacing.lg))
            Text(strings.languageHint, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkFaint, textAlign = TextAlign.Center)
        }
    }
}
