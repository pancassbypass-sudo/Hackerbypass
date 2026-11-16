package com.example.data.ai

import android.util.Log
import com.example.domain.AiPredictionVerdict
import com.example.domain.QuantAnalysisResult
import com.example.domain.WingoSize
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "WingoAiRepository"

class WingoAiRepository {

    private val predictionSchema = Schema.obj(
        properties = mapOf(
            "predictedSize" to Schema.enumeration(
                values = listOf("LARGE", "SMALL"),
                description = "Primary predicted outcome category: LARGE (digits 5-9) or SMALL (digits 0-4)"
            ),
            "predictedNumber" to Schema.integer(
                description = "Most likely single digit from 0 to 9 matching the predictedSize"
            ),
            "backupNumber" to Schema.integer(
                description = "Secondary single digit from 0 to 9 matching the predictedSize"
            ),
            "confidence" to Schema.integer(
                description = "Calibrated confidence percentage between 52 and 94"
            ),
            "signalStrength" to Schema.enumeration(
                values = listOf("STRONG", "MODERATE", "CAUTIOUS"),
                description = "Strength of the combined quantitative and pattern signal"
            ),
            "patternTag" to Schema.string(
                description = "Short 2-3 word uppercase pattern label like MEAN-REVERSION or MARKOV-CLUSTER"
            ),
            "analysisSummary" to Schema.string(
                description = "Concise 2-sentence analytical explanation referencing streak, recency, and Markov signals"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = predictionSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun analyzePrediction(
        issuePeriod: String,
        quant: QuantAnalysisResult
    ): Result<AiPredictionVerdict> {
        val newestFirstStr = quant.recentNumbers.take(25).joinToString(", ") {
            "$it (${if (it >= 5) "L" else "S"})"
        }
        val top3Str = quant.top3.joinToString(" | ") {
            "Digit ${it.number} [${WingoSize.fromNumber(it.number).label}]: score=${"%.1f".format(it.score)}, freq=${it.frequency}, gap=${it.lastSeenAgo}"
        }

        val prompt = """
            You are a quantitative sequence analyst evaluating Wingo draw telemetry for period $issuePeriod.
            Rules:
            - SMALL = digits 0, 1, 2, 3, 4
            - LARGE = digits 5, 6, 7, 8, 9
            - Sequence below is ordered NEWEST FIRST (index 0 is the latest completed draw):
            [$newestFirstStr]

            Pre-computed statistical features:
            - Active Streak: ${quant.currentStreakSize.label} x${quant.currentStreakLength}
            - Quantitative Large vs Small split: LARGE ${quant.largeProbabilityPct}% vs SMALL ${quant.smallProbabilityPct}%
            - Quantitative Top-3 Digits: $top3Str
            - Markov 1st-order next-digit candidate from latest digit (${quant.recentNumbers.firstOrNull()}): ${quant.markovBestNext ?: "None"}
            - Summary: ${quant.summaryLine}

            Analyze mean-reversion pressure, Markov transitions, cluster momentum, and zone balance.
            Choose the strongest LARGE or SMALL call, a primary single digit (0-9) inside that size, a backup digit inside that size, a realistic confidence (52-94), and provide a sharp 2-sentence rationale.
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(rawJson)
            val sizeStr = json.optString("predictedSize", quant.predictedSize.name)
            val size = if (sizeStr.equals("LARGE", ignoreCase = true)) WingoSize.LARGE else WingoSize.SMALL

            var primaryNum = json.optInt("predictedNumber", quant.predictedNumber).coerceIn(0, 9)
            // Keep digit aligned with LARGE (5-9) / SMALL (0-4)
            if (WingoSize.fromNumber(primaryNum) != size) {
                primaryNum = quant.allScores.first { WingoSize.fromNumber(it.number) == size }.number
            }

            var backupNum = json.optInt("backupNumber", quant.backupNumber).coerceIn(0, 9)
            if (WingoSize.fromNumber(backupNum) != size || backupNum == primaryNum) {
                backupNum = quant.allScores
                    .firstOrNull { WingoSize.fromNumber(it.number) == size && it.number != primaryNum }
                    ?.number ?: primaryNum
            }

            val verdict = AiPredictionVerdict(
                predictedSize = size,
                predictedNumber = primaryNum,
                backupNumber = backupNum,
                confidence = json.optInt("confidence", quant.confidence).coerceIn(50, 95),
                signalStrength = json.optString("signalStrength", "MODERATE"),
                patternTag = json.optString("patternTag", quant.patternTag).take(26),
                analysisSummary = json.optString("analysisSummary", quant.summaryLine)
            )
            Result.success(verdict)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI analysis timed out", e)
            Result.failure(Exception("AI analysis timed out after 45s. Tap Analyze to retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI analysis failed", e)
            Result.failure(e)
        }
    }
}
