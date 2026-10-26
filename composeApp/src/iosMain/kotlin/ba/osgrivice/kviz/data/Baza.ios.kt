package ba.osgrivice.kviz.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import ba.osgrivice.kviz.platform.folderAplikacije

actual fun bazaBuilder(ime: String): RoomDatabase.Builder<KvizDatabase> =
    Room.databaseBuilder<KvizDatabase>(name = "${folderAplikacije()}/$ime")
