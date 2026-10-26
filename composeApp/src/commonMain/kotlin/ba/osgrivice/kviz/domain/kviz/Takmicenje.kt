package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import kotlin.random.Random

/**
 * Pravila simulacije takmičenja — na jednom mjestu da se mijenjaju bez diranja UI-ja.
 *
 *  - 20 pitanja iz SVIH cjelina razreda, ravnomjerno (round-robin po lekciji),
 *  - samo A/B/C/D i Tačno/Netačno (kao na pravom takmičenju — bez kucanja),
 *  - 20 sekundi po pitanju; istek = netačno, bez povratne informacije do kraja,
 *  - istek vremena NE spušta Leitner kutiju (učenik nije pogriješio, nego nije stigao).
 */
object Takmicenje {
    const val BROJ_PITANJA = 20
    const val SEKUNDI_PO_PITANJU = 20
    const val MS_PO_PITANJU = SEKUNDI_PO_PITANJU * 1000L

    /** Pauza nakon odabira / isteka prije prelaska na sljedeće pitanje. */
    const val PAUZA_NAKON_ODGOVORA_MS = 350L
    const val PAUZA_NAKON_ISTEKA_MS = 700L

    val DOZVOLJENI_TIPOVI = setOf(TipPitanja.ABCD, TipPitanja.TACNO_NETACNO)

    /** Oznaka u bazi/Firestoreu za odgovor koji nije stigao na vrijeme. */
    const val ODGOVOR_ISTEK = "⏱"

    /**
     * Bira [broj] pitanja ravnomjerno po lekcijama: lekcije se obilaze redom (nasumičan redoslijed),
     * iz svake se uzima po jedno nasumično pitanje dok se ne napuni. Na kraju se sve izmiješa.
     */
    fun izaberi(sva: List<Pitanje>, broj: Int = BROJ_PITANJA, random: Random = Random.Default): List<Pitanje> {
        val kandidati = sva.filter { it.aktivno && it.tip in DOZVOLJENI_TIPOVI }
        if (kandidati.size <= broj) return kandidati.shuffled(random)

        val poLekciji = kandidati
            .groupBy { it.lekcijaId ?: "" }
            .values
            .map { it.shuffled(random).toMutableList() }
            .shuffled(random)

        val out = ArrayList<Pitanje>(broj)
        var i = 0
        while (out.size < broj && poLekciji.any { it.isNotEmpty() }) {
            poLekciji[i % poLekciji.size].removeFirstOrNull()?.let { out += it }
            i++
        }
        return out.shuffled(random)
    }

    fun procenat(tacnih: Int, ukupno: Int): Int = if (ukupno == 0) 0 else tacnih * 100 / ukupno

    /** Kratka ocjena za ekran rezultata. */
    fun ocjena(procenat: Int): String = when {
        procenat == 100 -> "Savršeno! Spremno za takmičenje."
        procenat >= 90 -> "Odlično — skoro bez greške."
        procenat >= 75 -> "Vrlo dobro — još malo ponavljanja."
        procenat >= 50 -> "Dobro — ponovi cjeline s greškama."
        else -> "Treba još vježbe — staza je tu."
    }

    /** "m:ss" */
    fun formatTrajanje(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    /** Da li je [novi] bolji od [stari] — više tačnih, pa kraće vrijeme. */
    fun jeBolji(noviTacnih: Int, noviTrajanjeMs: Long, stariTacnih: Int?, stariTrajanjeMs: Long?): Boolean {
        if (stariTacnih == null || stariTrajanjeMs == null) return true
        if (noviTacnih != stariTacnih) return noviTacnih > stariTacnih
        return noviTrajanjeMs < stariTrajanjeMs
    }
}
