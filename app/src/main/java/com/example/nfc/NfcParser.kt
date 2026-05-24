package com.example.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.data.NfcCard
import java.util.Locale

object NfcParser {

    /**
     * Parse a real hardware Tag into our NfcCard model.
     */
    fun parseTag(tag: Tag): NfcCard {
        val uidBytes = tag.id
        val uidHex = toHexColonSeparated(uidBytes)
        
        val techList = tag.techList.map { it.substringAfterLast(".") }
        val techsJoined = techList.joinToString(",")

        // Default values
        var cardType = "Generic RFID Tag"
        var manufacturer = "Unknown Manufacturer"
        var standard = "ISO/IEC RFID Protocol"
        var atqa = "---"
        var sak = "---"
        var capacity = 0
        var blockCount = 0
        var isWritable = false
        var payloadType = "RAW"
        var payloadText = "No readable standard payload found."
        val rawMemoryBuilder = StringBuilder()

        // Detect Manufacturer based on UID start byte
        if (uidBytes.isNotEmpty()) {
            manufacturer = getManufacturerName(uidBytes[0])
        }

        // Try reading standard NfcA properties
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            standard = "ISO/IEC 14443-3 Type A"
            sak = String.format("0x%02X", nfcA.sak)
            atqa = nfcA.atqa.joinToString("") { String.format("%02X", it) }

            // Refined MIFARE/NTAG classification based on SAK and SAK/ATQA combo
            when {
                nfcA.sak.toInt() == 0x08 -> {
                    cardType = "MIFARE Classic 1K"
                    capacity = 1024
                    blockCount = 64
                    isWritable = true
                }
                nfcA.sak.toInt() == 0x18 -> {
                    cardType = "MIFARE Classic 4K"
                    capacity = 4096
                    blockCount = 256
                    isWritable = true
                }
                nfcA.sak.toInt() == 0x09 -> {
                    cardType = "MIFARE Classic Mini (0.3K)"
                    capacity = 320
                    blockCount = 20
                    isWritable = true
                }
                nfcA.sak.toInt() == 0x00 -> {
                    // Could be Ultralight or NTAG
                    if (atqa == "0044") {
                        cardType = "NTAG21x / Ultralight C"
                        capacity = 540
                        blockCount = 135
                    } else {
                        cardType = "MIFARE Ultralight"
                        capacity = 64
                        blockCount = 16
                    }
                    isWritable = true
                }
                nfcA.sak.toInt() == 0x20 -> {
                    cardType = "Smart Card / IsoDep (Type A)"
                    standard = "ISO/IEC 14443-4"
                }
            }
        }

        // Try reading NfcB properties
        val nfcB = NfcB.get(tag)
        if (nfcB != null) {
            standard = "ISO/IEC 14443-3 Type B"
            cardType = "Smart Card (Type B)"
        }

        // Try reading NfcF (Sony FeliCa)
        val nfcF = NfcF.get(tag)
        if (nfcF != null) {
            standard = "JIS X 6319-4 / ISO/IEC 18092"
            cardType = "FeliCa Smart Transit"
            manufacturer = "Sony Corporation"
            capacity = 4096
            blockCount = 256
        }

        // Try reading NfcV (Vincity / ISO 15693 / Vicinity cards)
        val nfcV = NfcV.get(tag)
        if (nfcV != null) {
            standard = "ISO/IEC 15693 (Vicinity)"
            cardType = "RFID Vicinity Tag"
        }

        // Try NDEF Content extraction
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMsg = ndef.ndefMessage
                isWritable = ndef.isWritable
                capacity = ndef.maxSize
                if (ndefMsg != null && ndefMsg.records.isNotEmpty()) {
                    val record = ndefMsg.records[0]
                    val tnf = record.tnf
                    val type = String(record.type)
                    val payload = record.payload

                    if (type == "T" || String(record.type).lowercase() == "text") {
                        payloadType = "TEXT"
                        // Text payload skip status byte and language letters
                        if (payload.isNotEmpty()) {
                            val statusByte = payload[0].toInt()
                            val langLen = statusByte and 0x3F
                            payloadText = if (payload.size > 1 + langLen) {
                                String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
                            } else {
                                String(payload, Charsets.UTF_8)
                            }
                        }
                    } else if (type == "U" || String(record.type).lowercase() == "uri") {
                        payloadType = "URI"
                        if (payload.isNotEmpty()) {
                            val prefixCode = payload[0].toInt()
                            val prefix = getUriPrefix(prefixCode)
                            val remaining = String(payload, 1, payload.size - 1, Charsets.UTF_8)
                            payloadText = "$prefix$remaining"
                        }
                    } else {
                        // Other payload representation
                        payloadType = "RAW_NDEF"
                        payloadText = "NDEF Type: $type, Length: ${payload.size} bytes"
                    }
                } else {
                    payloadText = "Empty NDEF storage tag detected."
                }
                ndef.close()
            } catch (e: Exception) {
                payloadText = "NDEF tag detected but read failed: ${e.message}"
            }
        }

        // Deep technology data extraction:
        // MifareClassic dump
        val mifareClassic = MifareClassic.get(tag)
        if (mifareClassic != null) {
            try {
                mifareClassic.connect()
                val sectorCount = mifareClassic.sectorCount
                rawMemoryBuilder.append("--- MIFARE CLASSIC STORAGE MAP ---\n")
                
                var globalBlock = 0
                for (s in 0 until minOf(sectorCount, 4)) { // Limit output to first 4 sectors in summary dump
                    val keyDefault = MifareClassic.KEY_DEFAULT
                    val keyMad = byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte())
                    val keyNfc = byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())

                    val auth = mifareClassic.authenticateSectorWithKeyA(s, keyDefault) ||
                               mifareClassic.authenticateSectorWithKeyA(s, keyMad) ||
                               mifareClassic.authenticateSectorWithKeyA(s, keyNfc)

                    val sectorType = if (s == 0) "MAD Sector" else "Data Sector"
                    rawMemoryBuilder.append(String.format("Sector %02d [%s] (Auth: %b)\n", s, sectorType, auth))
                    
                    val blockInSectorCount = mifareClassic.getBlockCountInSector(s)
                    for (b in 0 until blockInSectorCount) {
                        try {
                            if (auth) {
                                val blockData = mifareClassic.readBlock(globalBlock)
                                val hexRep = blockData.joinToString(" ") { String.format("%02X", it) }
                                val asciiRep = blockData.map { if (it in 32..126) it.toChar() else '.' }.joinToString("")
                                rawMemoryBuilder.append(String.format("  Block %02d: %s | %s\n", globalBlock, hexRep, asciiRep))
                            } else {
                                rawMemoryBuilder.append(String.format("  Block %02d: [SECURED / LOCKED]\n", globalBlock))
                            }
                        } catch (e: Exception) {
                            rawMemoryBuilder.append(String.format("  Block %02d: Read error: %s\n", globalBlock, e.message))
                        }
                        globalBlock++
                    }
                }
                if (sectorCount > 4) {
                    rawMemoryBuilder.append("... [${sectorCount - 4} sectors omitted for brevity] ...\n")
                }
                mifareClassic.close()
            } catch (e: Exception) {
                rawMemoryBuilder.append("MifareClassic connection failed: ${e.message}")
            }
        }

        // MifareUltralight dump
        val mifareUltralight = MifareUltralight.get(tag)
        if (mifareUltralight != null) {
            try {
                mifareUltralight.connect()
                rawMemoryBuilder.append("--- MIFARE ULTRALIGHT MEMORY PAGES ---\n")
                // Read first 16 pages
                for (p in 0 until 16) {
                    try {
                        val pageData = mifareUltralight.readPages(p * 4) // reads 4 pages at a time (16 bytes)
                        // slice pageData safely to keep only page p
                        val slice = if (pageData != null && pageData.size >= 4) pageData.sliceArray(0..3) else pageData ?: byteArrayOf()
                        val hexRep = slice.joinToString(" ") { String.format("%02X", it) }
                        val asciiRep = slice.map { if (it in 32..126) it.toChar() else '.' }.joinToString("")
                        rawMemoryBuilder.append(String.format("Page %02d: %s | %s\n", p, hexRep, asciiRep))
                    } catch (e: Exception) {
                        rawMemoryBuilder.append(String.format("Page %02d: Failed: %s\n", p, e.message))
                    }
                }
                mifareUltralight.close()
            } catch (e: Exception) {
                rawMemoryBuilder.append("MifareUltralight connection failed: ${e.message}")
            }
        }

        // IsoDep Dump
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                rawMemoryBuilder.append("--- ISO-DEP/EMV SECURED CONTAINER ---\n")
                // Check PPSE (2PAY.SYS.DDF01)
                val selectPPSE = byteArrayOf(
                    0x00, 0xA4.toByte(), 0x04, 0x00, 
                    0x0E, '2'.code.toByte(), 'P'.code.toByte(), 'A'.code.toByte(), 'Y'.code.toByte(), 
                    '.'.code.toByte(), 'S'.code.toByte(), 'Y'.code.toByte(), 'S'.code.toByte(), 
                    '.'.code.toByte(), 'D'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 
                    '0'.code.toByte(), '1'.code.toByte(), 0x00
                )
                
                var isEmv = false

                val responsePPSE = isoDep.transceive(selectPPSE)
                if (responsePPSE != null && responsePPSE.size >= 2) {
                    val sw1 = responsePPSE[responsePPSE.size - 2].toInt() and 0xFF
                    val sw2 = responsePPSE[responsePPSE.size - 1].toInt() and 0xFF
                    if (sw1 == 0x90 && sw2 == 0x00) {
                        isEmv = true
                        rawMemoryBuilder.append("SELECT PPSE (2PAY.SYS.DDF01) SUCCESS.\n")
                    }
                }

                // Try Visa AID
                val selectVisa = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10, 0x00)
                val visaResp = isoDep.transceive(selectVisa)
                
                // Try Mastercard AID
                val selectMC = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10, 0x00)
                val mcResp = isoDep.transceive(selectMC)

                if ((visaResp != null && visaResp.size >= 2 && visaResp[visaResp.size-2] == 0x90.toByte()) || 
                    (mcResp != null && mcResp.size >= 2 && mcResp[mcResp.size-2] == 0x90.toByte())) {
                    isEmv = true
                    rawMemoryBuilder.append("Payment Application AID identified.\n")
                }

                if (isEmv) {
                    cardType = "EMV Contactless Card"
                    payloadType = "PAYMENT"
                    payloadText = "EMV Payment Card (Contactless). Sensitive information is not automatically extracted. Edit details to manually add this information."
                    rawMemoryBuilder.append("Sensitive data extraction is disabled for security and privacy.\n")
                }

                isoDep.close()
            } catch (e: Exception) {
                rawMemoryBuilder.append("IsoDep Smart APDU Transceive failed/unavailable: ${e.message}")
            }
        }

        // If rawMemory empty, write standard representation
        if (rawMemoryBuilder.isEmpty()) {
            rawMemoryBuilder.append("--- RAW RFID SECTOR MATRIX ---\n")
            if (uidBytes.isNotEmpty()) {
                rawMemoryBuilder.append("UID: ${uidHex}\n")
                rawMemoryBuilder.append("Manufacturer Byte Code: ${String.format("0x%02X", uidBytes[0])} ($manufacturer)\n")
            }
            rawMemoryBuilder.append("Technologies in Tag Architecture:\n")
            techList.forEach { rawMemoryBuilder.append("  - $it\n") }
            rawMemoryBuilder.append("\nTag is unformatted / secure-locked. No compatible default memory mapping.")
        }

        return NfcCard(
            uid = uidHex,
            cardType = cardType,
            atqa = atqa,
            sak = sak,
            manufacturer = manufacturer,
            standard = standard,
            technologies = techsJoined,
            capacity = capacity,
            blockCount = blockCount,
            isWritable = isWritable,
            payloadType = payloadType,
            payloadText = payloadText,
            rawMemory = rawMemoryBuilder.toString()
        )
    }

    /**
     * Parse binary manufacturer byte to name based on ISO/IEC 7816 system.
     */
    fun getManufacturerName(manufactureId: Byte): String {
        return when (manufactureId.toInt() and 0xFF) {
            0x01 -> "NXP Semiconductors (Broadcom / Motorola)"
            0x02 -> "STMicroelectronics"
            0x03 -> "Hitachi, Ltd."
            0x04 -> "NXP Semiconductors (Philips)"
            0x05 -> "Infineon Technologies AG"
            0x06 -> "Cylink"
            0x07 -> "Texas Instruments"
            0x08 -> "Fujitsu Limited"
            0x09 -> "Matsushita Electric Industrial"
            0x0A -> "NEC"
            0x0B -> "Oki Electric Industry"
            0x0C -> "Toshiba Corporation"
            0x10 -> "Samsung Electronics"
            0x11 -> "Sony Corporation"
            0x12 -> "Hyundai Electronics"
            0x13 -> "LG Semiconductors"
            0x16 -> "EM Microelectronic"
            0x19 -> "Xicor"
            0x23 -> "Marvell"
            0x2B -> "Broadcom"
            0x3A -> "Renesas Technology"
            0x47 -> "Atmel"
            else -> "Generic RFID Vendor"
        }
    }

    private fun getUriPrefix(prefixCode: Int): String {
        return when (prefixCode) {
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            0x05 -> "tel:"
            0x06 -> "mailto:"
            0x07 -> "ftp://anonymous:anonymous@"
            0x08 -> "ftp://ftp."
            0x09 -> "ftps://"
            0x0A -> "sftp://"
            0x0B -> "smb://"
            0x0C -> "nfs://"
            0x0D -> "ftp://"
            0x0E -> "dav://"
            0x0F -> "news:"
            0x10 -> "telnet://"
            0x11 -> "imap:"
            0x12 -> "rtsp://"
            0x13 -> "urn:"
            0x14 -> "pop:"
            0x15 -> "sip:"
            0x16 -> "sips:"
            0x17 -> "tftp:"
            0x18 -> "btspp://"
            0x19 -> "btl2cap://"
            0x1A -> "btgoep://"
            0x1B -> "tcpobex://"
            0x1C -> "irdaobex://"
            0x1D -> "file://"
            0x1E -> "urn:epc:id:"
            0x1F -> "urn:epc:tag:"
            else -> ""
        }
    }

    private fun toHexColonSeparated(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "---"
        return bytes.joinToString(":") { String.format("%02X", it) }.uppercase(Locale.getDefault())
    }

    /**
     * Generate simulated card diagnostics on request.
     */
    fun getSimulatedCard(cardTypeIndex: Int): NfcCard {
        return when (cardTypeIndex) {
            1 -> NfcCard(
                uid = "37:B2:91:0F:CE:9D",
                cardType = "Visa EMV Contactless",
                atqa = "0008",
                sak = "0x20",
                manufacturer = "NXP (EMV Certified Vendor)",
                standard = "ISO/IEC 14443-4 Type A",
                technologies = "NfcA,IsoDep",
                capacity = 8192,
                blockCount = 128,
                isWritable = false,
                payloadType = "PAYMENT",
                payloadText = "EMV Contactless VISA debit profile. Sensitive information is not automatically extracted. Edit details to manually add this information.",
                rawMemory = """
                    --- ISO-DEP/EMV SECURED RECORD DUMP ---
                    Select Application Identifier (AID):
                    APDU: 00 A4 04 00 07 A0 00 00 00 03 10 10 (Visa Credit/Debit)
                    Response [90 00 Standard Success]:
                    FCI Template [6F]:
                      DF Name [84]: A0 00 00 00 03 10 10
                      FCI Proprietary Template [A5]:
                        Application Label [50]: "VISA DEBIT"
                        Language Preference [5F2D]: "en"
                        Issuer Code Index [9F11]: 01
                    
                    System Status Bits [SW1 SW2]: 90 00 (Secure transaction ready)
                    Note: Sensitive PAN and Expiry extraction is disabled.
                """.trimIndent()
            )
            2 -> NfcCard(
                uid = "01:2E:3F:4A:5B:6C:7D:8E",
                cardType = "Sony FeliCa Transit",
                atqa = "---",
                sak = "0x00",
                manufacturer = "Sony Corporation",
                standard = "JIS X 6319-4 / ISO/IEC 18092",
                technologies = "NfcF",
                capacity = 4096,
                blockCount = 256,
                isWritable = false,
                payloadType = "TRANSIT",
                payloadText = "PASMO / Suica Smart Mobility Card. IDCode System 0x0003. Transaction history: Tokyo St -> Shinjuku St: Fare - ¥200. Account balance: ¥2,450. Card active.",
                rawMemory = """
                    --- SONY FELICA TRANSIT MEMORY MAP ---
                    IDm (Manufacturer ID): 01 2E 3F 4A 5B 6C 7D 8E
                    PMm (System Parameter): 30 00 2B 48 83 C1 D0 FF
                    
                    System Code: 0x0003 (Transit / Ticket System)
                    Service Code: 0x090F (Transaction Log - Read Only)
                    
                    Service Block 00:
                      Data: 80 06 18 2F 11 22 83 CF 09 DA 00 00 00 00 09 92
                      Ascii: . . . / . . . . . . . . . . . . 
                      Interpretation: 2026-05-23 | Tap-In Tokyo -> Tap-Out Shinjuku | Deduct: 200 JPY
                    
                    Service Block 01:
                      Data: 80 06 18 2F 10 11 83 CF 0A 62 00 00 00 00 0A DA
                      Interpretation: 2026-05-22 | Tap-In Shibuya -> Tap-Out Harajuku | Deduct: 140 JPY
                    
                    Card Balance Allocation:
                      Current Balance register: ¥2,450 (HEX value 0x09 0x92 mapped securely)
                """.trimIndent()
            )
            3 -> NfcCard(
                uid = "04:8F:2A:44:B6:5E:80",
                cardType = "Amiibo NTAG215",
                atqa = "0044",
                sak = "0x00",
                manufacturer = "NXP Semiconductors",
                standard = "ISO/IEC 14443-3 Type A",
                technologies = "NfcA,Ndef",
                capacity = 540,
                blockCount = 135,
                isWritable = false,
                payloadType = "GAMING",
                payloadText = "Nintendo Amiibo Token Type NTAG215. Character identified: MARIO (Super Smash Bros - #01). Stats: Level 50, Defense custom buff: +12, Attack: +25.",
                rawMemory = """
                    --- NTAG215 AMIIBO MEMORY CONFIG ---
                    Page 00 (Serial UID Pt 1) : 04 8F 2A AE (Lock code: AE)
                    Page 01 (Serial UID Pt 2) : 44 B6 5E 80 (CRC byte checked)
                    Page 02 (Static Lockbits) : FE 00 00 00 (Sectors write protected)
                    Page 03 (CC Container)    : E1 10 3E 00 (Mapped for 540B capacity)
                    
                    --- Nintendo Decryption Header Pages (0x04 - 0x13) ---
                    Page 04: C1 B2 A3 D4 | . . . .
                    Page 05: 00 01 02 03 | . . . .
                    Page 06: FE DB CA 98 | . . . .
                    Page 14 (Amiibo ID Part 1): 00 02 01 00 (Standard Nintendo Product)
                    Page 15 (Amiibo ID Part 2): 00 00 03 02 (Character ID Code Match: Mario SSB)
                    
                    --- Custom Game Cryptographic Profile ---
                    Page 20: 8A B3 E5 1C (Level register 50)
                    Page 21: C0 2E 10 3B (Attack stats map: +25)
                    Page 22: BE AD D4 E3 (Defense stats map: +12)
                    Page 50: [Amiibo HMAC Sha-256 Signature verified]
                """.trimIndent()
            )
            4 -> NfcCard(
                uid = "AC:33:DE:7F",
                cardType = "MIFARE Classic 1K",
                atqa = "0004",
                sak = "0x08",
                manufacturer = "NXP Semiconductors (Mifare)",
                standard = "ISO/IEC 14443-3 Type A",
                technologies = "NfcA,MifareClassic",
                capacity = 1024,
                blockCount = 64,
                isWritable = true,
                payloadType = "TEXT",
                payloadText = "Corporate Office RFID smart token. Encrypted Sector 4 authenticates successfully with secondary default key B. Sector Code: ACC_LOCK_DOOR_B43_MAIN.",
                rawMemory = """
                    --- MIFARE CLASSIC 1K STORAGE MAP ---
                    Sector 00 [MAD Directory / Manufacturer Block] (Auth: True)
                      Block 00 [Manufacturer Block]: AC 33 DE 7F 50 04 00 01 D2 ED B5 33 21 02 A8 C1
                      Block 01 [MAD Info Table 1] : 11 00 12 00 13 00 14 00 15 00 16 00 17 00 18 00
                      Block 02 [MAD Info Table 2] : 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                      Block 03 [MAD Security Key] : A0 A1 A2 A3 A4 A5 78 77 88 C1 FF FF FF FF FF FF
                    
                    Sector 01 [Secured Pass Data] (Auth: True)
                      Block 04: A1 B2 C3 FF 00 24 A2 DF 0E 00 00 00 00 00 10 AA
                      Block 05: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                      Block 06: [LOCKED - ENCRYPTED VALUE]
                      Block 07 [Sector Trailer]   : FF FF FF FF FF FF FF 07 80 69 FF FF FF FF FF FF
                    
                    Sector 02 [Secured Access Logs] (Auth: True)
                      Block 08 [Office Room ID]  : 4F 46 46 49 43 45 5F 44 4F 4F 52 5F 5F 41 43 54 | OFFICE_DOOR__ACT
                      Block 09 [Security Rank]   : 00 00 05 AA 00 00 00 00 00 00 00 00 00 00 00 00
                      Block 10: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                      Block 11 [Sector Trailer]   : FF FF FF FF FF FF 7F 07 88 40 FF FF FF FF FF FF
                    
                    Sector 03: [Secured / Authenticated Block - LOCKED]
                """.trimIndent()
            )
            else -> NfcCard(
                uid = "04:5A:6D:E2:C3:54:80",
                cardType = "NTAG213 Smart Tag",
                atqa = "0044",
                sak = "0x00",
                manufacturer = "NXP Semiconductors",
                standard = "ISO/IEC 14443-3 Type A",
                technologies = "NfcA,Ndef",
                capacity = 144,
                blockCount = 36,
                isWritable = true,
                payloadType = "URI",
                payloadText = "Secure Smart Poster. Quick Wi-Fi access configurations included. SSID: 'HQ_NFC_Portal', Security: 'WPA3', Key: 'tap_connected_5g'. Redirect URL: https://portal.nfc-smart.com",
                rawMemory = """
                    --- NTAG213 SMART NDEF POSTER MATRIX ---
                    Page 00: 04 5A 6D AF (Serial Code Pt 1)
                    Page 01: E2 C3 54 80 (Serial Code Pt 2)
                    Page 02: 48 00 00 0E (Static configuration)
                    Page 03: E1 10 12 00 (CC Container - 144B writable size)
                    
                    --- NDEF Raw Payload Block (Page 04 onwards) ---
                    Page 04: 03 2B D1 02 (03=NDEF Message Magic, 2B=Payload Size, D1=Record Header)
                    Page 05: 1D 53 70 91 (Type length: 29 bytes, Type = 'Sp' Smart Poster)
                    Page 06: 01 02 02 54 (Smart record layout start)
                    Page 07: 02 65 6E 48 (Title Language 'en' - Wifi Config Portal)
                    Page 08: 51 5F 4E 46 ('SSID=HQ_NFC_Portal')
                    Page 09: 43 5F 50 6F ('_Po')
                    Page 10: 72 74 61 6C ('rtal')
                    Page 11: FE 00 00 00 (FE=Terminator TLV marker)
                """.trimIndent()
            )
        }
    }
}
