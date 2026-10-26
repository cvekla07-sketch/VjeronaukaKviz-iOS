package ba.osgrivice.kviz.data.repository

import ba.osgrivice.kviz.data.local.dao.LekcijaDao
import ba.osgrivice.kviz.data.local.dao.PitanjeDao
import ba.osgrivice.kviz.data.local.entity.LekcijaEntity
import ba.osgrivice.kviz.data.local.entity.PitanjeEntity
import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Pristup pitanjima i lekcijama — UI i logika kviza rade samo s domenskim modelima. */
class PitanjaRepository(
    private val pitanjeDao: PitanjeDao,
    private val lekcijaDao: LekcijaDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun brojSvih(): Int = pitanjeDao.brojSvih()
    suspend fun brojZaRazred(razred: Int): Int = pitanjeDao.brojZaRazred(razred)
    suspend fun brojZaRazredITip(razred: Int, tip: TipPitanja): Int =
        pitanjeDao.brojZaRazredITip(razred, tip.kljuc)

    fun posmatrajBrojZaRazred(razred: Int): Flow<Int> = pitanjeDao.posmatrajBrojZaRazred(razred)

    suspend fun poId(id: String): Pitanje? = pitanjeDao.poId(id)?.uDomen()
    suspend fun poIds(ids: List<String>): List<Pitanje> =
        if (ids.isEmpty()) emptyList() else pitanjeDao.poIds(ids).map { it.uDomen() }

    suspend fun svaZaRazred(razred: Int): List<Pitanje> =
        pitanjeDao.svaZaRazred(razred).map { it.uDomen() }

    suspend fun zaRazredITip(razred: Int, tip: TipPitanja): List<Pitanje> =
        pitanjeDao.zaRazredITip(razred, tip.kljuc).map { it.uDomen() }

    suspend fun zaLekciju(lekcijaId: String): List<Pitanje> =
        pitanjeDao.zaLekciju(lekcijaId).map { it.uDomen() }

    suspend fun nevidjenaZaRazred(razred: Int): List<Pitanje> =
        pitanjeDao.nevidjenaZaRazred(razred).map { it.uDomen() }

    fun posmatrajLekcije(razred: Int): Flow<List<Lekcija>> =
        lekcijaDao.posmatrajZaRazred(razred).map { lista -> lista.map { it.uDomen() } }

    suspend fun lekcije(razred: Int): List<Lekcija> = lekcijaDao.zaRazred(razred).map { it.uDomen() }
    suspend fun lekcija(id: String): Lekcija? = lekcijaDao.poId(id)?.uDomen()

    // ---- mapiranje ----

    private fun PitanjeEntity.uDomen(): Pitanje = Pitanje(
        id = id,
        razred = razred,
        lekcijaId = lekcijaId,
        tip = TipPitanja.izKljuca(tip),
        tekst = tekst,
        opcije = runCatching { json.decodeFromString<List<String>>(opcijeJson) }.getOrDefault(emptyList()),
        tacno = tacno,
        objasnjenje = objasnjenje,
        aktivno = aktivno,
        verzija = verzija
    )

    private fun LekcijaEntity.uDomen(): Lekcija = Lekcija(id, razred, redniBroj, naslov, opis)
}
