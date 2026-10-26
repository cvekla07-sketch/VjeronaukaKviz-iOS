package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ba.osgrivice.kviz.data.local.entity.BedzEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BedzDao {

    @Query("SELECT * FROM bedzevi ORDER BY dodijeljenMs")
    suspend fun svi(): List<BedzEntity>

    @Query("SELECT * FROM bedzevi ORDER BY dodijeljenMs")
    fun posmatrajSve(): Flow<List<BedzEntity>>

    @Query("SELECT id FROM bedzevi")
    suspend fun ids(): List<String>

    /** IGNORE — bedž se nikad ne dodjeljuje dvaput. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun dodaj(bedzevi: List<BedzEntity>)

    @Query("SELECT * FROM bedzevi WHERE sinhronizovano = 0")
    suspend fun nesinhronizovani(): List<BedzEntity>

    @Query("UPDATE bedzevi SET sinhronizovano = 1 WHERE id IN (:ids)")
    suspend fun oznaciSinhronizovane(ids: List<String>)

    @Query("DELETE FROM bedzevi")
    suspend fun obrisiSve()
}
