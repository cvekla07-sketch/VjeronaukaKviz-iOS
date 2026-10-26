package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ba.osgrivice.kviz.data.local.entity.PitanjeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PitanjeDao {

    @Query("SELECT COUNT(*) FROM pitanja WHERE aktivno = 1")
    suspend fun brojSvih(): Int

    @Query("SELECT COUNT(*) FROM pitanja WHERE aktivno = 1 AND razred = :razred")
    suspend fun brojZaRazred(razred: Int): Int

    @Query("SELECT COUNT(*) FROM pitanja WHERE aktivno = 1 AND razred = :razred AND tip = :tip")
    suspend fun brojZaRazredITip(razred: Int, tip: String): Int

    @Query("SELECT COUNT(*) FROM pitanja WHERE aktivno = 1 AND razred = :razred")
    fun posmatrajBrojZaRazred(razred: Int): Flow<Int>

    @Query("SELECT * FROM pitanja WHERE id = :id LIMIT 1")
    suspend fun poId(id: String): PitanjeEntity?

    @Query("SELECT * FROM pitanja WHERE id IN (:ids)")
    suspend fun poIds(ids: List<String>): List<PitanjeEntity>

    @Query("SELECT * FROM pitanja WHERE aktivno = 1 AND razred = :razred")
    suspend fun svaZaRazred(razred: Int): List<PitanjeEntity>

    @Query("SELECT * FROM pitanja WHERE aktivno = 1 AND razred = :razred AND tip = :tip")
    suspend fun zaRazredITip(razred: Int, tip: String): List<PitanjeEntity>

    @Query("SELECT * FROM pitanja WHERE aktivno = 1 AND lekcijaId = :lekcijaId")
    suspend fun zaLekciju(lekcijaId: String): List<PitanjeEntity>

    /** Pitanja razreda koja učenik još nije ni jednom odgovorio. */
    @Query(
        """
        SELECT p.* FROM pitanja p
        LEFT JOIN napredak_pitanja n ON n.pitanjeId = p.id
        WHERE p.aktivno = 1 AND p.razred = :razred AND n.pitanjeId IS NULL
        """
    )
    suspend fun nevidjenaZaRazred(razred: Int): List<PitanjeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSve(pitanja: List<PitanjeEntity>)
}
