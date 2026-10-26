package ba.osgrivice.kviz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ba.osgrivice.kviz.data.local.entity.MetaEntity

@Dao
interface MetaDao {
    @Query("SELECT vrijednost FROM meta WHERE kljuc = :kljuc LIMIT 1")
    suspend fun get(kljuc: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: MetaEntity)
}
