package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.NapredakPitanja
import ba.osgrivice.kviz.platform.sadaMs
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Razmaknuto ponavljanje (Leitner kutije).
 * Tačan odgovor → kutija +1 i pitanje dospijeva za INTERVALI[kutija] dana.
 * Netačan → kutija 0, dospijeva odmah (ponovo u sljedećoj sesiji).
 */
object RasporedPonavljanja {

    /** Dani do sljedećeg ponavljanja po kutiji: 0→odmah, 1→1 dan, 2→3 dana, 3→7, 4→14, 5→30. */
    private val INTERVALI = longArrayOf(0, 1, 3, 7, 14, 30)
    const val MAX_KUTIJA = 5

    /** Kutija od koje pitanje smatramo savladanim — usklađeno s pravilom za 3 zvjezdice (sva pitanja kutija ≥ 2). */
    const val SAVLADANO_OD_KUTIJE = 2

    fun nakonOdgovora(prije: NapredakPitanja, tacno: Boolean, danasDan: Long, sadaMs: Long): NapredakPitanja {
        val novaKutija = if (tacno) (prije.kutija + 1).coerceAtMost(MAX_KUTIJA) else 0
        return prije.copy(
            kutija = novaKutija,
            sljedeceDan = danasDan + INTERVALI[novaKutija],
            tacnih = prije.tacnih + if (tacno) 1 else 0,
            netacnih = prije.netacnih + if (tacno) 0 else 1,
            zadnjiOdgovorMs = sadaMs
        )
    }

    /** Epoch dan po LOKALNOJ vremenskoj zoni (dan se mijenja u ponoć kod učenika, ne po UTC). */
    fun danasDan(sadaMs: Long = sadaMs()): Long =
        Instant.fromEpochMilliseconds(sadaMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()

    fun pocetakDanaMs(epochDan: Long): Long =
        LocalDate.fromEpochDays(epochDan.toInt()).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
