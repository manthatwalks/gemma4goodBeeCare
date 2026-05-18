package com.beecareanywhere.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One past diagnostic check-in. Persisted as a single line in DataStore. */
data class CheckIn(
    val id: String,            // short label shown in the round badge: "H1", "H2"...
    val description: String,   // what the user described — used as the row "note"
    val timestampMillis: Long, // when they ran the diagnosis
)

private val Context.checkInDataStore by preferencesDataStore(name = "check_ins")
private val KEY_CHECK_INS = stringSetPreferencesKey("entries")
private const val SEP = ""  // ASCII unit separator — safe across natural-language text

class CheckInRepository(private val context: Context) {

    /** Newest first. */
    val checkIns: Flow<List<CheckIn>> = context.checkInDataStore.data.map { prefs ->
        prefs[KEY_CHECK_INS].orEmpty()
            .mapNotNull(::decode)
            .sortedByDescending { it.timestampMillis }
    }

    suspend fun add(description: String) {
        context.checkInDataStore.edit { prefs ->
            val existing = prefs[KEY_CHECK_INS].orEmpty()
            val nextNumber = existing.size + 1
            val entry = CheckIn(
                id = "H$nextNumber",
                description = description.take(80),  // keep row note compact
                timestampMillis = System.currentTimeMillis(),
            )
            prefs[KEY_CHECK_INS] = existing + encode(entry)
        }
    }

    suspend fun clear() {
        context.checkInDataStore.edit { it.remove(KEY_CHECK_INS) }
    }

    private fun encode(c: CheckIn): String = "${c.id}$SEP${c.description}$SEP${c.timestampMillis}"

    private fun decode(line: String): CheckIn? {
        val parts = line.split(SEP)
        if (parts.size != 3) return null
        val ts = parts[2].toLongOrNull() ?: return null
        return CheckIn(id = parts[0], description = parts[1], timestampMillis = ts)
    }
}
