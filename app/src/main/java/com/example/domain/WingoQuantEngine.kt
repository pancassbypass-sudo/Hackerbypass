package com.example.domain

import kotlin.math.roundToInt

/**
 * Multi-factor quantitative engine upgraded from the Python MultiBaseEngine.
 * Numbers are ordered newest-first (index 0 = most recent draw).
 */
object WingoQuantEngine {
    private const val LOOKBACK = 40

    fun analyze(numbersInput: List<Int>): QuantAnalysisResult? {
        val raw = numbersInput.filter { it in 0..9 }.take(LOOKBACK)
        if (raw.isEmpty()) return null

        val sizes = raw.map { WingoSize.fromNumber(it) }
        val freqMap = IntArray(10)
        raw.forEach { freqMap[it]++ }

        val baseScores = DoubleArray(10)
        val lastSeenMap = IntArray(10)

        // 1. Frequency + Recency Deficit (digits absent longer get higher recovery weight)
        for (n in 0..9) {
            val freq = freqMap[n]
            val idx = raw.indexOf(n)
            val lastPos = if (idx >= 0) idx else 45
            lastSeenMap[n] = lastPos
            baseScores[n] = freq * 1.45 + lastPos * 0.58
        }

        // 2. Streak Detection & Mean Reversion Pressure
        val currentStreakSize = sizes.first()
        var streakLen = 1
        for (i in 1 until sizes.size) {
            if (sizes[i] == currentStreakSize) streakLen++ else break
        }

        if (streakLen >= 3) {
            val reversalBoost = if (streakLen >= 4) 8.5 + streakLen * 1.4 else 4.0
            if (currentStreakSize == WingoSize.LARGE) {
                for (n in 0..4) baseScores[n] += reversalBoost
            } else {
                for (n in 5..9) baseScores[n] += reversalBoost
            }
        } else if (streakLen == 2) {
            // Mild momentum continuation bias on short 2-streaks
            val momentumBoost = 1.8
            if (currentStreakSize == WingoSize.LARGE) {
                for (n in 5..9) baseScores[n] += momentumBoost
            } else {
                for (n in 0..4) baseScores[n] += momentumBoost
            }
        }

        // 3. 1st-Order Markov Transition (what number typically followed raw[0] in history?)
        // Remember raw is newest-first: raw[i+1] preceded raw[i], so raw[i] is the draw AFTER raw[i+1].
        val latestNum = raw[0]
        val nextCandidates = mutableListOf<Int>()
        for (i in 1 until raw.size) {
            if (raw[i] == latestNum) {
                nextCandidates.add(raw[i - 1])
            }
        }
        var markovBestNext: Int? = null
        if (nextCandidates.isNotEmpty()) {
            val counts = nextCandidates.groupingBy { it }.eachCount()
            val topEntry = counts.maxByOrNull { it.value }
            if (topEntry != null) {
                markovBestNext = topEntry.key
                counts.forEach { (digit, count) ->
                    baseScores[digit] += count * 5.5
                }
            }
        }

        // 4. 2nd-Order Size Markov (Big/Small pair transition)
        if (sizes.size >= 4) {
            val s0 = sizes[0]
            val s1 = sizes[1]
            var largeAfterPair = 0
            var smallAfterPair = 0
            for (i in 1 until sizes.size - 1) {
                if (sizes[i] == s0 && sizes[i + 1] == s1) {
                    if (sizes[i - 1] == WingoSize.LARGE) largeAfterPair++ else smallAfterPair++
                }
            }
            if (largeAfterPair != smallAfterPair) {
                val boost = ((largeAfterPair - smallAfterPair).let { kotlin.math.abs(it) }) * 2.5
                if (largeAfterPair > smallAfterPair) {
                    for (n in 5..9) baseScores[n] += boost
                } else {
                    for (n in 0..4) baseScores[n] += boost
                }
            }
        }

        // 5. Rolling Window Size Imbalance (Last 10 draws)
        val recent10 = sizes.take(10)
        val largeIn10 = recent10.count { it == WingoSize.LARGE }
        val smallIn10 = recent10.size - largeIn10
        if (recent10.size >= 6) {
            val imbalance = largeIn10 - smallIn10
            if (imbalance >= 4) {
                for (n in 0..4) baseScores[n] += imbalance * 1.2
            } else if (imbalance <= -4) {
                for (n in 5..9) baseScores[n] += (-imbalance) * 1.2
            }
        }

        // Build ranked scores
        val allScores = (0..9).map { n ->
            QuantScore(
                number = n,
                score = baseScores[n],
                frequency = freqMap[n],
                lastSeenAgo = lastSeenMap[n]
            )
        }.sortedByDescending { it.score }

        val smallTotalWeight = allScores.filter { it.number <= 4 }.sumOf { it.score }.coerceAtLeast(1.0)
        val largeTotalWeight = allScores.filter { it.number >= 5 }.sumOf { it.score }.coerceAtLeast(1.0)
        val totalWeight = smallTotalWeight + largeTotalWeight

        val rawLargePct = ((largeTotalWeight / totalWeight) * 100.0).roundToInt().coerceIn(8, 92)
        val rawSmallPct = 100 - rawLargePct

        val predictedSize = if (rawLargePct >= rawSmallPct) WingoSize.LARGE else WingoSize.SMALL
        val sizeFilteredTop = allScores.filter { WingoSize.fromNumber(it.number) == predictedSize }
        val primaryDigit = sizeFilteredTop.firstOrNull()?.number ?: allScores.first().number
        val backupDigit = sizeFilteredTop.getOrNull(1)?.number ?: allScores.getOrNull(1)?.number ?: primaryDigit

        val sizeMargin = kotlin.math.abs(rawLargePct - rawSmallPct)
        val topMargin = if (allScores.size >= 2) allScores[0].score - allScores[1].score else 2.0
        val confidence = (54 + (sizeMargin * 0.75).roundToInt() + (topMargin * 1.4).roundToInt())
            .coerceIn(52, 92)

        val patternTag = when {
            streakLen >= 4 -> "REVERSAL-${currentStreakSize.label}x$streakLen"
            markovBestNext != null -> "MARKOV-CHAIN"
            kotlin.math.abs(largeIn10 - smallIn10) >= 4 -> "ZONE-BALANCE"
            else -> "MULTI-FACTOR"
        }

        val summaryLine = "Streak ${currentStreakSize.label} x$streakLen • 10-Draw L/S: $largeIn10/$smallIn10" +
            (markovBestNext?.let { " • Markov->$it" } ?: "")

        return QuantAnalysisResult(
            recentNumbers = raw,
            predictedSize = predictedSize,
            largeProbabilityPct = rawLargePct,
            smallProbabilityPct = rawSmallPct,
            predictedNumber = primaryDigit,
            backupNumber = backupDigit,
            confidence = confidence,
            currentStreakSize = currentStreakSize,
            currentStreakLength = streakLen,
            top3 = allScores.take(3),
            allScores = allScores,
            patternTag = patternTag,
            summaryLine = summaryLine,
            markovBestNext = markovBestNext
        )
    }
}
