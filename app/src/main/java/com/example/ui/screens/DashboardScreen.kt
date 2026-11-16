package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AiState
import com.example.domain.DrawEntry
import com.example.domain.FeedStatus
import com.example.domain.QuantAnalysisResult
import com.example.domain.WingoSize
import com.example.ui.components.FeedStatusBadge
import com.example.ui.components.NumberOrb
import com.example.ui.components.ProbabilitySplitBar
import com.example.ui.components.SizeCalloutPill
import com.example.ui.components.formatRupiah
import com.example.ui.components.sizeAccentColor
import com.example.ui.theme.AmberLarge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.IndigoAi
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RoseLoss
import com.example.ui.viewmodel.WingoUiState

@Composable
fun DashboardScreen(
    uiState: WingoUiState,
    isWideScreen: Boolean,
    onRefreshLive: () -> Unit,
    onToggleAutoPoll: (Boolean) -> Unit,
    onAnalyzeAi: () -> Unit,
    onOpenLogResult: () -> Unit,
    onOpenPasteSheet: () -> Unit,
    onNavigateToFeedSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.widthIn(max = 960.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LivePeriodBanner(
                    uiState = uiState,
                    onRefreshLive = onRefreshLive,
                    onToggleAutoPoll = onToggleAutoPoll
                )

                if (uiState.draws.isEmpty()) {
                    EmptyFeedOnboardingCard(
                        feedStatus = uiState.feedStatus,
                        onOpenPasteSheet = onOpenPasteSheet,
                        onNavigateToFeedSetup = onNavigateToFeedSetup
                    )
                } else {
                    if (isWideScreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HeroPredictionCard(
                                    uiState = uiState,
                                    onAnalyzeAi = onAnalyzeAi,
                                    onOpenLogResult = onOpenLogResult,
                                    onOpenPasteSheet = onOpenPasteSheet
                                )
                                DrawSequenceRibbon(draws = uiState.draws, quant = uiState.quantResult)
                            }
                            Column(
                                modifier = Modifier.weight(0.9f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AiAndQuantMatrixCard(
                                    quant = uiState.quantResult,
                                    aiState = uiState.aiState,
                                    onRetryAi = onAnalyzeAi
                                )
                            }
                        }
                    } else {
                        HeroPredictionCard(
                            uiState = uiState,
                            onAnalyzeAi = onAnalyzeAi,
                            onOpenLogResult = onOpenLogResult,
                            onOpenPasteSheet = onOpenPasteSheet
                        )
                        DrawSequenceRibbon(draws = uiState.draws, quant = uiState.quantResult)
                        AiAndQuantMatrixCard(
                            quant = uiState.quantResult,
                            aiState = uiState.aiState,
                            onRetryAi = onAnalyzeAi
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LivePeriodBanner(
    uiState: WingoUiState,
    onRefreshLive: () -> Unit,
    onToggleAutoPoll: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_ai_core_banner_1794821522144),
                contentDescription = "Quantitative waveform banner",
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.large)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ObsidianSurface.copy(alpha = 0.92f),
                                ObsidianSurface.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeedStatusBadge(feedStatus = uiState.feedStatus)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AUTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.session.autoPollEnabled,
                            onCheckedChange = onToggleAutoPoll,
                            enabled = uiState.session.bearerToken.isNotBlank(),
                            modifier = Modifier.testTag("auto_poll_switch")
                        )
                        IconButton(
                            onClick = onRefreshLive,
                            modifier = Modifier.testTag("refresh_feed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh live feed",
                                tint = ElectricCyan
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "ACTIVE ISSUE PERIOD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.currentIssue,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NEXT BET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRupiah(uiState.session.currentBet),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EmeraldPulse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPredictionCard(
    uiState: WingoUiState,
    onAnalyzeAi: () -> Unit,
    onOpenLogResult: () -> Unit,
    onOpenPasteSheet: () -> Unit
) {
    val quant = uiState.quantResult ?: return
    val aiReady = (uiState.aiState as? AiState.Ready)?.verdict
    val displaySize = aiReady?.predictedSize ?: quant.predictedSize
    val displayNum = aiReady?.predictedNumber ?: quant.predictedNumber
    val backupNum = aiReady?.backupNumber ?: quant.backupNumber
    val confidence = aiReady?.confidence ?: quant.confidence
    val signalStrength = aiReady?.signalStrength ?: if (confidence >= 74) "STRONG" else "MODERATE"
    val patternTag = aiReady?.patternTag ?: quant.patternTag

    val accent = sizeAccentColor(displaySize)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_prediction_card"),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.35f),
                        radius = size.maxDimension * 0.65f
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(size.width * 0.22f, size.height * 0.35f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Verdict",
                            tint = if (aiReady != null) IndigoAi else accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (aiReady != null) "GEMINI AI + QUANT CALL" else "QUANTITATIVE CALL",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (aiReady != null) IndigoAi else accent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianSurfaceHigh,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = "$signalStrength • $patternTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Giant LARGE / SMALL Callout + Primary & Backup Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = displaySize,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "sizeTransition"
                    ) { targetSize ->
                        val targetAccent = sizeAccentColor(targetSize)
                        Column {
                            Text(
                                text = targetSize.label,
                                style = MaterialTheme.typography.displayLarge,
                                color = targetAccent,
                                modifier = Modifier.testTag("predicted_size_text")
                            )
                            Text(
                                text = "DIGITS ${targetSize.rangeLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NumberOrb(number = displayNum, orbSize = 58.dp, isHighlighted = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TOP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NumberOrb(number = backupNum, orbSize = 44.dp, isHighlighted = false)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ALT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Confidence meter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SIGNAL CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$confidence%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = accent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { confidence / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accent,
                        trackColor = ObsidianSurfaceHigh
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAnalyzeAi,
                        enabled = uiState.aiState !is AiState.Analyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF070B14)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("analyze_ai_button")
                    ) {
                        if (uiState.aiState is AiState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF070B14)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyzing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Predict", fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenLogResult,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("log_result_button")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Result")
                    }

                    OutlinedButton(
                        onClick = onOpenPasteSheet,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("paste_data_button")
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste web data", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawSequenceRibbon(
    draws: List<DrawEntry>,
    quant: QuantAnalysisResult?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT DRAWS (NEWEST → OLDEST)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (quant != null) {
                    val streakColor = sizeAccentColor(quant.currentStreakSize)
                    Text(
                        text = "${quant.currentStreakLength}x ${quant.currentStreakSize.label} STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = streakColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(draws.take(20)) { idx, draw ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NumberOrb(
                            number = draw.number,
                            orbSize = if (idx == 0) 44.dp else 38.dp,
                            isHighlighted = idx == 0
                        )
                        Text(
                            text = if (draw.size == WingoSize.LARGE) "L" else "S",
                            style = MaterialTheme.typography.labelSmall,
                            color = sizeAccentColor(draw.size),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAndQuantMatrixCard(
    quant: QuantAnalysisResult?,
    aiState: AiState,
    onRetryAi: () -> Unit
) {
    if (quant == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = IndigoAi,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI & QUANT MATRIX",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                SizeCalloutPill(size = quant.predictedSize)
            }

            // AI Verdict Rationale / Status
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ObsidianSurfaceHigh,
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (aiState) {
                        is AiState.Idle -> {
                            Text(
                                text = quant.summaryLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiState.Analyzing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = IndigoAi
                                )
                                Text(
                                    text = "Gemini 3.8 Flash evaluating Markov transitions & streak reversal...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IndigoAi
                                )
                            }
                        }
                        is AiState.Ready -> {
                            Text(
                                text = aiState.verdict.analysisSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Model: ${aiState.verdict.modelName} • Pattern: ${aiState.verdict.patternTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoAi
                            )
                        }
                        is AiState.Error -> {
                            Text(
                                text = "Gemini AI unavailable: ${aiState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseLoss
                            )
                            Text(
                                text = "Quant Engine Fallback: ${quant.summaryLine}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onRetryAi,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Retry AI Analysis")
                            }
                        }
                    }
                }
            }

            // Large vs Small Probability Split
            ProbabilitySplitBar(
                largePct = quant.largeProbabilityPct,
                smallPct = quant.smallProbabilityPct
            )

            // Top-3 Numeric Candidates
            Text(
                text = "TOP 3 QUANT CANDIDATES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val maxScore = quant.top3.maxOfOrNull { it.score }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quant.top3.forEach { item ->
                    val size = WingoSize.fromNumber(item.number)
                    val accent = sizeAccentColor(size)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NumberOrb(number = item.number, orbSize = 32.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${size.label} • Freq ${item.frequency} • Gap ${item.lastSeenAgo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "%.1f".format(item.score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (item.score / maxScore).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accent,
                                trackColor = ObsidianSurfaceHigh
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedOnboardingCard(
    feedStatus: FeedStatus,
    onOpenPasteSheet: () -> Unit,
    onNavigateToFeedSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Connect Live Web Feed or Paste Draw Data",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            val statusMessage = when (feedStatus) {
                is FeedStatus.TokenRequired -> feedStatus.message
                is FeedStatus.Error -> feedStatus.message
                else -> "Stream real-time Wingo periods from api.55fiveapi.com using your Bearer Token, or paste recent draw digits to run AI Large/Small prediction."
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenPasteSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("empty_paste_button")
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Numbers")
                }
                OutlinedButton(
                    onClick = onNavigateToFeedSetup,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("empty_setup_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("API Setup")
                }
            }
        }
    }
}
