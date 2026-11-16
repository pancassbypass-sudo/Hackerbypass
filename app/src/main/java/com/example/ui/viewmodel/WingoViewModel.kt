package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.WingoAiRepository
import com.example.data.local.GameHistoryRepository
import com.example.data.local.GameRecordEntity
import com.example.data.local.SessionPreferencesRepository
import com.example.data.local.WingoDatabase
import com.example.data.remote.LiveWebResult
import com.example.data.remote.WingoRemoteRepository
import com.example.domain.AiState
import com.example.domain.DrawEntry
import com.example.domain.FeedStatus
import com.example.domain.PlayMode
import com.example.domain.QuantAnalysisResult
import com.example.domain.SessionState
import com.example.domain.WingoQuantEngine
import com.example.domain.WingoSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WingoUiState(
    val session: SessionState = SessionState(),
    val currentIssue: String = WingoRemoteRepository.fallbackIssueString(),
    val draws: List<DrawEntry> = emptyList(),
    val quantResult: QuantAnalysisResult? = null,
    val aiState: AiState = AiState.Idle,
    val feedStatus: FeedStatus = FeedStatus.Idle,
    val history: List<GameRecordEntity> = emptyList(),
    val statusBanner: String? = null
)

class WingoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = SessionPreferencesRepository(application)
    private val historyRepo = GameHistoryRepository(WingoDatabase.getInstance(application).gameRecordDao())
    private val remoteRepo = WingoRemoteRepository()
    private val aiRepo = WingoAiRepository()

    private val _uiState = MutableStateFlow(WingoUiState())
    val uiState: StateFlow<WingoUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var aiJob: Job? = null
    private var initializedFeed = false

    init {
        viewModelScope.launch {
            prefsRepo.sessionFlow.collectLatest { session ->
                _uiState.update { it.copy(session = session) }

                if (!initializedFeed) {
                    initializedFeed = true
                    if (session.bearerToken.isNotBlank()) {
                        refreshLiveFeed(runAi = true)
                    } else if (session.manualNumbersCsv.isNotBlank()) {
                        applyManualFeedInternal(session.manualNumbersCsv, saveToPrefs = false, runAi = true)
                    } else {
                        _uiState.update {
                            it.copy(
                                feedStatus = FeedStatus.TokenRequired(
                                    "Add your 55Five Bearer Token or paste recent web draw numbers in Feed & Setup."
                                )
                            )
                        }
                    }
                }

                syncAutoPolling(session.autoPollEnabled, session.pollIntervalSec)
            }
        }

        viewModelScope.launch {
            historyRepo.records.collectLatest { records ->
                _uiState.update { it.copy(history = records) }
            }
        }
    }

    private fun syncAutoPolling(enabled: Boolean, intervalSec: Int) {
        if (enabled && _uiState.value.session.bearerToken.isNotBlank()) {
            if (pollJob?.isActive == true) return
            pollJob = viewModelScope.launch {
                while (isActive) {
                    refreshLiveFeed(runAi = true, silentIfSameIssue = true)
                    delay(intervalSec.coerceIn(2, 30) * 1000L)
                }
            }
        } else {
            pollJob?.cancel()
            pollJob = null
        }
    }

    fun refreshLiveFeed(runAi: Boolean = true, silentIfSameIssue: Boolean = false) {
        val currentSession = _uiState.value.session
        viewModelScope.launch {
            if (!silentIfSameIssue) {
                _uiState.update { it.copy(feedStatus = FeedStatus.Loading) }
            }

            when (val result = remoteRepo.fetchLiveFeed(currentSession.bearerToken, currentSession.typeId)) {
                is LiveWebResult.Success -> {
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val previousIssue = _uiState.value.currentIssue
                    val isNewIssue = result.currentIssue != previousIssue || _uiState.value.draws.isEmpty()

                    evaluatePendingWithDraws(result.draws)

                    val quant = WingoQuantEngine.analyze(result.draws.map { it.number })
                    _uiState.update {
                        it.copy(
                            currentIssue = result.currentIssue,
                            draws = result.draws,
                            quantResult = quant,
                            feedStatus = FeedStatus.LiveConnected(timeStr, result.draws.size),
                            statusBanner = null
                        )
                    }

                    if (runAi && quant != null && (isNewIssue || _uiState.value.aiState !is AiState.Ready)) {
                        runAiPrediction(result.currentIssue, quant, lockRecord = true)
                    }
                }

                is LiveWebResult.AuthRequired -> {
                    _uiState.update {
                        it.copy(feedStatus = FeedStatus.TokenRequired(result.reason))
                    }
                }

                is LiveWebResult.Failure -> {
                    _uiState.update {
                        it.copy(feedStatus = FeedStatus.Error(result.reason))
                    }
                }
            }
        }
    }

    fun applyPastedWebData(rawInput: String): Boolean {
        val parsed = remoteRepo.parsePastedWebData(rawInput)
        if (parsed.isEmpty()) {
            _uiState.update {
                it.copy(statusBanner = "No valid digits (0–9) or 55Five JSON found in pasted data.")
            }
            return false
        }
        viewModelScope.launch {
            applyManualFeedInternal(rawInput, saveToPrefs = true, runAi = true)
        }
        return true
    }

    private suspend fun applyManualFeedInternal(
        rawInput: String,
        saveToPrefs: Boolean,
        runAi: Boolean
    ) {
        val parsed = remoteRepo.parsePastedWebData(rawInput)
        if (parsed.isEmpty()) return

        if (saveToPrefs) {
            val updated = _uiState.value.session.copy(manualNumbersCsv = rawInput)
            prefsRepo.saveSession(updated)
        }

        val nextIssue = parsed.firstOrNull()?.issueNumber?.let {
            WingoRemoteRepository.incrementIssue(it)
        } ?: WingoRemoteRepository.fallbackIssueString()

        evaluatePendingWithDraws(parsed)

        val quant = WingoQuantEngine.analyze(parsed.map { it.number })
        _uiState.update {
            it.copy(
                currentIssue = nextIssue,
                draws = parsed,
                quantResult = quant,
                feedStatus = FeedStatus.ManualActive(
                    count = parsed.size,
                    note = "Web/Manual feed active (${parsed.size} draws)"
                ),
                statusBanner = null
            )
        }

        if (runAi && quant != null) {
            runAiPrediction(nextIssue, quant, lockRecord = true)
        }
    }

    fun triggerAiAnalysis() {
        val quant = _uiState.value.quantResult ?: return
        runAiPrediction(_uiState.value.currentIssue, quant, lockRecord = true)
    }

    private fun runAiPrediction(
        issue: String,
        quant: QuantAnalysisResult,
        lockRecord: Boolean
    ) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiState.Analyzing) }
            val result = aiRepo.analyzePrediction(issue, quant)
            result.onSuccess { verdict ->
                _uiState.update { it.copy(aiState = AiState.Ready(verdict)) }
                if (lockRecord) {
                    savePendingPrediction(
                        period = issue,
                        predictedSize = verdict.predictedSize,
                        predictedNum = verdict.predictedNumber,
                        backupNum = verdict.backupNumber,
                        confidence = verdict.confidence,
                        pattern = verdict.patternTag,
                        reasoning = verdict.analysisSummary
                    )
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _uiState.update {
                    it.copy(
                        aiState = AiState.Error(
                            err.localizedMessage ?: "Firebase AI analysis failed."
                        )
                    )
                }
                // Still lock quantitative prediction if no pending record exists for this period
                if (lockRecord) {
                    savePendingPrediction(
                        period = issue,
                        predictedSize = quant.predictedSize,
                        predictedNum = quant.predictedNumber,
                        backupNum = quant.backupNumber,
                        confidence = quant.confidence,
                        pattern = quant.patternTag,
                        reasoning = quant.summaryLine
                    )
                }
            }
        }
    }

    private suspend fun savePendingPrediction(
        period: String,
        predictedSize: WingoSize,
        predictedNum: Int,
        backupNum: Int,
        confidence: Int,
        pattern: String,
        reasoning: String
    ) {
        val session = _uiState.value.session
        if (session.lastIssue == period) return

        val record = GameRecordEntity(
            period = period,
            actualNum = -1,
            predictedSize = predictedSize.label,
            predictedNum = predictedNum,
            backupNum = backupNum,
            confidence = confidence,
            pattern = pattern,
            aiReasoning = reasoning,
            betAmount = session.currentBet,
            status = "PENDING",
            profitLoss = 0L
        )
        historyRepo.upsert(record)
        prefsRepo.saveSession(session.copy(lastIssue = period))
    }

    /**
     * Allows user to manually record an observed result digit (0..9) when feeding draws manually.
     */
    fun recordObservedResult(actualNumber: Int) {
        if (actualNumber !in 0..9) return
        viewModelScope.launch {
            val currentIssue = _uiState.value.currentIssue
            val newDraw = DrawEntry(issueNumber = currentIssue, number = actualNumber)
            val updatedDraws = (listOf(newDraw) + _uiState.value.draws).take(40)
            val csv = updatedDraws.joinToString(", ") { it.number.toString() }

            evaluatePendingWithDraws(updatedDraws)

            val nextIssue = WingoRemoteRepository.incrementIssue(currentIssue)
            val quant = WingoQuantEngine.analyze(updatedDraws.map { it.number })
            val updatedSession = _uiState.value.session.copy(manualNumbersCsv = csv)
            prefsRepo.saveSession(updatedSession)

            _uiState.update {
                it.copy(
                    currentIssue = nextIssue,
                    draws = updatedDraws,
                    quantResult = quant,
                    feedStatus = FeedStatus.ManualActive(updatedDraws.size, "Manual stream (${updatedDraws.size} draws)")
                )
            }

            if (quant != null) {
                runAiPrediction(nextIssue, quant, lockRecord = true)
            }
        }
    }

    private suspend fun evaluatePendingWithDraws(draws: List<DrawEntry>) {
        val pending = historyRepo.getPending()
        if (pending.isEmpty()) return

        val drawMap = draws.associateBy { it.issueNumber }
        var session = _uiState.value.session
        var changed = false

        for (rec in pending) {
            val matched = drawMap[rec.period] ?: continue
            val actualNum = matched.number
            val actualSize = WingoSize.fromNumber(actualNum)

            val isWin = when (session.playMode) {
                PlayMode.LARGE_SMALL -> actualSize.label == rec.predictedSize
                PlayMode.SINGLE_NUMBER -> actualNum == rec.predictedNum
            }

            val pnl = if (isWin) {
                (rec.betAmount * session.playMode.netMultiplier).toLong()
            } else {
                -rec.betAmount
            }

            historyRepo.upsert(
                rec.copy(
                    actualNum = actualNum,
                    status = if (isWin) "WIN" else "LOSS",
                    profitLoss = pnl
                )
            )

            session = if (isWin) {
                session.copy(
                    balance = session.balance + pnl,
                    profit = session.profit + pnl,
                    wins = session.wins + 1,
                    consecLoss = 0,
                    consecWin = session.consecWin + 1,
                    betIndex = 0
                )
            } else {
                session.copy(
                    balance = (session.balance - rec.betAmount).coerceAtLeast(0L),
                    profit = session.profit - rec.betAmount,
                    losses = session.losses + 1,
                    consecLoss = session.consecLoss + 1,
                    consecWin = 0,
                    betIndex = (session.betIndex + 1).coerceAtMost(session.betSteps.lastIndex)
                )
            }
            changed = true
        }

        if (changed) {
            prefsRepo.saveSession(session)
        }
    }

    fun updateApiConfig(token: String, typeId: Int, pollIntervalSec: Int, autoPoll: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.session.copy(
                bearerToken = token.trim(),
                typeId = typeId,
                pollIntervalSec = pollIntervalSec.coerceIn(2, 30),
                autoPollEnabled = autoPoll
            )
            prefsRepo.saveSession(updated)
            if (token.isNotBlank()) {
                refreshLiveFeed(runAi = true)
            }
        }
    }

    fun toggleAutoPoll(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.session.copy(autoPollEnabled = enabled)
            prefsRepo.saveSession(updated)
        }
    }

    fun updatePlayMode(mode: PlayMode) {
        viewModelScope.launch {
            val updated = _uiState.value.session.copy(playMode = mode)
            prefsRepo.saveSession(updated)
        }
    }

    fun updateBankrollSettings(balance: Long, minBet: Long, targetProfit: Long) {
        viewModelScope.launch {
            val safeBalance = balance.coerceAtLeast(1_000L)
            val safeMinBet = minBet.coerceAtLeast(100L)
            val safeTarget = targetProfit.coerceAtLeast(safeMinBet)
            val updated = _uiState.value.session.copy(
                balance = safeBalance,
                initialBalance = safeBalance,
                profit = 0L,
                wins = 0,
                losses = 0,
                consecLoss = 0,
                consecWin = 0,
                betIndex = 0,
                minBet = safeMinBet,
                targetProfit = safeTarget
            )
            prefsRepo.saveSession(updated)
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            historyRepo.clear()
            val cur = _uiState.value.session
            prefsRepo.saveSession(
                cur.copy(
                    balance = cur.initialBalance,
                    profit = 0L,
                    wins = 0,
                    losses = 0,
                    consecLoss = 0,
                    consecWin = 0,
                    betIndex = 0,
                    lastIssue = ""
                )
            )
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }
}
