package com.example.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class PaymentEmulationService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        // This is where you would intercept the POS terminal's SELECT AID command
        // and respond with the simulated EMV card data.
        // Returning 0x90 0x00 means "Success" in APDU.
        return byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    override fun onDeactivated(reason: Int) {
        // Handle when the phone is pulled away from the terminal
    }
}
