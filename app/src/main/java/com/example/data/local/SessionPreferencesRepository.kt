package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.PlayMode
import com.example.domain.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wingo_session_prefs")

class SessionPreferencesRepository(private val context: Context) {

    private object Keys {
        val BEARER_TOKEN = stringPreferencesKey("bearer_token")
        val TYPE_ID = intPreferencesKey("type_id")
        val PLAY_MODE = stringPreferencesKey("play_mode")
        val BALANCE = longPreferencesKey("balance")
        val INITIAL_BALANCE = longPreferencesKey("initial_balance")
        val PROFIT = longPreferencesKey("profit")
        val WINS = intPreferencesKey("wins")
        val LOSSES = intPreferencesKey("losses")
        val CONSEC_LOSS = intPreferencesKey("consec_loss")
        val CONSEC_WIN = intPreferencesKey("consec_win")
        val BET_INDEX = intPreferencesKey("bet_index")
        val LAST_ISSUE = stringPreferencesKey("last_issue")
        val MIN_BET = longPreferencesKey("min_bet")
        val TARGET_PROFIT = longPreferencesKey("target_profit")
        val POLL_INTERVAL_SEC = intPreferencesKey("poll_interval_sec")
        val AUTO_POLL = booleanPreferencesKey("auto_poll")
        val MANUAL_NUMBERS = stringPreferencesKey("manual_numbers_csv")
    }

    val sessionFlow: Flow<SessionState> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.PLAY_MODE] ?: PlayMode.LARGE_SMALL.name
        val playMode = runCatching { PlayMode.valueOf(modeName) }.getOrDefault(PlayMode.LARGE_SMALL)
        SessionState(
            bearerToken = prefs[Keys.BEARER_TOKEN] ?: "",
            typeId = prefs[Keys.TYPE_ID] ?: 1,
            playMode = playMode,
            balance = prefs[Keys.BALANCE] ?: 20_000L,
            initialBalance = prefs[Keys.INITIAL_BALANCE] ?: 20_000L,
            profit = prefs[Keys.PROFIT] ?: 0L,
            wins = prefs[Keys.WINS] ?: 0,
            losses = prefs[Keys.LOSSES] ?: 0,
            consecLoss = prefs[Keys.CONSEC_LOSS] ?: 0,
            consecWin = prefs[Keys.CONSEC_WIN] ?: 0,
            betIndex = prefs[Keys.BET_INDEX] ?: 0,
            lastIssue = prefs[Keys.LAST_ISSUE] ?: "",
            minBet = prefs[Keys.MIN_BET] ?: 5_000L,
            targetProfit = prefs[Keys.TARGET_PROFIT] ?: 80_000L,
            pollIntervalSec = prefs[Keys.POLL_INTERVAL_SEC] ?: 3,
            autoPollEnabled = prefs[Keys.AUTO_POLL] ?: false,
            manualNumbersCsv = prefs[Keys.MANUAL_NUMBERS] ?: ""
        )
    }

    suspend fun saveSession(state: SessionState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BEARER_TOKEN] = state.bearerToken
            prefs[Keys.TYPE_ID] = state.typeId
            prefs[Keys.PLAY_MODE] = state.playMode.name
            prefs[Keys.BALANCE] = state.balance
            prefs[Keys.INITIAL_BALANCE] = state.initialBalance
            prefs[Keys.PROFIT] = state.profit
            prefs[Keys.WINS] = state.wins
            prefs[Keys.LOSSES] = state.losses
            prefs[Keys.CONSEC_LOSS] = state.consecLoss
            prefs[Keys.CONSEC_WIN] = state.consecWin
            prefs[Keys.BET_INDEX] = state.betIndex
            prefs[Keys.LAST_ISSUE] = state.lastIssue
            prefs[Keys.MIN_BET] = state.minBet
            prefs[Keys.TARGET_PROFIT] = state.targetProfit
            prefs[Keys.POLL_INTERVAL_SEC] = state.pollIntervalSec
            prefs[Keys.AUTO_POLL] = state.autoPollEnabled
            prefs[Keys.MANUAL_NUMBERS] = state.manualNumbersCsv
        }
    }
}
