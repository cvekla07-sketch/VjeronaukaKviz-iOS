package ba.osgrivice.kviz.data.remote

import ba.osgrivice.kviz.data.local.KvizDatabase
import ba.osgrivice.kviz.data.local.entity.LekcijaEntity
import ba.osgrivice.kviz.data.local.entity.MetaEntity
import ba.osgrivice.kviz.data.local.entity.PitanjeEntity
import ba.osgrivice.kviz.data.seed.SeedImporter
import ba.osgrivice.kviz.data.remote.rest.Filter
import ba.osgrivice.kviz.data.remote.rest.Firebase
import ba.osgrivice.kviz.platform.Log
import ba.osgrivice.kviz.platform.sadaMs
import kotlinx.serialization.json.Json

/**
 * Ispravke i nova pitanja iz Admin Toola — bez nove verzije aplikacije.
 *
 * Alat pri svakoj objavi podigne config/sadrzaj.verzija i tu verziju upiše u promijenjene
 * dokumente pitanja/{id} i lekcije/{id}. Aplikacija:
 *   1. pročita config/sadrzaj (1 čitanje),
 *   2. ako je verzija veća od lokalne → povuče samo dokumente s verzija > lokalna,
 *   3. upsert u Room (izvor = "cloud"), zapamti verziju.
 *
 * Ako nova verzija aplikacije donese novi seed (prebriše pitanja), lokalna cloud verzija se
 * resetuje pa se sve cloud izmjene ponovo primijene preko seed-a.
 */
class CloudSadrzaj(private val db: KvizDatabase) {

    private val fs = Firebase.firestore
    private val json = Json { ignoreUnknownKeys = true }
    private var zadnjaProvjeraMs = 0L

    /** Najviše jednom u [RAZMAK_MS] (otvaranje staze). Vraća true ako je nešto ažurirano. Nikad ne baca. */
    suspend fun sinhronizuj(): Boolean {
        val sada = sadaMs()
        if (sada - zadnjaProvjeraMs < RAZMAK_MS) return false
        zadnjaProvjeraMs = sada
        return runCatching { uradi() }
            .onFailure { Log.w(TAG, "cloud sadržaj: ${it.message}") }
            .getOrDefault(false)
    }

    private suspend fun uradi(): Boolean {
        Firebase.auth.prijavi()
        val meta = db.metaDao()
        val seedVerzija = meta.get(SeedImporter.META_SEED_VERZIJA) ?: "0"
        var lokalna = meta.get(META_CLOUD_VERZIJA)?.toLongOrNull() ?: 0L
        if (meta.get(META_SEED_PRI_CLOUDU) != seedVerzija) lokalna = 0L   // novi seed → ponovo sve

        val cfg = fs.dokument("config/sadrzaj")
        val udaljena = cfg?.getLong("verzija") ?: 0L
        if (udaljena <= lokalna) {
            meta.set(MetaEntity(META_SEED_PRI_CLOUDU, seedVerzija))
            return false
        }

        val lekcije = fs.upit("lekcije", listOf(Filter.veceOd("verzija", lokalna)))
            .mapNotNull { d ->
                LekcijaEntity(
                    id = d.id,
                    razred = (d.getLong("razred") ?: return@mapNotNull null).toInt(),
                    redniBroj = (d.getLong("redniBroj") ?: 0L).toInt(),
                    naslov = d.getString("naslov") ?: return@mapNotNull null,
                    opis = d.getString("opis")
                )
            }
        val pitanja = fs.upit("pitanja", listOf(Filter.veceOd("verzija", lokalna)))
            .mapNotNull { d -> uEntitet(d.id, d.data) }

        if (lekcije.isNotEmpty()) db.lekcijaDao().upsertSve(lekcije)
        if (pitanja.isNotEmpty()) db.pitanjeDao().upsertSve(pitanja)

        meta.set(MetaEntity(META_CLOUD_VERZIJA, udaljena.toString()))
        meta.set(MetaEntity(META_SEED_PRI_CLOUDU, seedVerzija))
        Log.i(TAG, "cloud sadržaj v$udaljena: ${pitanja.size} pitanja, ${lekcije.size} lekcija")
        return pitanja.isNotEmpty() || lekcije.isNotEmpty()
    }

    /**
     * Dokument → entitet. Dokument koji je samo deaktiviran (merge: aktivno+verzija) nema ostala polja —
     * tada se preuzme postojeći red iz baze i samo mu se promijeni aktivno.
     */
    private suspend fun uEntitet(id: String, d: Map<String, Any?>): PitanjeEntity? {
        val verzija = (d["verzija"] as? Number)?.toInt() ?: 0
        val aktivno = d["aktivno"] as? Boolean ?: true
        val tekst = d["tekst"] as? String
        if (tekst == null) {
            val postojece = db.pitanjeDao().poId(id) ?: return null
            return postojece.copy(aktivno = aktivno, verzija = verzija, izvor = "cloud")
        }
        @Suppress("UNCHECKED_CAST")
        val opcije = (d["opcije"] as? List<Any?>)?.map { it?.toString() ?: "" } ?: emptyList()
        return PitanjeEntity(
            id = id,
            razred = (d["razred"] as? Number)?.toInt() ?: return null,
            lekcijaId = d["lekcija"] as? String,
            tip = d["tip"] as? String ?: "abcd",
            tekst = tekst,
            opcijeJson = json.encodeToString(opcije),
            tacno = d["tacno"] as? String ?: "",
            objasnjenje = d["objasnjenje"] as? String,
            aktivno = aktivno,
            verzija = verzija,
            izvor = "cloud"
        )
    }

    companion object {
        private const val TAG = "CloudSadrzaj"
        private const val RAZMAK_MS = 5 * 60 * 1000L
        const val META_CLOUD_VERZIJA = "cloud_verzija"
        const val META_SEED_PRI_CLOUDU = "cloud_seed_verzija"
    }
}
