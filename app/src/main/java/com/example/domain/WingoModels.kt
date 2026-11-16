package com.example.domain

enum class WingoSize(val label: String, val rangeLabel: String) {
    LARGE("LARGE", "5–9"),
    SMALL("SMALL", "0–4");

    companion object {
        fun fromNumber(number: Int): WingoSize = if (number >= 5) LARGE else SMALL
    }
}

enum class PlayMode(val label: String, val netMultiplier: Double) {
    LARGE_SMALL("Large / Small (1.96x)", 0.96),
    SINGLE_NUMBER("Single Digit (9.0x)", 8.5)
}

enum class WingoGameType(val typeId: Int, val displayName: String) {
    WINGO_1M(1, "WinGo 1 Min"),
    WINGO_3M(2, "WinGo 3 Min"),
    WINGO_5M(3, "WinGo 5 Min"),
    WINGO_30S(30, "WinGo 30 Sec")
}

data class DrawEntry(
    val issueNumber: String,
    val number: Int,
    val colour: String = ""
) {
    val size: WingoSize get() = WingoSize.fromNumber(number)
}

data class QuantScore(
    val number: Int,
    val score: Double,
    val frequency: Int,
    val lastSeenAgo: Int
)

data class QuantAnalysisResult(
    val recentNumbers: List<Int>,
    val predictedSize: WingoSize,
    val largeProbabilityPct: Int,
    val smallProbabilityPct: Int,
    val predictedNumber: Int,
    val backupNumber: Int,
    val confidence: Int,
    val currentStreakSize: WingoSize,
    val currentStreakLength: Int,
    val top3: List<QuantScore>,
    val allScores: List<QuantScore>,
    val patternTag: String,
    val summaryLine: String,
    val markovBestNext: Int?
)

data class AiPredictionVerdict(
    val predictedSize: WingoSize,
    val predictedNumber: Int,
    val backupNumber: Int,
    val confidence: Int,
    val signalStrength: String,
    val patternTag: String,
    val analysisSummary: String,
    val modelName: String = "gemini-3.8-flash",
    val generatedAtMs: Long = System.currentTimeMillis()
)

sealed interface AiState {
    data object Idle : AiState
    data object Analyzing : AiState
    data class Ready(val verdict: AiPredictionVerdict) : AiState
    data class Error(val message: String) : AiState
}

sealed interface FeedStatus {
    data object Idle : FeedStatus
    data object Loading : FeedStatus
    data class LiveConnected(val lastSyncTime: String, val count: Int) : FeedStatus
    data class ManualActive(val count: Int, val note: String) : FeedStatus
    data class TokenRequired(val message: String) : FeedStatus
    data class Error(val message: String) : FeedStatus
}

data class SessionState(
    val bearerToken: String = "",
    val typeId: Int = 1,
    val playMode: PlayMode = PlayMode.LARGE_SMALL,
    val balance: Long = 20_000L,
    val initialBalance: Long = 20_000L,
    val profit: Long = 0L,
    val wins: Int = 0,
    val losses: Int = 0,
    val consecLoss: Int = 0,
    val consecWin: Int = 0,
    val betIndex: Int = 0,
    val lastIssue: String = "",
    val minBet: Long = 5_000L,
    val targetProfit: Long = 80_000L,
    val pollIntervalSec: Int = 3,
    val autoPollEnabled: Boolean = false,
    val manualNumbersCsv: String = ""
) {
    val betSteps: List<Long>
        get() = List(8) { i -> minBet * (1L shl i) }

    val currentBet: Long
        get() = betSteps[betIndex.coerceIn(0, betSteps.lastIndex)]

    val totalGames: Int
        get() = wins + losses

    val winRatePct: Float
        get() = if (totalGames == 0) 0f else (wins.toFloat() / totalGames.toFloat()) * 100f

    val progressPct: Float
        get() = if (targetProfit <= 0L) 0f else ((profit.coerceAtLeast(0L).toFloat() / targetProfit.toFloat()) * 100f).coerceIn(0f, 100f)

    val isTargetReached: Boolean
        get() = targetProfit > 0L && profit >= targetProfit

    val isLowBalance: Boolean
        get() = balance < minBet
}
