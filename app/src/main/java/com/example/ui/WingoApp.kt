package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PasteWebDataSheet
import com.example.ui.components.QuickLogResultSheet
import com.example.ui.components.formatRupiah
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeedSetupScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.WingoViewModel

enum class WingoTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    PREDICT("Predict", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_predict"),
    HISTORY("Session", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    FEED("Feed & Setup", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_feed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WingoApp(
    viewModel: WingoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(WingoTab.PREDICT) }
    var showLogSheet by rememberSaveable { mutableStateOf(false) }
    var showPasteSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (currentTab != WingoTab.PREDICT) {
        BackHandler {
            currentTab = WingoTab.PREDICT
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "WINGO PULSE AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    actions = {
                        Text(
                            text = formatRupiah(uiState.session.balance),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ObsidianSurface,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        WingoTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface
                    ) {
                        WingoTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Crossfade(
                    targetState = currentTab,
                    modifier = Modifier.weight(1f),
                    label = "tabCrossfade"
                ) { tab ->
                    when (tab) {
                        WingoTab.PREDICT -> DashboardScreen(
                            uiState = uiState,
                            isWideScreen = isWideScreen,
                            onRefreshLive = { viewModel.refreshLiveFeed(runAi = true) },
                            onToggleAutoPoll = { viewModel.toggleAutoPoll(it) },
                            onAnalyzeAi = { viewModel.triggerAiAnalysis() },
                            onOpenLogResult = { showLogSheet = true },
                            onOpenPasteSheet = { showPasteSheet = true },
                            onNavigateToFeedSetup = { currentTab = WingoTab.FEED }
                        )

                        WingoTab.HISTORY -> HistoryScreen(
                            uiState = uiState,
                            onPlayModeChange = { viewModel.updatePlayMode(it) }
                        )

                        WingoTab.FEED -> FeedSetupScreen(
                            uiState = uiState,
                            onSaveApiConfig = { token, typeId, interval, auto ->
                                viewModel.updateApiConfig(token, typeId, interval, auto)
                                currentTab = WingoTab.PREDICT
                            },
                            onApplyPastedData = { raw ->
                                if (viewModel.applyPastedWebData(raw)) {
                                    currentTab = WingoTab.PREDICT
                                }
                            },
                            onSaveBankroll = { bal, min, tgt ->
                                viewModel.updateBankrollSettings(bal, min, tgt)
                            },
                            onResetSession = {
                                viewModel.resetSession()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLogSheet) {
        QuickLogResultSheet(
            currentIssue = uiState.currentIssue,
            onDismiss = { showLogSheet = false },
            onSelectNumber = { digit -> viewModel.recordObservedResult(digit) }
        )
    }

    if (showPasteSheet) {
        PasteWebDataSheet(
            initialText = uiState.session.manualNumbersCsv,
            onDismiss = { showPasteSheet = false },
            onApply = { raw -> viewModel.applyPastedWebData(raw) }
        )
    }
}
