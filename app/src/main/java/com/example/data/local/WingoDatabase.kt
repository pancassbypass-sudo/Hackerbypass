package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey val period: String,
    val actualNum: Int = -1,
    val predictedSize: String,
    val predictedNum: Int,
    val backupNum: Int,
    val confidence: Int,
    val pattern: String,
    val aiReasoning: String,
    val betAmount: Long,
    val status: String = "PENDING", // PENDING, WIN, LOSS
    val profitLoss: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface GameRecordDao {
    @Query("SELECT * FROM game_records ORDER BY createdAtMs DESC LIMIT 100")
    fun observeRecords(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE status = 'PENDING'")
    suspend fun getPendingRecords(): List<GameRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: GameRecordEntity)

    @Query("DELETE FROM game_records")
    suspend fun clearAll()
}

@Database(entities = [GameRecordEntity::class], version = 1, exportSchema = false)
abstract class WingoDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao

    companion object {
        @Volatile
        private var INSTANCE: WingoDatabase? = null

        fun getInstance(context: Context): WingoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WingoDatabase::class.java,
                    "wingo_pulse.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class GameHistoryRepository(private val dao: GameRecordDao) {
    val records: Flow<List<GameRecordEntity>> = dao.observeRecords()

    suspend fun getPending(): List<GameRecordEntity> = dao.getPendingRecords()

    suspend fun upsert(record: GameRecordEntity) = dao.upsertRecord(record)

    suspend fun clear() = dao.clearAll()
}
