package ba.osgrivice.kviz.data.repository

import ba.osgrivice.kviz.platform.sadaMs
import ba.osgrivice.kviz.data.local.dao.NapredakDao
import ba.osgrivice.kviz.data.local.dao.SesijaDao
import ba.osgrivice.kviz.data.local.entity.NapredakPitanjaEntity
import ba.osgrivice.kviz.data.local.entity.OdgovorEntity
import ba.osgrivice.kviz.data.local.entity.SesijaEntity
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.model.NapredakPitanja
import ba.osgrivice.kviz.domain.model.VrstaSesije
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Sažetak jedne simulacije takmičenja (za profil, stazu i Firestore). */
data class RezultatSimulacije(
    val tacnih: Int,
    val ukupno: Int,
    val trajanjeMs: Long,
    val pocetakMs: Long
)

/** Bilježenje sesija, odgovora i stanja ponavljanja po pitanju. */
class NapredakRepository(
    private val napredakDao: NapredakDao,
    private val sesijaDao: SesijaDao
) {

    suspend fun zapocniSesiju(vrsta: VrstaSesije, razred: Int, lekcijaId: String?, brojPitanja: Int): Long =
        sesijaDao.ubaciSesiju(
            SesijaEntity(
                vrsta = vrsta.kljuc,
                razred = razred,
                lekcijaId = lekcijaId,
                pocetakMs = sadaMs(),
                krajMs = null,
                ukupno = brojPitanja,
                tacnih = 0,
                xp = 0
            )
        )

    /**
     * Upisuje odgovor i ažurira raspored ponavljanja za to pitanje.
     * [azurirajRaspored] = false samo za istek vremena u simulaciji — odgovor se bilježi, kutija ostaje.
     */
    suspend fun zabiljeziOdgovor(
        sesijaId: Long,
        pitanjeId: String,
        tacno: Boolean,
        odgovor: String,
        azurirajRaspored: Boolean = true
    ) {
        val sada = sadaMs()
        sesijaDao.ubaciOdgovor(
            OdgovorEntity(
                sesijaId = sesijaId,
                pitanjeId = pitanjeId,
                tacno = tacno,
                odgovorKorisnika = odgovor,
                vrijemeMs = sada
            )
        )
        if (!azurirajRaspored) return
        val prije = napredakDao.poPitanju(pitanjeId)?.uDomen() ?: NapredakPitanja(pitanjeId)
        val poslije = RasporedPonavljanja.nakonOdgovora(prije, tacno, RasporedPonavljanja.danasDan(sada), sada)
        napredakDao.upsert(poslije.uEntitet())
    }

    suspend fun zavrsiSesiju(sesijaId: Long, tacnih: Int, xp: Int) {
        val s = sesijaDao.poId(sesijaId) ?: return
        sesijaDao.azurirajSesiju(s.copy(krajMs = sadaMs(), tacnih = tacnih, xp = xp))
    }

    suspend fun napredak(pitanjeIds: List<String>): Map<String, NapredakPitanja> =
        if (pitanjeIds.isEmpty()) emptyMap()
        else napredakDao.poPitanjima(pitanjeIds).associate { it.pitanjeId to it.uDomen() }

    suspend fun dospjelaIds(razred: Int): List<String> =
        napredakDao.dospjelaZaRazred(razred, RasporedPonavljanja.danasDan()).map { it.pitanjeId }

    suspend fun brojDospjelih(razred: Int): Int =
        napredakDao.brojDospjelih(razred, RasporedPonavljanja.danasDan())

    suspend fun greskeIds(razred: Int): List<String> = napredakDao.greskeZaRazred(razred).map { it.pitanjeId }

    suspend fun brojVidjenih(razred: Int): Int = napredakDao.brojVidjenih(razred)

    suspend fun brojSavladanih(razred: Int): Int =
        napredakDao.brojSavladanih(razred, RasporedPonavljanja.SAVLADANO_OD_KUTIJE)

    fun posmatrajUkupniXp(): Flow<Int> = sesijaDao.posmatrajUkupniXp()

    suspend fun aktivniDani(): List<Long> =
        sesijaDao.vremenaSesija().map { RasporedPonavljanja.danasDan(it) }.distinct()

    suspend fun ukupnoOdgovora(): Int = sesijaDao.ukupnoOdgovora()
    suspend fun ukupnoTacnih(): Int = sesijaDao.ukupnoTacnih()
    suspend fun ukupniXp(): Int = sesijaDao.posmatrajUkupniXp().first()

    /** Briše sav napredak učenika (pitanja ostaju). */
    suspend fun resetuj() {
        sesijaDao.obrisiSveSesije()   // odgovori se brišu kaskadno
        napredakDao.obrisiSve()
    }

    suspend fun brojOdgovoraDanas(): Int = sesijaDao.brojOdgovoraOd(pocetakDanasMs())

    fun posmatrajBrojOdgovoraDanas(): Flow<Int> = sesijaDao.posmatrajBrojOdgovoraOd(pocetakDanasMs())

    private fun pocetakDanasMs(): Long = RasporedPonavljanja.pocetakDanaMs(RasporedPonavljanja.danasDan())

    // ---- simulacija takmičenja (korak 6) ----

    suspend fun najboljaSimulacija(razred: Int): RezultatSimulacije? =
        sesijaDao.najboljaSesija(VrstaSesije.SIMULACIJA.kljuc, razred)?.uRezultat()

    suspend fun brojSimulacija(): Int = sesijaDao.brojSesijaVrste(VrstaSesije.SIMULACIJA.kljuc)

    suspend fun zadnjeSimulacije(razred: Int, limit: Int = 5): List<RezultatSimulacije> =
        sesijaDao.zadnjeSesijeVrste(VrstaSesije.SIMULACIJA.kljuc, razred, limit).mapNotNull { it.uRezultat() }

    // ---- mapiranje ----

    private fun SesijaEntity.uRezultat(): RezultatSimulacije? {
        val kraj = krajMs ?: return null
        return RezultatSimulacije(tacnih = tacnih, ukupno = ukupno, trajanjeMs = kraj - pocetakMs, pocetakMs = pocetakMs)
    }

    private fun NapredakPitanjaEntity.uDomen() =
        NapredakPitanja(pitanjeId, kutija, sljedeceDan, tacnih, netacnih, zadnjiOdgovorMs)

    private fun NapredakPitanja.uEntitet() =
        NapredakPitanjaEntity(pitanjeId, kutija, sljedeceDan, tacnih, netacnih, zadnjiOdgovorMs)
}
