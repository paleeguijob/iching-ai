package com.iching.ai.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.iching.ai.ui.theme.ZenColors

/**
 * The poster as a full-screen popup: the artwork (4:5, never taller than 70% of the screen), and a round × above
 * its top-right corner. Tapping outside closes it; tapping the artwork follows its link, when it has one.
 */
@Composable
fun PosterPopupDialog(popup: PosterPopup, onTap: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            val maxImageHeight = maxHeight * 0.7f
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.padding(bottom = 4.dp).size(48.dp),
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(ZenColors.PaperCream), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "ปิดประกาศนี้", tint = ZenColors.InkBlack, modifier = Modifier.size(18.dp))
                    }
                }
                val tap = popup.link?.let { Modifier.clickable(role = Role.Button, onClick = onTap) } ?: Modifier
                AsyncImage(
                    model = popup.imageUrl,
                    contentDescription = popup.alt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = this@BoxWithConstraints.maxWidth - 48.dp)
                        .heightIn(max = maxImageHeight)
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(16.dp))
                        // Swallows taps on the artwork so they never reach the close-on-outside backdrop.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .then(tap),
                )
            }
        }
    }
}
