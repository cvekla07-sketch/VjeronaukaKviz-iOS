package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ba.osgrivice.kviz.data.local.entity.NapredakPitanjaEntity

@Dao
interface NapredakDao {

    @Query("SELECT * FROM napredak_pitanja WHERE pitanjeId = :pitanjeId LIMIT 1")
    suspend fun poPitanju(pitanjeId: String): NapredakPitanjaEntity?

    @Query("SELECT * FROM napredak_pitanja WHERE pitanjeId IN (:ids)")
    suspend fun poPitanjima(ids: List<String>): List<NapredakPitanjaEntity>

    /** Pitanja razreda koja dospijevaju za ponavljanje do (uključivo) datog dana. */
    @Query(
        """
        SELECT n.* FROM napredak_pitanja n
        INNER JOIN pitanja p ON p.id = n.pitanjeId
        WHERE p.aktivno = 1 AND p.razred = :razred AND n.sljedeceDan <= :dan
        ORDER BY n.sljedeceDan ASC, n.kutija ASC
        """
    )
    suspend fun dospjelaZaRazred(razred: Int, dan: Long): List<NapredakPitanjaEntity>

    @Query(
        """
        SELECT COUNT(*) FROM napredak_pitanja n
        INNER JOIN pitanja p ON p.id = n.pitanjeId
        WHERE p.aktivno = 1 AND p.razred = :razred AND n.sljedeceDan <= :dan
        """
    )
    suspend fun brojDospjelih(razred: Int, dan: Long): Int

    /** Pitanja razreda koja je učenik zadnji put pogriješio (netacnih > 0 i kutija = 0). */
    @Query(
        """
        SELECT n.* FROM napredak_pitanja n
        INNER JOIN pitanja p ON p.id = n.pitanjeId
        WHERE p.aktivno = 1 AND p.razred = :razred AND n.kutija = 0 AND n.netacnih > 0
        ORDER BY n.zadnjiOdgovorMs DESC
        """
    )
    suspend fun greskeZaRazred(razred: Int): List<NapredakPitanjaEntity>

    @Query(
        """
        SELECT COUNT(*) FROM napredak_pitanja n
        INNER JOIN pitanja p ON p.id = n.pitanjeId
        WHERE p.aktivno = 1 AND p.razred = :razred AND (n.tacnih + n.netacnih) > 0
        """
    )
    suspend fun brojVidjenih(razred: Int): Int

    @Query(
        """
        SELECT COUNT(*) FROM napredak_pitanja n
        INNER JOIN pitanja p ON p.id = n.pitanjeId
        WHERE p.aktivno = 1 AND p.razred = :razred AND n.kutija >= :minKutija
        """
    )
    suspend fun brojSavladanih(razred: Int, minKutija: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(napredak: NapredakPitanjaEntity)

    @Query("DELETE FROM napredak_pitanja")
    suspend fun obrisiSve()
}
