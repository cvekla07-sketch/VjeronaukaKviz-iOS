package ba.osgrivice.kviz.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Ključ/vrijednost za interne podatke (verzija seed-a i sl.). */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val kljuc: String,
    val vrijednost: String
)

@Entity(
    tableName = "lekcije",
    indices = [Index(value = ["razred", "redniBroj"])]
)
data class LekcijaEntity(
    @PrimaryKey val id: String,
    val razred: Int,
    val redniBroj: Int,
    val naslov: String,
    val opis: String?
)

@Entity(
    tableName = "pitanja",
    indices = [
        Index(value = ["razred", "tip"]),
        Index(value = ["lekcijaId"])
    ]
)
data class PitanjeEntity(
    @PrimaryKey val id: String,
    val razred: Int,
    val lekcijaId: String?,
    val tip: String,
    val tekst: String,
    /** JSON lista stringova. */
    val opcijeJson: String,
    val tacno: String,
    val objasnjenje: String?,
    @ColumnInfo(defaultValue = "1") val aktivno: Boolean = true,
    @ColumnInfo(defaultValue = "1") val verzija: Int = 1,
    /** "seed" ili "cloud" — odakle je zadnji put došlo. */
    @ColumnInfo(defaultValue = "seed") val izvor: String = "seed"
)

@Entity(
    tableName = "napredak_pitanja",
    indices = [Index(value = ["sljedeceDan"])]
)
data class NapredakPitanjaEntity(
    @PrimaryKey val pitanjeId: String,
    val kutija: Int,
    val sljedeceDan: Long,
    val tacnih: Int,
    val netacnih: Int,
    val zadnjiOdgovorMs: Long
)

@Entity(
    tableName = "sesije",
    indices = [Index(value = ["pocetakMs"]), Index(value = ["sinhronizovano"])]
)
data class SesijaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vrsta: String,
    val razred: Int,
    val lekcijaId: String?,
    val pocetakMs: Long,
    val krajMs: Long?,
    val ukupno: Int,
    val tacnih: Int,
    val xp: Int,
    @ColumnInfo(defaultValue = "0") val sinhronizovano: Boolean = false
)

@Entity(
    tableName = "odgovori",
    foreignKeys = [
        ForeignKey(
            entity = SesijaEntity::class,
            parentColumns = ["id"],
            childColumns = ["sesijaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sesijaId"]), Index(value = ["pitanjeId"])]
)
data class OdgovorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesijaId: Long,
    val pitanjeId: String,
    val tacno: Boolean,
    val odgovorKorisnika: String,
    val vrijemeMs: Long
)
