package com.bitchat.android.disaster

import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.BitchatMessageType
import com.bitchat.android.model.DisasterPriority
import java.util.Locale

/**
 * Offline disaster triage: priority + tags from message text using weighted keywords.
 * Suitable for mesh/offline; can be swapped for on-device ML later.
 */
object DisasterMessageClassifier {

    private val tagKeywords: Map<String, List<String>> = mapOf(
        "Medical" to listOf(
            "medical", "medic", "hospital", "ambulance", "injury", "injured", "bleeding", "blood",
            "doctor", "nurse", "patient", "sick", "wound", "pain", "heart attack", "stroke", "allergy",
            "vaccine", "insulin", "oxygen", "cp", "cpr", "defibrillator", "pharmacy", "clinic"
        ),
        "Food" to listOf(
            "food", "hungry", "hunger", "ration", "meal", "nutrition", "baby formula", "formula",
            "canned", "kitchen", "feeding", "starving", "supplies food"
        ),
        "Shelter" to listOf(
            "shelter", "tent", "housing", "displaced", "evacuat", "evac", "refuge", "camp", "roof",
            "homeless", "temporary housing", "warming center"
        ),
        "Rescue" to listOf(
            "rescue", "trapped", "stuck", "save us", "search and rescue", "sar", "missing person",
            "collapsed", "buried", "under rubble", "need help stuck"
        ),
        "Water" to listOf(
            "water", "drinking water", "dehydrat", "flood", "flooding", "sanitation", "hygiene",
            "contamination", "boil water"
        ),
        "Infrastructure" to listOf(
            "power", "electricity", "grid", "road", "bridge", "cell tower", "network", "internet",
            "gas line", "water main", "dam", "levee", "infrastructure"
        ),
        "Safety" to listOf(
            "unsafe", "hazard", "caution", "danger", "stay away", "do not enter", "gas leak",
            "chemical", "radiation", "structural"
        )
    )

    private val emergencyPhrases = listOf(
        "sos", "life threatening", "dying", "can't breathe", "cannot breathe", "bleeding out",
        "heart attack", "cardiac arrest", "unconscious", "not breathing", "trapped", "under rubble",
        "shooting", "active shooter", "bomb", "explosion", "fire spreading", "drowning", "help me now",
        "urgent help", "need rescue now", "save my life", "critical"
    )

    private val warningPhrases = listOf(
        "warning", "watch", "advisory", "evacuate", "evacuation", "shelter in place", "aftershock",
        "flash flood", "storm surge", "high winds", "hazardous", "chemical spill", "contaminated water",
        "unsafe building", "do not use", "avoid area"
    )

    data class Result(val priority: DisasterPriority, val tags: List<String>)

    fun classify(rawText: String): Result {
        val text = rawText.lowercase(Locale.US).trim()
        if (text.isEmpty()) return Result(DisasterPriority.GENERAL, emptyList())

        var emergencyScore = 0
        for (p in emergencyPhrases) {
            if (text.contains(p)) emergencyScore += 3
        }
        // Strong single tokens
        val strongEmergency = listOf("sos", "911", "112", "999")
        for (t in strongEmergency) {
            if (Regex("\\b${Regex.escape(t)}\\b").containsMatchIn(text)) emergencyScore += 4
        }

        var warningScore = 0
        for (p in warningPhrases) {
            if (text.contains(p)) warningScore += 2
        }

        val tagScores = linkedMapOf<String, Int>()
        for ((tag, keys) in tagKeywords) {
            var s = 0
            for (k in keys) {
                if (text.contains(k)) s += 2
            }
            if (s > 0) tagScores[tag] = s
        }

        val priority = when {
            emergencyScore >= 3 -> DisasterPriority.EMERGENCY
            emergencyScore >= 1 && warningScore == 0 -> DisasterPriority.EMERGENCY
            warningScore >= 2 || (warningScore >= 1 && emergencyScore == 0) -> DisasterPriority.WARNING
            else -> DisasterPriority.GENERAL
        }

        var tags = tagScores.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }
            .toMutableList()

        val fallbackOrder = listOf("Safety", "Infrastructure", "Medical", "Food", "Shelter", "Rescue", "Water")
        if (text.isNotEmpty()) {
            if (tags.size == 1) {
                fallbackOrder.firstOrNull { it !in tags }?.let { tags.add(it) }
            } else if (tags.isEmpty() && (priority == DisasterPriority.EMERGENCY || priority == DisasterPriority.WARNING)) {
                tags = when (priority) {
                    DisasterPriority.EMERGENCY -> mutableListOf("Rescue", "Safety")
                    DisasterPriority.WARNING -> mutableListOf("Safety", "Infrastructure")
                    DisasterPriority.GENERAL -> tags
                }
            }
        }

        return Result(priority, tags.take(4))
    }

    /** Classify text chat; for media, uses filename/path tail. */
    fun enrich(message: BitchatMessage): BitchatMessage {
        val text = when (message.type) {
            BitchatMessageType.Message -> message.content
            else -> {
                val c = message.content.trim()
                val slash = c.lastIndexOf('/')
                val bs = c.lastIndexOf('\\')
                val i = maxOf(slash, bs)
                if (i >= 0 && i < c.length - 1) c.substring(i + 1) else c
            }
        }
        val r = classify(text)
        return message.copy(disasterPriority = r.priority, disasterTags = r.tags.takeIf { it.isNotEmpty() })
    }
}
