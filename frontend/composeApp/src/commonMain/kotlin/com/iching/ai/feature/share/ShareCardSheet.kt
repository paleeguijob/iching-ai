package com.iching.ai.feature.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iching.ai.platform.encodePng
import com.iching.ai.platform.rememberImageSharer
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.components.ZenSecondaryButton
import com.iching.ai.ui.theme.ZenColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val CARD_WIDTH_PX = 1080f
private const val CARD_HEIGHT_PX = 1920f

/** Full-screen preview of the card, with the button that opens the system share sheet. */
@Composable
fun ShareCardSheet(content: ShareCardContent, log: (String, Map<String, String>) -> Unit, onDismiss: () -> Unit) {
    val layer = rememberGraphicsLayer()
    val sharer = rememberImageSharer()
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ZenColors.PaperShade)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val fit = with(LocalDensity.current) {
                    min(maxWidth.toPx() / CARD_WIDTH_PX, maxHeight.toPx() / CARD_HEIGHT_PX)
                }
                // Fixed density and font scale: the same pixels on every phone, whatever its settings.
                CompositionLocalProvider(LocalDensity provides Density(3f, fontScale = 1f)) {
                    Box(
                        Modifier
                            .requiredSize(360.dp, 640.dp)
                            .graphicsLayer {
                                scaleX = fit
                                scaleY = fit
                            }
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                    ) {
                        ShareCard(content)
                    }
                }
            }
            if (failed) {
                Text(
                    "แชร์ไม่สำเร็จ โปรดลองใหม่",
                    color = ZenColors.CinnabarRed,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZenSecondaryButton("ปิด", onClick = onDismiss, modifier = Modifier.weight(1f))
                ZenPrimaryButton(
                    text = "แชร์",
                    enabled = !sharing,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sharing = true
                        failed = false
                        scope.launch {
                            val shared = try {
                                val png = withContext(Dispatchers.Default) { layer.toImageBitmap().encodePng() }
                                sharer.share(png, SHARE_TEXT)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                false
                            }
                            if (shared) log("share_card", mapOf("source" to content.source))
                            failed = !shared
                            sharing = false
                        }
                    },
                )
            }
        }
    }
}
