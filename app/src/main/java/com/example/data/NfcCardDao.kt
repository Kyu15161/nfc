package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcCardDao {
    @Query("SELECT * FROM nfc_cards ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<NfcCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: NfcCard): Long

    @Update
    suspend fun updateCard(card: NfcCard)

    @Query("DELETE FROM nfc_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("DELETE FROM nfc_cards")
    suspend fun clearHistory()
}
