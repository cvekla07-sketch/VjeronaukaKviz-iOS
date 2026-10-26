package ba.osgrivice.kviz.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Osvojeni bedž (korak 6b, baza v2). ID = Bedzevi.* konstanta. */
@Entity(tableName = "bedzevi")
data class BedzEntity(
    @PrimaryKey val id: String,
    val dodijeljenMs: Long,
    @ColumnInfo(defaultValue = "0") val sinhronizovano: Boolean = false
)
