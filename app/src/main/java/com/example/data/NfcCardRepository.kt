package com.example.data

import kotlinx.coroutines.flow.Flow

class NfcCardRepository(private val nfcCardDao: NfcCardDao) {
    val allCards: Flow<List<NfcCard>> = nfcCardDao.getAllCards()

    suspend fun insertCard(card: NfcCard): Long {
        return nfcCardDao.insertCard(card)
    }

    suspend fun updateCard(card: NfcCard) {
        nfcCardDao.updateCard(card)
    }

    suspend fun deleteCardById(id: Long) {
        nfcCardDao.deleteCardById(id)
    }

    suspend fun clearHistory() {
        nfcCardDao.clearHistory()
    }
}
