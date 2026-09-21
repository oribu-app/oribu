package app.oribu.ui.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import app.oribu.data.LocalePreferences
import app.oribu.data.SavedLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * App language, applied via [AppCompatDelegate.setApplicationLocales], which recreates every
 * running Activity in the process to apply it — requires `MainActivity` to be an
 * `AppCompatActivity` (see the comment there); on this single-Activity app there's otherwise no
 * AppCompatDelegate for the static call to reach. Persisted automatically by the
 * `AppLocalesMetadataHolderService` entry in the manifest, so it needs no DataStore key of its
 * own (unlike [DateFormatMode], which AppCompat has no concept of).
 */
enum class LanguageMode(
    val tag: String?,
    val label: String,
) {
    SYSTEM(null, "System default"),
    ENGLISH("en", "English"),
    PORTUGUESE("pt-BR", "Português"),
}

enum class DateFormatMode(
    val pattern: String?,
) {
    SYSTEM(null),
    DAY_MONTH_YEAR("dd/MM/yyyy"),
    MONTH_DAY_YEAR("MM/dd/yyyy"),
    ISO("yyyy-MM-dd"),
}

private val dateFormatSample: Date by lazy { GregorianCalendar(2026, Calendar.DECEMBER, 31).time }

fun DateFormatMode.exampleLabel(): String {
    val name =
        when (this) {
            DateFormatMode.SYSTEM -> "System default"
            DateFormatMode.DAY_MONTH_YEAR -> "Day/Month/Year"
            DateFormatMode.MONTH_DAY_YEAR -> "Month/Day/Year"
            DateFormatMode.ISO -> "Year-Month-Day (ISO)"
        }
    return "$name (${formatDate(dateFormatSample.time)})"
}

/** Bare pattern label (e.g. "dd/MM/yyyy") for the option list in the picker dialog. */
fun DateFormatMode.patternLabel(): String = pattern ?: "System default"

/** Formats a stored date (epoch millis) following [AppLocaleController.dateFormatMode]. */
fun formatDate(dateMs: Long): String {
    val date = Date(dateMs)
    val pattern = AppLocaleController.dateFormatMode.pattern
    return if (pattern == null) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}

object AppLocaleController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Set right before triggering the locale change, so `MainActivity` can paint a plain black
     * screen over the transition instead of the flash the platform's own recreate-driven
     * animation shows for it — this object is a process-wide singleton, so the flag survives the
     * Activity recreate itself; `MainActivity` clears it after a short delay once the new
     * Activity instance is up.
     */
    var isLanguageChanging by mutableStateOf(false)

    private var _dateFormatMode by mutableStateOf(DateFormatMode.SYSTEM)
    var dateFormatMode: DateFormatMode
        get() = _dateFormatMode
        set(value) {
            _dateFormatMode = value
            persist()
        }

    var languageMode: LanguageMode
        get() {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return LanguageMode.entries.firstOrNull { it.tag != null && tags.startsWith(it.tag) }
                ?: LanguageMode.SYSTEM
        }
        set(value) {
            if (value == languageMode) return
            isLanguageChanging = true
            AppCompatDelegate.setApplicationLocales(
                value.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList(),
            )
        }

    fun init(context: Context) {
        LocalePreferences.init(context)
        scope.launch {
            _dateFormatMode = LocalePreferences.saved.first().dateFormatMode
        }
    }

    private fun persist() {
        LocalePreferences.persist(SavedLocale(dateFormatMode))
    }
}
