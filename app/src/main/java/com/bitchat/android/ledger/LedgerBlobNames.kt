package com.bitchat.android.ledger

import com.bitchat.android.model.BitchatFilePacket

object LedgerBlobNames {
    private val pattern = Regex("^ledger_([0-9a-f]{64})_(.+)$", RegexOption.IGNORE_CASE)

    fun isLedgerBlob(file: BitchatFilePacket): Boolean =
        pattern.matches(file.fileName)

    fun parseHash(file: BitchatFilePacket): String? =
        pattern.matchEntire(file.fileName)?.groupValues?.get(1)?.lowercase()
}
