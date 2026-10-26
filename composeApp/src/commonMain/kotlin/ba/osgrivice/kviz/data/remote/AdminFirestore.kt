package ba.osgrivice.kviz.data.remote

import ba.osgrivice.kviz.data.remote.rest.Filter
import ba.osgrivice.kviz.data.remote.rest.Firebase
import ba.osgrivice.kviz.data.remote.rest.FirestoreIzuzetak

/** Red u nastavničkoj listi — sve iz dokumenta ucenici/{uid}. */
data class UcenikPregled(
    val uid: String,
    val ime: String,
    val razred: Int,
    val skola: String,
    val xp: Int,
    val streak: Int,
    val savladano: Int,
    val odgovora: Int,
    val tacnih: Int,
    val dnevniCilj: Int,
    val brojSimulacija: Int,
    val simTacnih: Int?,
    val simUkupno: Int?,
    val simTrajanjeMs: Long?,
    val brojBedzeva: Int,
    val zadnjaAktivnostMs: Long?,
    val verzijaApp: String
) {
    val procenatTacnih: Int get() = if (odgovora == 0) 0 else tacnih * 100 / odgovora
}

data class LekcijaPregled(val lekcijaId: String, val zvjezdice: Int, val savladano: Int, val vidjeno: Int, val ukupno: Int)

data class SesijaPregled(val vrsta: String, val lekcijaId: String?, val pocetakMs: Long, val krajMs: Long?, val ukupno: Int, val tacnih: Int, val xp: Int)

data class BedzPregled(val id: String, val dodijeljenMs: Long)

class NemaPristupaException : Exception("Nema pristupa")

/**
 * Čitanje podataka za nastavnički pregled (korak 7). Radi samo ako je uid nastavnika
 * upisan u config/admin.uids — inače Firestore vraća PERMISSION_DENIED → [NemaPristupaException].
 */
class AdminFirestore {
    private val db = Firebase.firestore

    /** PIN iz config/admin (null = dokument ne postoji ili nema polja). */
    suspend fun pin(): String? = prevedi {
        db.dokument("config/admin")?.getString("pin")
    }

    /** Svi učenici razreda; sortiranje radi UI (bez složenog indeksa). */
    suspend fun ucenici(razred: Int): List<UcenikPregled> = prevedi {
        val snap = db.upit("ucenici", listOf(Filter.jednako("razred", razred.toLong())))
        snap.map { d ->
            val sim = d.get("simulacija") as? Map<*, *>
            UcenikPregled(
                uid = d.id,
                ime = d.getString("ime").orEmpty().ifBlank { "Učenik" },
                razred = (d.getLong("razred") ?: 0L).toInt(),
                skola = d.getString("skola").orEmpty(),
                xp = (d.getLong("xp") ?: 0L).toInt(),
                streak = (d.getLong("streak") ?: 0L).toInt(),
                savladano = (d.getLong("savladano") ?: 0L).toInt(),
                odgovora = (d.getLong("odgovora") ?: 0L).toInt(),
                tacnih = (d.getLong("tacnih") ?: 0L).toInt(),
                dnevniCilj = (d.getLong("dnevniCilj") ?: 0L).toInt(),
                brojSimulacija = (d.getLong("brojSimulacija") ?: 0L).toInt(),
                simTacnih = (sim?.get("tacnih") as? Number)?.toInt(),
                simUkupno = (sim?.get("ukupno") as? Number)?.toInt(),
                simTrajanjeMs = (sim?.get("trajanjeMs") as? Number)?.toLong(),
                brojBedzeva = (d.getLong("brojBedzeva") ?: 0L).toInt(),
                zadnjaAktivnostMs = d.getTimestampMs("zadnjaAktivnost"),
                verzijaApp = d.getString("verzijaApp").orEmpty()
            )
        }
    }

    suspend fun lekcije(uid: String): List<LekcijaPregled> = prevedi {
        db.kolekcija("ucenici/$uid/lekcije").map { d ->
            LekcijaPregled(
                lekcijaId = d.id,
                zvjezdice = (d.getLong("zvjezdice") ?: 0L).toInt(),
                savladano = (d.getLong("savladano") ?: 0L).toInt(),
                vidjeno = (d.getLong("vidjeno") ?: 0L).toInt(),
                ukupno = (d.getLong("ukupno") ?: 0L).toInt()
            )
        }
    }

    suspend fun sesije(uid: String, limit: Int = 40): List<SesijaPregled> = prevedi {
        db.upit("ucenici/$uid/sesije", poredak = "pocetakMs", opadajuce = true, limit = limit)
            .map { d ->
                SesijaPregled(
                    vrsta = d.getString("vrsta").orEmpty(),
                    lekcijaId = d.getString("lekcijaId"),
                    pocetakMs = d.getLong("pocetakMs") ?: 0L,
                    krajMs = d.getLong("krajMs"),
                    ukupno = (d.getLong("ukupno") ?: 0L).toInt(),
                    tacnih = (d.getLong("tacnih") ?: 0L).toInt(),
                    xp = (d.getLong("xp") ?: 0L).toInt()
                )
            }
    }

    suspend fun bedzevi(uid: String): List<BedzPregled> = prevedi {
        db.kolekcija("ucenici/$uid/bedzevi").map { d ->
            BedzPregled(id = d.id, dodijeljenMs = d.getLong("dodijeljenMs") ?: 0L)
        }
    }

    private inline fun <T> prevedi(blok: () -> T): T = try {
        blok()
    } catch (e: FirestoreIzuzetak) {
        if (e.status == "PERMISSION_DENIED") throw NemaPristupaException() else throw e
    }
}
