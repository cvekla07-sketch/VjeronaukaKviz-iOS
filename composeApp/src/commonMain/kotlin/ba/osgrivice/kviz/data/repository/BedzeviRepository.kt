package ba.osgrivice.kviz.data.repository

import ba.osgrivice.kviz.platform.sadaMs
import ba.osgrivice.kviz.data.local.dao.BedzDao
import ba.osgrivice.kviz.data.local.entity.BedzEntity
import ba.osgrivice.kviz.domain.kviz.Bedz
import ba.osgrivice.kviz.domain.kviz.Bedzevi
import ba.osgrivice.kviz.domain.kviz.Lekcije
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.kviz.SesijaSazetak
import ba.osgrivice.kviz.domain.kviz.StanjeZaBedzeve
import ba.osgrivice.kviz.domain.kviz.Streak

/** Osvojeni bedž s vremenom dodjele. */
data class OsvojenBedz(val bedz: Bedz, val dodijeljenMs: Long)

/**
 * Dodjela bedževa: skupi stanje iz ostalih repozitorija, pusti čistu logiku [Bedzevi.provjeri],
 * upiši nove. Zove se na kraju svake završene sesije.
 */
class BedzeviRepository(
    private val bedzDao: BedzDao,
    private val pitanja: PitanjaRepository,
    private val napredak: NapredakRepository
) {

    suspend fun osvojeni(): List<OsvojenBedz> =
        bedzDao.svi().mapNotNull { e -> Bedzevi.poId(e.id)?.let { OsvojenBedz(it, e.dodijeljenMs) } }

    suspend fun osvojeniIds(): Set<String> = bedzDao.ids().toHashSet()

    /**
     * Provjerava uslove i upisuje nove bedževe. Vraća samo NOVE (za animaciju dodjele).
     * [zadnjaSesija] = sesija koja je upravo završena (null kad se zove bez sesije, npr. pri startu).
     */
    suspend fun provjeriIDodijeli(razred: Int, zadnjaSesija: SesijaSazetak?): List<Bedz> {
        val stanje = izracunajStanje(razred, zadnjaSesija)
        val novi = Bedzevi.provjeri(stanje, osvojeniIds())
        if (novi.isEmpty()) return emptyList()
        val sada = sadaMs()
        bedzDao.dodaj(novi.map { BedzEntity(id = it.id, dodijeljenMs = sada) })
        return novi
    }

    private suspend fun izracunajStanje(razred: Int, zadnjaSesija: SesijaSazetak?): StanjeZaBedzeve {
        val lekcije = pitanja.lekcije(razred)
        val sva = pitanja.svaZaRazred(razred)
        val nap = napredak.napredak(sva.map { it.id })
        val poLekciji = sva.filter { it.lekcijaId != null }.groupBy({ it.lekcijaId!! }, { it.id })
        val staza = Lekcije.izracunajStazu(lekcije, poLekciji, nap)
        val sim = napredak.najboljaSimulacija(razred)
        return StanjeZaBedzeve(
            zvjezdicePoLekciji = staza.map { it.zvjezdice },
            streak = Streak.izracunaj(napredak.aktivniDani(), RasporedPonavljanja.danasDan()),
            ukupnoTacnih = napredak.ukupnoTacnih(),
            brojSimulacija = napredak.brojSimulacija(),
            najboljaSimulacijaTacnih = sim?.tacnih,
            najboljaSimulacijaUkupno = sim?.ukupno,
            zadnjaSesija = zadnjaSesija
        )
    }

    suspend fun nesinhronizovani(): List<BedzEntity> = bedzDao.nesinhronizovani()
    suspend fun oznaciSinhronizovane(ids: List<String>) = bedzDao.oznaciSinhronizovane(ids)

    /** Samo uz "Resetuj napredak". */
    suspend fun obrisiSve() = bedzDao.obrisiSve()
}
