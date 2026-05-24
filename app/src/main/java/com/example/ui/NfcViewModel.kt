package com.example.ui

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NfcCard
import com.example.data.NfcDatabase
import com.example.data.NfcCardRepository
import com.example.nfc.NfcParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NfcViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NfcCardRepository
    val allCards: StateFlow<List<NfcCard>>

    private val _activeCardScanned = MutableStateFlow<NfcCard?>(null)
    val activeCardScanned: StateFlow<NfcCard?> = _activeCardScanned.asStateFlow()

    private val _isScanningActive = MutableStateFlow(true)
    val isScanningActive: StateFlow<Boolean> = _isScanningActive.asStateFlow()

    private val _showToastMessage = MutableStateFlow<String?>(null)
    val showToastMessage: StateFlow<String?> = _showToastMessage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun tapToPay(card: NfcCard) {
        viewModelScope.launch {
            _showToastMessage.value = "Starting Host Card Emulation for ${card.walletLabel ?: card.cardType}..."
            kotlinx.coroutines.delay(2000)
            _showToastMessage.value = "Payment Simulation Completed"
        }
    }

    init {
        val database = NfcDatabase.getDatabase(application)
        repository = NfcCardRepository(database.nfcCardDao())
        allCards = repository.allCards.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setScanningActive(active: Boolean) {
        _isScanningActive.value = active
    }

    /**
     * Triggered by actual hardware scans.
     */
    fun onPhysicalCardDetected(tag: Tag) {
        viewModelScope.launch {
            try {
                val parsedCard = NfcParser.parseTag(tag)
                repository.insertCard(parsedCard)
                _activeCardScanned.value = parsedCard
                _showToastMessage.value = "Card Scanned: ${parsedCard.cardType}"
            } catch (e: Exception) {
                _showToastMessage.value = "Hardware read failed: ${e.message}"
            }
        }
    }

    /**
     * Triggered by simulated scanner.
     */
    fun onSimulateScan(cardIndex: Int) {
        viewModelScope.launch {
            try {
                val mockCard = NfcParser.getSimulatedCard(cardIndex)
                val idInDb = repository.insertCard(mockCard)
                // Set the active card with correct database id if we want to refer to it
                val finalCard = mockCard.copy(id = idInDb)
                _activeCardScanned.value = finalCard
                _showToastMessage.value = "Simulated Scan: ${mockCard.cardType}"
            } catch (e: Exception) {
                _showToastMessage.value = "Simulation failed: ${e.message}"
            }
        }
    }

    fun selectCardFromHistory(card: NfcCard) {
        _activeCardScanned.value = card
    }

    fun deleteCard(id: Long) {
        viewModelScope.launch {
            repository.deleteCardById(id)
            if (_activeCardScanned.value?.id == id) {
                _activeCardScanned.value = null
            }
            _showToastMessage.value = "Scan removed"
        }
    }

    fun clearAllScans() {
        viewModelScope.launch {
            repository.clearHistory()
            _activeCardScanned.value = null
            _showToastMessage.value = "Scan history cleared"
        }
    }

    fun clearActiveCard() {
        _activeCardScanned.value = null
    }

    fun dismissToast() {
        _showToastMessage.value = null
    }

    fun saveToWallet(card: NfcCard, label: String) {
        viewModelScope.launch {
            val updatedCard = card.copy(isSavedToWallet = true, walletLabel = label)
            repository.updateCard(updatedCard)
            if (_activeCardScanned.value?.id == card.id) {
                _activeCardScanned.value = updatedCard
            }
            _showToastMessage.value = "Saved to Wallet"
        }
    }

    fun updateCardInfo(card: NfcCard, newLabel: String, newDetails: String) {
        viewModelScope.launch {
            val updatedCard = card.copy(walletLabel = newLabel, payloadText = newDetails)
            repository.updateCard(updatedCard)
            if (_activeCardScanned.value?.id == card.id) {
                _activeCardScanned.value = updatedCard
            }
            _showToastMessage.value = "Card Info Updated"
        }
    }

    fun addManualCard(label: String, type: String, details: String) {
        viewModelScope.launch {
            val unqUid = "MANUAL-${System.currentTimeMillis().toString().takeLast(6)}"
            val manualCard = com.example.data.NfcCard(
                uid = unqUid,
                cardType = type,
                atqa = "N/A",
                sak = "N/A",
                manufacturer = "User Manual Entry",
                standard = "Mock Standard",
                technologies = "Manual",
                capacity = 0,
                blockCount = 0,
                isWritable = false,
                payloadType = "USER_DEFINED",
                payloadText = details,
                rawMemory = "User manually entered card.",
                walletLabel = label,
                isSavedToWallet = true
            )
            val insertedId = repository.insertCard(manualCard)
            _showToastMessage.value = "Card Added to Wallet"
        }
    }

    /**
     * Factory for creating NfcViewModel.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NfcViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NfcViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
