package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.WingoGameType
import com.example.ui.components.FeedStatusBadge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoseLoss
import com.example.ui.viewmodel.WingoUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedSetupScreen(
    uiState: WingoUiState,
    onSaveApiConfig: (token: String, typeId: Int, intervalSec: Int, autoPoll: Boolean) -> Unit,
    onApplyPastedData: (String) -> Unit,
    onSaveBankroll: (balance: Long, minBet: Long, targetProfit: Long) -> Unit,
    onResetSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.session
    var tokenInput by remember(session.bearerToken) { mutableStateOf(session.bearerToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    var selectedTypeId by remember(session.typeId) { mutableIntStateOf(session.typeId) }
    var pollInterval by remember(session.pollIntervalSec) { mutableIntStateOf(session.pollIntervalSec) }
    var autoPoll by remember(session.autoPollEnabled) { mutableStateOf(session.autoPollEnabled) }

    var pastedText by remember(session.manualNumbersCsv) { mutableStateOf(session.manualNumbersCsv) }

    var balanceText by remember(session.initialBalance) { mutableStateOf(session.initialBalance.toString()) }
    var minBetText by remember(session.minBet) { mutableStateOf(session.minBet.toString()) }
    var targetText by remember(session.targetProfit) { mutableStateOf(session.targetProfit.toString()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 840.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live 55Five Web API Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, tint = ElectricCyan)
                            Text("55Five Live Web API", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        FeedStatusBadge(feedStatus = uiState.feedStatus)
                    }

                    Text(
                        text = "Connects directly to api.55fiveapi.com/api/webapi (GetGameIssue & GetNoaverageEmerdList).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Bearer Token") },
                        placeholder = { Text("Paste your 55Five Bearer Token") },
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle token visibility"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bearer_token_input")
                    )

                    Text("Game Room (typeId)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WingoGameType.entries.forEach { room ->
                            FilterChip(
                                selected = selectedTypeId == room.typeId,
                                onClick = { selectedTypeId = room.typeId },
                                label = { Text(room.displayName) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-poll live rounds (${pollInterval}s)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoPoll,
                            onCheckedChange = { autoPoll = it }
                        )
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 5, 10).forEach { sec ->
                            FilterChip(
                                selected = pollInterval == sec,
                                onClick = { pollInterval = sec },
                                label = { Text("${sec}s") }
                            )
                        }
                    }

                    Button(
                        onClick = { onSaveApiConfig(tokenInput, selectedTypeId, pollInterval, autoPoll) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_api_button")
                    ) {
                        Text("Save & Sync Live API", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Paste Web Draw Data Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = EmeraldPulse)
                        Text("Paste Web Draw History", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text(
                        text = "Paste comma-separated draw digits (newest first) or raw GetNoaverageEmerdList JSON from the web feed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text("Web Draw Sequence or JSON") },
                        placeholder = { Text("e.g. 4, 9, 2, 1, 7, 8, 3, 0, 6, 5") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_paste_input")
                    )
                    Button(
                        onClick = { onApplyPastedData(pastedText) },
                        enabled = pastedText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_pasted_data_button")
                    ) {
                        Text("Run AI Analysis on Pasted Data", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Bankroll & Martingale Wizard
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Savings, contentDescription = null, tint = ElectricCyan)
                        Text("Bankroll & Bet Ladder", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { balanceText = it.filter { c -> c.isDigit() } },
                            label = { Text("Initial Balance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minBetText,
                            onValueChange = { minBetText = it.filter { c -> c.isDigit() } },
                            label = { Text("Base Bet") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Profit (WD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val bal = balanceText.toLongOrNull() ?: 20_000L
                                val min = minBetText.toLongOrNull() ?: 5_000L
                                val tgt = targetText.toLongOrNull() ?: (bal * 4L)
                                onSaveBankroll(bal, min, tgt)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_bankroll_button")
                        ) {
                            Text("Apply Bankroll")
                        }
                        OutlinedButton(
                            onClick = onResetSession,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reset_session_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = RoseLoss)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", color = RoseLoss)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
