package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_cards")
data class NfcCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val cardType: String,
    val atqa: String,
    val sak: String,
    val manufacturer: String,
    val standard: String,
    val technologies: String, // Comma separated techs
    val capacity: Int, // in bytes
    val blockCount: Int,
    val isWritable: Boolean,
    val payloadType: String, // TEXT, URI, PAYMENT, TRANSIT, GAMING, ENCRYPTED, RAW
    val payloadText: String, // Parsed legible payload
    val rawMemory: String, // Separated blocks
    val walletLabel: String? = null,
    val isSavedToWallet: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
