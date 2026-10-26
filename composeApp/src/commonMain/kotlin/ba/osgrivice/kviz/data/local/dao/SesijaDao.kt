package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ba.osgrivice.kviz.data.local.entity.OdgovorEntity
import ba.osgrivice.kviz.data.local.entity.SesijaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SesijaDao {

    @Insert
    suspend fun ubaciSesiju(sesija: SesijaEntity): Long

    @Update
    suspend fun azurirajSesiju(sesija: SesijaEntity)

    @Query("SELECT * FROM sesije WHERE id = :id LIMIT 1")
    suspend fun poId(id: Long): SesijaEntity?

    @Insert
    suspend fun ubaciOdgovor(odgovor: OdgovorEntity): Long

    @Query("SELECT * FROM odgovori WHERE sesijaId = :sesijaId ORDER BY id")
    suspend fun odgovoriSesije(sesijaId: Long): List<OdgovorEntity>

    @Query("SELECT * FROM sesije WHERE krajMs IS NOT NULL ORDER BY pocetakMs DESC LIMIT :limit")
    fun posmatrajZadnje(limit: Int): Flow<List<SesijaEntity>>

    @Query("SELECT * FROM sesije WHERE krajMs IS NOT NULL AND sinhronizovano = 0 ORDER BY pocetakMs")
    suspend fun nesinhronizovane(): List<SesijaEntity>

    @Query("UPDATE sesije SET sinhronizovano = 1 WHERE id IN (:ids)")
    suspend fun oznaciSinhronizovane(ids: List<Long>)

    /** Ukupno XP iz završenih sesija. */
    @Query("SELECT COALESCE(SUM(xp), 0) FROM sesije WHERE krajMs IS NOT NULL")
    fun posmatrajUkupniXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(ukupno), 0) FROM sesije WHERE krajMs IS NOT NULL AND razred = :razred")
    suspend fun ukupnoOdgovorenih(razred: Int): Int

    @Query("SELECT COUNT(*) FROM odgovori WHERE vrijemeMs >= :odMs")
    suspend fun brojOdgovoraOd(odMs: Long): Int

    @Query("SELECT COUNT(*) FROM odgovori WHERE vrijemeMs >= :odMs")
    fun posmatrajBrojOdgovoraOd(odMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM odgovori")
    suspend fun ukupnoOdgovora(): Int

    @Query("SELECT COUNT(*) FROM odgovori WHERE tacno = 1")
    suspend fun ukupnoTacnih(): Int

    @Query("DELETE FROM sesije")
    suspend fun obrisiSveSesije()

    /** Početna vremena završenih sesija — repozitorij ih pretvara u lokalne dane (za streak). */
    @Query("SELECT pocetakMs FROM sesije WHERE krajMs IS NOT NULL ORDER BY pocetakMs DESC")
    suspend fun vremenaSesija(): List<Long>

    // ---- simulacija takmičenja (korak 6) ----

    /** Najbolja završena sesija date vrste za razred: najviše tačnih, pa najkraće trajanje. */
    @Query(
        """
        SELECT * FROM sesije
        WHERE vrsta = :vrsta AND razred = :razred AND krajMs IS NOT NULL
        ORDER BY tacnih DESC, (krajMs - pocetakMs) ASC
        LIMIT 1
        """
    )
    suspend fun najboljaSesija(vrsta: String, razred: Int): SesijaEntity?

    @Query("SELECT COUNT(*) FROM sesije WHERE vrsta = :vrsta AND krajMs IS NOT NULL")
    suspend fun brojSesijaVrste(vrsta: String): Int

    @Query("SELECT * FROM sesije WHERE vrsta = :vrsta AND razred = :razred AND krajMs IS NOT NULL ORDER BY pocetakMs DESC LIMIT :limit")
    suspend fun zadnjeSesijeVrste(vrsta: String, razred: Int, limit: Int): List<SesijaEntity>
}
