package com.bitchat.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI-assisted disaster triage priority (local heuristic classifier).
 */
@Parcelize
enum class DisasterPriority : Parcelable {
    EMERGENCY,
    WARNING,
    GENERAL
}
