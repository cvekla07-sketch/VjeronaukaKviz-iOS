package ba.osgrivice.kviz.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import ba.osgrivice.kviz.data.local.dao.BedzDao
import ba.osgrivice.kviz.data.local.dao.LekcijaDao
import ba.osgrivice.kviz.data.local.dao.MetaDao
import ba.osgrivice.kviz.data.local.dao.NapredakDao
import ba.osgrivice.kviz.data.local.dao.PitanjeDao
import ba.osgrivice.kviz.data.local.dao.SesijaDao
import ba.osgrivice.kviz.data.local.entity.BedzEntity
import ba.osgrivice.kviz.data.local.entity.LekcijaEntity
import ba.osgrivice.kviz.data.local.entity.MetaEntity
import ba.osgrivice.kviz.data.local.entity.NapredakPitanjaEntity
import ba.osgrivice.kviz.data.local.entity.OdgovorEntity
import ba.osgrivice.kviz.data.local.entity.PitanjeEntity
import ba.osgrivice.kviz.data.local.entity.SesijaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Lokalna baza — izvor istine na telefonu.
 *
 * PRAVILO ZA NAREDNE GODINE: svaka promjena šeme = version + 1 i nova Migration u listi
 * [MIGRACIJE]. Nikad fallbackToDestructiveMigration u produkciji — učenik ne smije izgubiti napredak.
 *
 * Historija:
 *  v1 — temelj (meta, lekcije, pitanja, napredak_pitanja, sesije, odgovori)
 *  v2 — tabela bedzevi (korak 6b)
 *
 * iOS: aplikacija na iPhoneu kreće odmah od verzije 2 (nema starih instalacija v1),
 * pa MIGRACIJA_1_2 nije potrebna. Buduće migracije (2→3…) se ovdje pišu preko
 * Migration.migrate(connection: SQLiteConnection) — isti SQL kao na Androidu.
 */
@Database(
    entities = [
        MetaEntity::class,
        LekcijaEntity::class,
        PitanjeEntity::class,
        NapredakPitanjaEntity::class,
        SesijaEntity::class,
        OdgovorEntity::class,
        BedzEntity::class
    ],
    version = 2,
    exportSchema = false
)
@ConstructedBy(KvizDatabaseKonstruktor::class)
abstract class KvizDatabase : RoomDatabase() {

    abstract fun metaDao(): MetaDao
    abstract fun lekcijaDao(): LekcijaDao
    abstract fun pitanjeDao(): PitanjeDao
    abstract fun napredakDao(): NapredakDao
    abstract fun sesijaDao(): SesijaDao
    abstract fun bedzDao(): BedzDao

    companion object {
        const val IME = "vjeronauka_kviz.db"

        fun kreiraj(): KvizDatabase =
            bazaBuilder(IME)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}

/** Room generiše stvarnu implementaciju za iOS. */
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object KvizDatabaseKonstruktor : RoomDatabaseConstructor<KvizDatabase> {
    override fun initialize(): KvizDatabase
}

/** Putanja baze je različita po platformi (iOS: Documents folder aplikacije). */
expect fun bazaBuilder(ime: String): RoomDatabase.Builder<KvizDatabase>
