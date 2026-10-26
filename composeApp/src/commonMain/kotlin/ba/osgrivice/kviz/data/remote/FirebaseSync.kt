package ba.osgrivice.kviz.data.remote

import ba.osgrivice.kviz.data.local.dao.SesijaDao
import ba.osgrivice.kviz.data.prefs.ProfilStore
import ba.osgrivice.kviz.data.repository.BedzeviRepository
import ba.osgrivice.kviz.data.repository.NapredakRepository
import ba.osgrivice.kviz.data.repository.PitanjaRepository
import ba.osgrivice.kviz.domain.kviz.Lekcije
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.kviz.Streak
import ba.osgrivice.kviz.data.remote.rest.Firebase
import ba.osgrivice.kviz.data.remote.rest.Pisanje
import ba.osgrivice.kviz.data.remote.rest.ServerVrijeme
import ba.osgrivice.kviz.platform.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RangUnos(
    val uid: String,
    val ime: String,
    val xp: Int,
    val streak: Int,
    val skola: String,
    val mjesto: Int,
    /** Najbolja simulacija takmičenja: broj tačnih od 20 (null = nije radio simulaciju). */
    val simTacnih: Int? = null,
    val simTrajanjeMs: Long? = null
)

/**
 * Sinhronizacija s Firestoreom. Lokalna baza je izvor istine; ovdje se samo šalje gore.
 *
 * Struktura:
 *   ucenici/{uid}                      profil + agregati (xp, streak, savladano, simulacija{...}, zadnjaAktivnost)
 *   ucenici/{uid}/sesije/{sesijaId}    završene sesije (vrsta, razred, lekcija, ukupno, tacnih, xp, pocetak, kraj)
 *   ucenici/{uid}/lekcije/{lekcijaId}  zvjezdice, savladano, ukupno
 *   ucenici/{uid}/bedzevi/{bedzId}     dodijeljenMs (korak 6b)
 *   rang/{razred}/ucenici/{uid}        ime, xp, streak, skola, simTacnih, simTrajanjeMs — za rang-listu (orderBy xp)
 */
class FirebaseSync(
    private val profilStore: ProfilStore,
    private val napredak: NapredakRepository,
    private val pitanja: PitanjaRepository,
    private val bedzevi: BedzeviRepository,
    private val sesijaDao: SesijaDao,
    private val verzijaApp: String
) {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val brava = Mutex()

    val uid: String? get() = auth.uid

    /** Anonimna prijava — jednom po instalaciji; vraća uid ili null (nema mreže). */
    suspend fun prijavi(): String? {
        auth.uid?.let { return it }
        return runCatching { auth.prijavi() }
            .onFailure { Log.w(TAG, "Prijava nije uspjela: ${it.message}") }
            .getOrNull()
    }

    /** Šalje sve što nije poslano. Bezopasno zvati često; nikad ne baca. */
    suspend fun sinhronizuj() {
        val uid = prijavi() ?: return
        brava.withLock {
            runCatching { posaljiProfil(uid) }.onFailure { Log.w(TAG, "profil: ${it.message}") }
            runCatching { posaljiSesije(uid) }.onFailure { Log.w(TAG, "sesije: ${it.message}") }
            runCatching { posaljiLekcije(uid) }.onFailure { Log.w(TAG, "lekcije: ${it.message}") }
            runCatching { posaljiBedzeve(uid) }.onFailure { Log.w(TAG, "bedzevi: ${it.message}") }
        }
    }

    private suspend fun posaljiProfil(uid: String) {
        val p = profilStore.trenutni()
        if (!p.podesen) return
        val danas = RasporedPonavljanja.danasDan()
        val xp = napredak.ukupniXp()
        val streak = Streak.izracunaj(napredak.aktivniDani(), danas)
        val savladano = napredak.brojSavladanih(p.razred)
        val sim = napredak.najboljaSimulacija(p.razred)
        val brojSim = napredak.brojSimulacija()

        val profil = mutableMapOf<String, Any?>(
            "ime" to p.ime,
            "razred" to p.razred,
            "skola" to p.skola,
            "xp" to xp,
            "streak" to streak,
            "savladano" to savladano,
            "odgovora" to napredak.ukupnoOdgovora(),
            "tacnih" to napredak.ukupnoTacnih(),
            "dnevniCilj" to p.dnevniCilj,
            "verzijaApp" to verzijaApp,
            "brojSimulacija" to brojSim,
            "brojBedzeva" to bedzevi.osvojeniIds().size,
            "zadnjaAktivnost" to ServerVrijeme
        )
        if (sim != null) {
            profil["simulacija"] = mapOf(
                "tacnih" to sim.tacnih, "ukupno" to sim.ukupno,
                "trajanjeMs" to sim.trajanjeMs, "pocetakMs" to sim.pocetakMs
            )
        }
        db.commit(listOf(Pisanje("ucenici/$uid", profil, merge = true)))

        val rang = mutableMapOf<String, Any?>(
            "ime" to p.ime, "xp" to xp, "streak" to streak, "skola" to p.skola,
            "azurirano" to ServerVrijeme
        )
        if (sim != null) {
            rang["simTacnih"] = sim.tacnih
            rang["simTrajanjeMs"] = sim.trajanjeMs
        }
        db.commit(listOf(Pisanje("rang/${p.razred}/ucenici/$uid", rang)))
    }

    private suspend fun posaljiSesije(uid: String) {
        val nove = sesijaDao.nesinhronizovane()
        if (nove.isEmpty()) return
        db.commit(nove.map { s ->
            Pisanje(
                "ucenici/$uid/sesije/${s.id}", mapOf(
                    "vrsta" to s.vrsta, "razred" to s.razred, "lekcijaId" to s.lekcijaId,
                    "pocetakMs" to s.pocetakMs, "krajMs" to s.krajMs,
                    "ukupno" to s.ukupno, "tacnih" to s.tacnih, "xp" to s.xp
                )
            )
        })
        sesijaDao.oznaciSinhronizovane(nove.map { it.id })
    }

    private suspend fun posaljiLekcije(uid: String) {
        val p = profilStore.trenutni()
        if (!p.podesen) return
        val sva = pitanja.svaZaRazred(p.razred)
        val nap = napredak.napredak(sva.map { it.id })
        val poLekciji = sva.filter { it.lekcijaId != null }.groupBy({ it.lekcijaId!! }, { it.id })
        val staza = Lekcije.izracunajStazu(pitanja.lekcije(p.razred), poLekciji, nap)
        db.commit(staza.filter { it.vidjeno > 0 }.map { l ->
            Pisanje("ucenici/$uid/lekcije/${l.lekcija.id}", mapOf("zvjezdice" to l.zvjezdice, "savladano" to l.savladano, "vidjeno" to l.vidjeno, "ukupno" to l.ukupno, "razred" to p.razred))
        })
    }

    private suspend fun posaljiBedzeve(uid: String) {
        val novi = bedzevi.nesinhronizovani()
        if (novi.isEmpty()) return
        db.commit(novi.map { b ->
            Pisanje("ucenici/$uid/bedzevi/${b.id}", mapOf("dodijeljenMs" to b.dodijeljenMs))
        })
        bedzevi.oznaciSinhronizovane(novi.map { it.id })
    }

    /** Top N za razred + pozicija trenutnog učenika (ili null ako nije u top N). */
    suspend fun rangLista(razred: Int, limit: Int = 50): List<RangUnos> {
        val snap = db.upit("rang/$razred/ucenici", poredak = "xp", opadajuce = true, limit = limit)
        return snap.mapIndexed { i, d ->
            RangUnos(
                uid = d.id,
                ime = d.getString("ime").orEmpty().ifBlank { "Učenik" },
                xp = (d.getLong("xp") ?: 0L).toInt(),
                streak = (d.getLong("streak") ?: 0L).toInt(),
                skola = d.getString("skola").orEmpty(),
                mjesto = i + 1,
                simTacnih = d.getLong("simTacnih")?.toInt(),
                simTrajanjeMs = d.getLong("simTrajanjeMs")
            )
        }
    }

    companion object { private const val TAG = "FirebaseSync" }
}
