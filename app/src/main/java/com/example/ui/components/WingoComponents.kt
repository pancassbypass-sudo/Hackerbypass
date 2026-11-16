package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FeedStatus
import com.example.domain.WingoSize
import com.example.ui.theme.AmberLarge
import com.example.ui.theme.CoralLargeGlow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RoseLoss
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Long, showPlus: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val absFormatted = formatter.format(kotlin.math.abs(amount))
    return when {
        amount < 0 -> "-Rp $absFormatted"
        amount > 0 && showPlus -> "+Rp $absFormatted"
        else -> "Rp $absFormatted"
    }
}

@Composable
fun sizeAccentColor(size: WingoSize): Color =
    if (size == WingoSize.LARGE) AmberLarge else ElectricCyan

@Composable
fun FeedStatusBadge(
    feedStatus: FeedStatus,
    modifier: Modifier = Modifier
) {
    val (label, color, icon) = when (feedStatus) {
        is FeedStatus.LiveConnected -> Triple("LIVE • ${feedStatus.lastSyncTime}", EmeraldPulse, Icons.Filled.CheckCircle)
        is FeedStatus.ManualActive -> Triple("WEB/MANUAL (${feedStatus.count})", ElectricCyan, Icons.Filled.EditNote)
        is FeedStatus.Loading -> Triple("SYNCING...", ElectricCyan, Icons.Filled.Sync)
        is FeedStatus.TokenRequired -> Triple("TOKEN / PASTE NEEDED", AmberLarge, Icons.Filled.Key)
        is FeedStatus.Error -> Triple("FEED ERROR", RoseLoss, Icons.Filled.CloudOff)
        is FeedStatus.Idle -> Triple("STANDBY", Color(0xFF94A3B8), Icons.Filled.Bolt)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SizeCalloutPill(
    size: WingoSize,
    modifier: Modifier = Modifier
) {
    val color = sizeAccentColor(size)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = size.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun NumberOrb(
    number: Int,
    orbSize: Dp = 44.dp,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val wingoSize = WingoSize.fromNumber(number)
    val accent = if (wingoSize == WingoSize.LARGE) AmberLarge else ElectricCyan
    val secondary = if (wingoSize == WingoSize.LARGE) CoralLargeGlow else EmeraldPulse

    Box(
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isHighlighted) 0.34f else 0.18f),
                        secondary.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = accent.copy(alpha = if (isHighlighted) 0.9f else 0.5f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (orbSize.value * 0.42f).sp,
            color = Color.White
        )
    }
}

@Composable
fun ProbabilitySplitBar(
    largePct: Int,
    smallPct: Int,
    modifier: Modifier = Modifier
) {
    val animatedLarge by animateFloatAsState(
        targetValue = (largePct / 100f).coerceIn(0.05f, 0.95f),
        animationSpec = tween(durationMillis = 500),
        label = "largeSplit"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LARGE (5–9)  $largePct%",
                style = MaterialTheme.typography.labelMedium,
                color = AmberLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$smallPct%  SMALL (0–4)",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianSurfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedLarge)
                    .background(
                        Brush.horizontalGradient(listOf(AmberLarge, CoralLargeGlow))
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - animatedLarge)
                    .background(
                        Brush.horizontalGradient(listOf(ElectricCyan, EmeraldPulse))
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogResultSheet(
    currentIssue: String,
    onDismiss: () -> Unit,
    onSelectNumber: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Log Result for Period ${currentIssue.takeLast(8)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap the actual drawn digit (0–9) to evaluate the prediction, update bankroll, and generate the next AI Large/Small call.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "SMALL (0–4)",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..4).forEach { digit ->
                    DigitPickButton(
                        digit = digit,
                        accent = ElectricCyan,
                        onClick = {
                            onSelectNumber(digit)
                            onDismiss()
                        }
                    )
                }
            }

            Text(
                text = "LARGE (5–9)",
                style = MaterialTheme.typography.labelMedium,
                color = AmberLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (5..9).forEach { digit ->
                    DigitPickButton(
                        digit = digit,
                        accent = AmberLarge,
                        onClick = {
                            onSelectNumber(digit)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DigitPickButton(
    digit: Int,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(54.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.5.dp, accent.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("log_digit_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteWebDataSheet(
    initialText: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var input by remember { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Paste Web Draw Data",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Paste recent numbers newest-first (e.g. 2, 7, 4, 1, 9, 0, 3, 6) or raw 55Five GetNoaverageEmerdList JSON.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Draw numbers or JSON") },
                placeholder = { Text("e.g. 3, 8, 1, 4, 9, 2, 6, 0, 7, 5") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_paste_input")
            )
            Button(
                onClick = {
                    onApply(input)
                    onDismiss()
                },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sheet_apply_paste_button")
            ) {
                Text("Analyze with AI Engine", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
