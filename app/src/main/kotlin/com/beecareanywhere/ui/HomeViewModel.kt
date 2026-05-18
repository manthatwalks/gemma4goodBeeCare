package com.beecareanywhere.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beecareanywhere.data.CheckIn
import com.beecareanywhere.data.CheckInRepository
import com.beecareanywhere.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class HiveEntry(
    val id: String,
    val name: String,
    val note: String,
    val when_: String,
    val toneHex: Long,
)

class HomeViewModel(
    private val settings: Settings,
    private val checkIns: CheckInRepository,
) : ViewModel() {

    data class UiState(
        val language: Settings.Language = Settings.Language.English,
        val hives: List<HiveEntry> = emptyList(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.language, checkIns.checkIns) { lang, entries ->
                lang to entries
            }.collect { (lang, entries) ->
                _ui.update {
                    it.copy(
                        language = lang,
                        hives = entries.take(MAX_VISIBLE).mapIndexed { index, c -> c.toHive(index, lang) },
                    )
                }
            }
        }
    }

    fun setLanguage(lang: Settings.Language) {
        viewModelScope.launch { settings.setLanguage(lang) }
    }

    private fun CheckIn.toHive(index: Int, lang: Settings.Language): HiveEntry {
        val tone = ROW_TONES[index % ROW_TONES.size]
        return HiveEntry(
            id = id,
            name = description.lineFirst().ifBlank { defaultName(lang) },
            note = description.lineRest().ifBlank { description },
            when_ = relativeTime(timestampMillis, lang),
            toneHex = tone,
        )
    }

    private fun String.lineFirst(): String = substringBefore(' ').take(20)
    private fun String.lineRest(): String = substringAfter(' ', missingDelimiterValue = "").take(60)

    private fun defaultName(lang: Settings.Language): String =
        if (lang == Settings.Language.Swahili) "Mzinga" else "Hive"

    private fun relativeTime(ts: Long, lang: Settings.Language): String {
        val diff = System.currentTimeMillis() - ts
        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val isSw = lang == Settings.Language.Swahili
        return when {
            mins < 2L -> if (isSw) "Sasa" else "Now"
            hours < 1L -> "${mins}m"
            days < 1L -> "${hours}h"
            days < 7L -> "${days}d"
            else -> "${days / 7L}w"
        }
    }

    class Factory(
        private val settings: Settings,
        private val checkIns: CheckInRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(settings, checkIns) as T
        }
    }

    companion object {
        private const val MAX_VISIBLE = 6
        private val ROW_TONES = longArrayOf(
            0xFFFBE07A,  // honey
            0xFFB9DBB6,  // sage
            0xFFF4B0A2,  // coral
            0xFFB7D3EA,  // sky
            0xFFD6CCEF,  // lilac
        )
    }
}
