package com.example.data.remote

import com.example.domain.DrawEntry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GetGameIssueRequest(
    val typeId: Int = 1,
    val language: Int = 1,
    val random: String = "970465f2d1ca4742982826a945c7839e",
    val signature: String = "5232F77555C8A357217CC8521D7D1B5A",
    val timestamp: Long = System.currentTimeMillis() / 1000L
)

@JsonClass(generateAdapter = true)
data class GetEmerdListRequest(
    val typeId: Int = 1,
    val language: Int = 1,
    val pageSize: Int = 30,
    val pageNo: Int = 1,
    val random: String = "b422565265dd46b2bb9a6ccf82b3b8bc",
    val signature: String = "EADB3378CB0A8CD3551CC67CE66091EC",
    val timestamp: Long = System.currentTimeMillis() / 1000L
)

@JsonClass(generateAdapter = true)
data class GameIssueResponse(
    val code: Int = -1,
    val msg: String? = null,
    val data: GameIssueData? = null
)

@JsonClass(generateAdapter = true)
data class GameIssueData(
    val issueNumber: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)

@JsonClass(generateAdapter = true)
data class EmerdListResponse(
    val code: Int = -1,
    val msg: String? = null,
    val data: EmerdListData? = null
)

@JsonClass(generateAdapter = true)
data class EmerdListData(
    @Json(name = "list") val list: List<EmerdListItem>? = null
)

@JsonClass(generateAdapter = true)
data class EmerdListItem(
    val issueNumber: String? = null,
    val number: String? = null,
    val colour: String? = null
)

interface WingoApiService {
    @POST("GetGameIssue")
    suspend fun getGameIssue(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json;charset=UTF-8",
        @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT,
        @Body body: GetGameIssueRequest
    ): Response<GameIssueResponse>

    @POST("GetNoaverageEmerdList")
    suspend fun getNoaverageEmerdList(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json;charset=UTF-8",
        @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT,
        @Body body: GetEmerdListRequest
    ): Response<EmerdListResponse>

    companion object {
        const val BASE_URL = "https://api.55fiveapi.com/api/webapi/"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; SM-S908B) AppleWebKit/537.36"

        fun create(): WingoApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WingoApiService::class.java)
        }
    }
}

sealed interface LiveWebResult {
    data class Success(
        val currentIssue: String,
        val draws: List<DrawEntry>
    ) : LiveWebResult

    data class AuthRequired(val reason: String) : LiveWebResult
    data class Failure(val reason: String) : LiveWebResult
}

class WingoRemoteRepository(
    private val api: WingoApiService = WingoApiService.create()
) {
    suspend fun fetchLiveFeed(token: String, typeId: Int): LiveWebResult {
        val cleanToken = token.trim().removePrefix("Bearer ").trim()
        if (cleanToken.isBlank()) {
            return LiveWebResult.AuthRequired(
                "Enter your 55Five Bearer Token in Feed & Setup to stream live rounds, or paste draw numbers manually."
            )
        }
        val authHeader = "Bearer $cleanToken"
        val nowSec = System.currentTimeMillis() / 1000L

        return try {
            val listResp = api.getNoaverageEmerdList(
                authorization = authHeader,
                body = GetEmerdListRequest(typeId = typeId, timestamp = nowSec)
            )
            val issueResp = api.getGameIssue(
                authorization = authHeader,
                body = GetGameIssueRequest(typeId = typeId, timestamp = nowSec)
            )

            if (!listResp.isSuccessful) {
                val code = listResp.code()
                return if (code == 401 || code == 403) {
                    LiveWebResult.AuthRequired("API returned HTTP $code. Update your Bearer Token.")
                } else {
                    LiveWebResult.Failure("55Five API HTTP $code: ${listResp.message()}")
                }
            }

            val listBody = listResp.body()
            if (listBody == null || listBody.code != 0) {
                val apiMsg = listBody?.msg ?: "Unknown API response"
                return if (listBody?.code == 401 || apiMsg.contains("login", ignoreCase = true) || apiMsg.contains("token", ignoreCase = true)) {
                    LiveWebResult.AuthRequired("Session unauthorized ($apiMsg). Refresh Bearer Token.")
                } else {
                    LiveWebResult.Failure("API code ${listBody?.code}: $apiMsg")
                }
            }

            val draws = listBody.data?.list.orEmpty().mapNotNull { item ->
                val num = item.number?.trim()?.toIntOrNull()
                val issue = item.issueNumber?.trim()
                if (num != null && num in 0..9 && !issue.isNullOrBlank()) {
                    DrawEntry(
                        issueNumber = issue,
                        number = num,
                        colour = item.colour.orEmpty()
                    )
                } else {
                    null
                }
            }

            val activeIssue = issueResp.body()?.data?.issueNumber?.takeIf { it.isNotBlank() }
                ?: draws.firstOrNull()?.issueNumber?.let { incrementIssue(it) }
                ?: fallbackIssueString()

            if (draws.isEmpty()) {
                LiveWebResult.Failure("55Five API returned an empty draw list.")
            } else {
                LiveWebResult.Success(currentIssue = activeIssue, draws = draws)
            }
        } catch (e: Exception) {
            LiveWebResult.Failure(e.localizedMessage ?: "Network error reaching api.55fiveapi.com")
        }
    }

    /**
     * Parses either pasted 55five JSON (`GetNoaverageEmerdList` payload) or
     * plain numbers separated by spaces/commas/newlines (newest first).
     */
    fun parsePastedWebData(rawInput: String): List<DrawEntry> {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return emptyList()

        // Try JSON format first
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                val listArray = if (trimmed.startsWith("[")) {
                    org.json.JSONArray(trimmed)
                } else {
                    val root = JSONObject(trimmed)
                    if (root.has("data")) {
                        root.getJSONObject("data").getJSONArray("list")
                    } else if (root.has("list")) {
                        root.getJSONArray("list")
                    } else null
                }
                if (listArray != null) {
                    val parsed = mutableListOf<DrawEntry>()
                    for (i in 0 until listArray.length()) {
                        val obj = listArray.getJSONObject(i)
                        val num = obj.optString("number").trim().toIntOrNull()
                        val issue = obj.optString("issueNumber").ifBlank { "WEB-${listArray.length() - i}" }
                        val colour = obj.optString("colour")
                        if (num != null && num in 0..9) {
                            parsed.add(DrawEntry(issueNumber = issue, number = num, colour = colour))
                        }
                    }
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (_: Exception) {
                // Fall through to digit parsing
            }
        }

        // Plain digit sequence (comma/space/newline separated, or continuous digits)
        val tokens = Regex("\\d").findAll(trimmed).mapNotNull { it.value.toIntOrNull() }.toList()
        if (tokens.isEmpty()) return emptyList()

        val baseTime = fallbackIssuePrefix()
        return tokens.mapIndexed { idx, digit ->
            DrawEntry(
                issueNumber = "$baseTime${(100 - idx).coerceAtLeast(1).toString().padStart(3, '0')}",
                number = digit
            )
        }
    }

    companion object {
        fun incrementIssue(issue: String): String {
            val numeric = issue.toLongOrNull()
            return if (numeric != null) (numeric + 1L).toString() else "${issue}_next"
        }

        fun fallbackIssuePrefix(): String {
            val cal = java.util.Calendar.getInstance()
            val y = cal.get(java.util.Calendar.YEAR)
            val m = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            return "$y$m${d}1000"
        }

        fun fallbackIssueString(): String {
            val cal = java.util.Calendar.getInstance()
            val hh = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val mm = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
            return "${fallbackIssuePrefix()}$hh$mm"
        }
    }
}
