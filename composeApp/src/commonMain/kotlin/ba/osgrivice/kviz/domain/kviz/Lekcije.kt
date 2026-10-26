package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.NapredakPitanja

enum class StatusLekcije { ZAKLJUCANA, OTKLJUCANA, AKTIVNA, ZAVRSENA }

data class StanjeLekcije(
    val lekcija: Lekcija,
    val ukupno: Int,
    val vidjeno: Int,
    val savladano: Int,
    val zvjezdice: Int,
    val status: StatusLekcije
)

/**
 * Pravila staze:
 *  - zvjezdice: 1 ≥ 50 % pitanja odgovoreno tačno bar jednom (kutija ≥ 1); 2 ≥ 85 %; 3 = sva pitanja kutija ≥ 2.
 *  - lekcija je otključana kad su SVA pitanja prethodne viđena (odgovorena bar jednom, tačno ili ne);
 *    prva je uvijek otključana. Zvjezdice ne blokiraju napredovanje — Leitner sam vraća pogrešno.
 *  - aktivna = prva otključana bez 3 zvjezdice.
 */
object Lekcije {

    fun zvjezdice(ukupno: Int, kutije: List<Int>): Int {
        if (ukupno == 0) return 0
        val bar1 = kutije.count { it >= 1 }
        val bar2 = kutije.count { it >= 2 }
        return when {
            bar2 == ukupno -> 3
            bar1 * 100 / ukupno >= 85 -> 2
            bar1 * 100 / ukupno >= 50 -> 1
            else -> 0
        }
    }

    fun izracunajStazu(
        lekcije: List<Lekcija>,
        pitanjaPoLekciji: Map<String, List<String>>,
        napredak: Map<String, NapredakPitanja>
    ): List<StanjeLekcije> {
        var prethodnaOtkljucava = true
        var aktivnaDodijeljena = false
        return lekcije.map { l ->
            val ids = pitanjaPoLekciji[l.id].orEmpty()
            val kutije = ids.map { napredak[it]?.kutija ?: 0 }
            val vidjeno = ids.count { napredak[it]?.vidjeno == true }
            val savladano = kutije.count { it >= RasporedPonavljanja.SAVLADANO_OD_KUTIJE }
            val zv = zvjezdice(ids.size, kutije)
            val otkljucana = prethodnaOtkljucava
            val status = when {
                !otkljucana -> StatusLekcije.ZAKLJUCANA
                zv == 3 -> StatusLekcije.ZAVRSENA
                !aktivnaDodijeljena -> { aktivnaDodijeljena = true; StatusLekcije.AKTIVNA }
                else -> StatusLekcije.OTKLJUCANA
            }
            prethodnaOtkljucava = otkljucana && (ids.isEmpty() || vidjeno == ids.size)
            StanjeLekcije(l, ids.size, vidjeno, savladano, zv, status)
        }
    }
}
