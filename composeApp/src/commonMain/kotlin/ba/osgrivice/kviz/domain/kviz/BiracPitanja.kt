package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.NapredakPitanja
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja

/**
 * Bira pitanja za jednu sesiju tako da:
 *  1. učenik dobije SVA još neviđena pitanja aktivne lekcije (jedan kviz = cijeli nivo, pa se sljedeći odmah otvara),
 *  2. uz njih najviše [maxPonavljanja] pitanja koja DOSPIJEVAJU za ponavljanje (iz bilo koje lekcije),
 *  3. ako novih nema, sesija je [velicina] pitanja: ponavljanja + najslabija pitanja lekcije,
 *  4. redoslijed miješa tipove da ne idu 5 T/N zaredom.
 */
object BiracPitanja {

    data class Plan(val pitanja: List<Pitanje>, val novih: Int, val ponavljanja: Int)

    fun zaLekciju(
        pitanjaLekcije: List<Pitanje>,
        dospjelaSvuda: List<Pitanje>,
        napredak: Map<String, NapredakPitanja>,
        velicina: Int = 10,
        maxPonavljanja: Int = 3
    ): Plan {
        val nova = pitanjaLekcije.filter { napredak[it.id]?.vidjeno != true }
        val dospjelaIds = dospjelaSvuda.map { it.id }.toHashSet()

        // ponavljanja: prvo iz ove lekcije, pa iz ostalih
        val ponavljanja = (dospjelaSvuda.filter { it.lekcijaId == pitanjaLekcije.firstOrNull()?.lekcijaId } +
                dospjelaSvuda.filter { it.lekcijaId != pitanjaLekcije.firstOrNull()?.lekcijaId })
            .distinctBy { it.id }
            .take(if (nova.isNotEmpty()) maxPonavljanja else velicina)

        val izabrana = LinkedHashMap<String, Pitanje>()
        nova.forEach { izabrana[it.id] = it }
        ponavljanja.forEach { izabrana[it.id] = it }

        if (izabrana.size < velicina) {
            // dopuna: najslabija (najniža kutija) iz lekcije koja nisu već izabrana
            pitanjaLekcije
                .filter { it.id !in izabrana && it.id !in dospjelaIds }
                .sortedBy { napredak[it.id]?.kutija ?: 0 }
                .take(velicina - izabrana.size)
                .forEach { izabrana[it.id] = it }
        }

        val lista = izmijesajTipove(izabrana.values.toList())
        return Plan(lista, novih = lista.count { napredak[it.id]?.vidjeno != true }, ponavljanja = ponavljanja.size)
    }

    /** Za "ponavljanje" sesiju: samo dospjela pitanja, najhitnija prvo. */
    fun zaPonavljanje(dospjela: List<Pitanje>, velicina: Int = 10): Plan =
        Plan(izmijesajTipove(dospjela.take(velicina)), novih = 0, ponavljanja = minOf(dospjela.size, velicina))

    /** Round-robin po tipu, pa lagano miješanje da redoslijed ne bude predvidljiv. */
    fun izmijesajTipove(lista: List<Pitanje>): List<Pitanje> {
        val grupe = TipPitanja.entries.associateWith { t -> lista.filter { it.tip == t }.shuffled().toMutableList() }
        val out = ArrayList<Pitanje>(lista.size)
        val redoslijed = listOf(TipPitanja.ABCD, TipPitanja.TACNO_NETACNO, TipPitanja.DOPUNI, TipPitanja.TACNO_NETACNO)
        var i = 0
        while (out.size < lista.size) {
            val t = redoslijed[i % redoslijed.size]
            grupe[t]?.removeFirstOrNull()?.let { out += it }
            i++
            if (i > lista.size * 8) { grupe.values.forEach { out += it; it.clear() } }
        }
        return out
    }
}
