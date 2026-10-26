package ba.osgrivice.kviz.data.seed

import ba.osgrivice.kviz.data.local.KvizDatabase
import ba.osgrivice.kviz.data.local.entity.LekcijaEntity
import ba.osgrivice.kviz.data.local.entity.MetaEntity
import ba.osgrivice.kviz.data.local.entity.PitanjeEntity
import ba.osgrivice.kviz.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Puni bazu pitanjima iz pitanja_seed.json (iOS: composeResources/files/).
 * Uvozi samo kad je verzija seed fajla veća od one zapisane u bazi (meta.seed_verzija),
 * pa nova verzija aplikacije može donijeti nova/ispravljena pitanja bez brisanja napretka.
 */
class SeedImporter(
    private val db: KvizDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uvezAkoTreba(): Boolean = withContext(Dispatchers.IO) {
        // iOS: fajl je u composeResources/files (na Androidu u assets/)
        val tekst = Res.readBytes("files/" + SEED_FAJL).decodeToString()
        val seed = json.decodeFromString<SeedFajl>(tekst)

        val trenutna = db.metaDao().get(META_SEED_VERZIJA)?.toIntOrNull() ?: 0
        if (seed.verzija <= trenutna) return@withContext false

        db.lekcijaDao().upsertSve(seed.lekcije.map {
            LekcijaEntity(it.id, it.razred, it.redniBroj, it.naslov, it.opis)
        })
        db.pitanjeDao().upsertSve(seed.pitanja.map {
            PitanjeEntity(
                id = it.id,
                razred = it.razred,
                lekcijaId = it.lekcija,
                tip = it.tip,
                tekst = it.tekst,
                opcijeJson = json.encodeToString(it.opcije),
                tacno = it.tacno,
                objasnjenje = it.objasnjenje,
                aktivno = it.aktivno,
                verzija = seed.verzija,
                izvor = "seed"
            )
        })
        db.metaDao().set(MetaEntity(META_SEED_VERZIJA, seed.verzija.toString()))
        true
    }

    companion object {
        const val SEED_FAJL = "pitanja_seed.json"
        const val META_SEED_VERZIJA = "seed_verzija"
    }
}
