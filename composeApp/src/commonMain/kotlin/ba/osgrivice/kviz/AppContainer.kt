package ba.osgrivice.kviz

import ba.osgrivice.kviz.data.local.KvizDatabase
import ba.osgrivice.kviz.data.prefs.ProfilStore
import ba.osgrivice.kviz.data.repository.BedzeviRepository
import ba.osgrivice.kviz.data.repository.NapredakRepository
import ba.osgrivice.kviz.data.repository.PitanjaRepository
import ba.osgrivice.kviz.data.seed.SeedImporter
import ba.osgrivice.kviz.platform.verzijaAplikacije

/**
 * Ručni DI kontejner — jedno mjesto gdje se prave baza i repozitoriji.
 * ViewModel-i ga dobijaju preko KvizApp.container (iOS: KvizApp je object).
 */
class AppContainer {

    val db: KvizDatabase by lazy { KvizDatabase.kreiraj() }

    val profilStore: ProfilStore by lazy { ProfilStore() }

    val seedImporter: SeedImporter by lazy { SeedImporter(db) }

    val pitanjaRepository: PitanjaRepository by lazy {
        PitanjaRepository(db.pitanjeDao(), db.lekcijaDao())
    }

    val napredakRepository: NapredakRepository by lazy {
        NapredakRepository(db.napredakDao(), db.sesijaDao())
    }

    val bedzeviRepository: BedzeviRepository by lazy {
        BedzeviRepository(db.bedzDao(), pitanjaRepository, napredakRepository)
    }

    // Dio 4: firebaseSync, adminFirestore, cloudSadrzaj (Firebase)

    /** versionName iz manifesta — prikaz na profilu i slanje u Firestore. */
    val verzija: String = runCatching { verzijaAplikacije() }.getOrDefault("?")

    /** Poziva se jednom pri startu — puni bazu iz seed fajla ako je potrebno. */
    suspend fun inicijalizuj() {
        seedImporter.uvezAkoTreba()
    }
}
