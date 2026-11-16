package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameRecordEntity
import com.example.domain.PlayMode
import com.example.domain.SessionState
import com.example.domain.WingoSize
import com.example.ui.components.NumberOrb
import com.example.ui.components.SizeCalloutPill
import com.example.ui.components.formatRupiah
import com.example.ui.theme.AmberLarge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RoseLoss
import com.example.ui.viewmodel.WingoUiState

@Composable
fun HistoryScreen(
    uiState: WingoUiState,
    onPlayModeChange: (PlayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SessionBankrollCard(
                    session = uiState.session,
                    onPlayModeChange = onPlayModeChange
                )
            }

            item {
                Text(
                    text = "PREDICTION HISTORY (${uiState.history.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.history.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No evaluated periods yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Predictions are logged automatically whenever a new period is analyzed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.history, key = { it.period }) { record ->
                    HistoryRecordCard(record = record)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SessionBankrollCard(
    session: SessionState,
    onPlayModeChange: (PlayMode) -> Unit
) {
    val pnlColor = if (session.profit >= 0L) EmeraldPulse else RoseLoss

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Text(
                        text = "SESSION BANKROLL",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Text(
                    text = "WR ${"%.1f".format(session.winRatePct)}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = AmberLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(session.balance),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET P&L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(session.profit, showPlus = true),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = pnlColor
                    )
                }
            }

            // Target Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TARGET WD: ${formatRupiah(session.targetProfit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(session.progressPct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPulse,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { session.progressPct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPulse,
                    trackColor = ObsidianSurfaceHigh
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill("WIN", session.wins.toString(), EmeraldPulse)
                StatPill("LOSS", session.losses.toString(), RoseLoss)
                StatPill("STREAK LOSS", "${session.consecLoss}x", AmberLarge)
                StatPill("BET LVL", "${session.betIndex + 1}/8", ElectricCyan)
            }

            // Evaluation Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = session.playMode == mode,
                        onClick = { onPlayModeChange(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("play_mode_${mode.name}")
                    )
                }
            }

            // Martingale Ladder Preview
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(session.betSteps) { index, stepAmount ->
                    val active = index == session.betIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurfaceHigh,
                        border = BorderStroke(1.dp, if (active) ElectricCyan else ObsidianBorder)
                    ) {
                        Text(
                            text = "L${index + 1}: ${formatRupiah(stepAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = color
        )
    }
}

@Composable
private fun HistoryRecordCard(record: GameRecordEntity) {
    val statusColor = when (record.status) {
        "WIN" -> EmeraldPulse
        "LOSS" -> RoseLoss
        else -> AmberLarge
    }
    val predictedSize = if (record.predictedSize == "LARGE") WingoSize.LARGE else WingoSize.SMALL

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${record.period.takeLast(10)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    SizeCalloutPill(size = predictedSize)
                    Text(
                        text = "Digit ${record.predictedNum} (${record.confidence}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${record.pattern} • Bet ${formatRupiah(record.betAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (record.actualNum >= 0) {
                    NumberOrb(number = record.actualNum, orbSize = 34.dp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = record.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (record.status == "PENDING") "..." else formatRupiah(record.profitLoss, showPlus = true),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
        }
    }
}
