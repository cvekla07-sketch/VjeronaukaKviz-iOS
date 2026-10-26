package ba.osgrivice.kviz.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ba.osgrivice.kviz.platform.kreirajDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Isto ime kao na Androidu ("profil"); iOS: Documents/profil.preferences_pb */
private val profilDataStore: DataStore<Preferences> by lazy { kreirajDataStore("profil") }

data class Profil(
    val ime: String = "",
    val razred: Int = 0,
    val dnevniCilj: Int = 10,
    /** Naziv škole (slobodan unos; za rang-listu i nastavnički pregled). */
    val skola: String = "",
    /** Epoch dan takmičenja (0 = nije postavljen → koristi se podrazumijevani). */
    val takmicenjeDan: Long = 0
) {
    val podesen: Boolean get() = razred in 8..9
}

/** Lokalne postavke učenika (ime, razred, cilj). Firebase sync u koraku 5 čita odavde. */
class ProfilStore {

    private object K {
        val IME = stringPreferencesKey("ime")
        val RAZRED = intPreferencesKey("razred")
        val CILJ = intPreferencesKey("dnevni_cilj")
        val TAKMICENJE = longPreferencesKey("takmicenje_dan")
        val SKOLA = stringPreferencesKey("skola")
        val ADMIN = booleanPreferencesKey("admin_otkljucan")
    }

    val profil: Flow<Profil> = profilDataStore.data.map { p ->
        Profil(
            ime = p[K.IME] ?: "",
            razred = p[K.RAZRED] ?: 0,
            dnevniCilj = p[K.CILJ] ?: 10,
            skola = p[K.SKOLA] ?: "",
            takmicenjeDan = p[K.TAKMICENJE] ?: 0
        )
    }

    suspend fun trenutni(): Profil = profil.first()

    suspend fun sacuvajOsnovno(ime: String, razred: Int) {
        profilDataStore.edit {
            it[K.IME] = ime.trim()
            it[K.RAZRED] = razred
        }
    }

    suspend fun postaviSkolu(skola: String) {
        profilDataStore.edit { it[K.SKOLA] = skola.trim() }
    }

    suspend fun postaviCilj(cilj: Int) {
        profilDataStore.edit { it[K.CILJ] = cilj.coerceIn(5, 50) }
    }

    suspend fun postaviTakmicenje(epochDan: Long) {
        profilDataStore.edit { it[K.TAKMICENJE] = epochDan }
    }

    // ---- nastavnički pregled (korak 7) ----

    val adminOtkljucan: Flow<Boolean> = profilDataStore.data.map { it[K.ADMIN] ?: false }

    suspend fun jeAdminOtkljucan(): Boolean = adminOtkljucan.first()

    suspend fun postaviAdmin(otkljucan: Boolean) {
        profilDataStore.edit { it[K.ADMIN] = otkljucan }
    }
}
