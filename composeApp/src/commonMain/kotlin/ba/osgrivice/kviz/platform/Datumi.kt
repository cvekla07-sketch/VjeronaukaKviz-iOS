package ba.osgrivice.kviz.platform

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Zamjene za java.time formatiranje — isti izgled kao na Androidu. */

private fun lokalno(ms: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())

/** Kao LocalDate.now() */
fun danasLokalno(): LocalDate = lokalno(sadaMs()).date

/** "d. M. yyyy." */
fun formatLokalniDatum(d: LocalDate): String = "${d.dayOfMonth}. ${d.monthNumber}. ${d.year}."

/** "d. M. yyyy." */
fun formatDatum(ms: Long): String = formatLokalniDatum(lokalno(ms).date)

/** "d. M. yyyy. HH:mm" */
fun formatDatumVrijeme(ms: Long): String {
    val t = lokalno(ms)
    return "${formatLokalniDatum(t.date)} ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}
