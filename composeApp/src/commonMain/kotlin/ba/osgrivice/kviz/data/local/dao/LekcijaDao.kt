package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ba.osgrivice.kviz.data.local.entity.LekcijaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LekcijaDao {
    @Query("SELECT * FROM lekcije WHERE razred = :razred ORDER BY redniBroj")
    fun posmatrajZaRazred(razred: Int): Flow<List<LekcijaEntity>>

    @Query("SELECT * FROM lekcije WHERE razred = :razred ORDER BY redniBroj")
    suspend fun zaRazred(razred: Int): List<LekcijaEntity>

    @Query("SELECT * FROM lekcije WHERE id = :id LIMIT 1")
    suspend fun poId(id: String): LekcijaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSve(lekcije: List<LekcijaEntity>)
}
