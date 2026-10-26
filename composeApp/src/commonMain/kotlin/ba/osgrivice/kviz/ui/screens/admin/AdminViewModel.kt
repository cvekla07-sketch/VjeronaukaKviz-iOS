package ba.osgrivice.kviz.ui.screens.admin

import ba.osgrivice.kviz.platform.sadaMs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.data.remote.NemaPristupaException
import ba.osgrivice.kviz.data.remote.UcenikPregled
import ba.osgrivice.kviz.domain.kviz.Takmicenje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminFaza { PROVJERA, PIN, UCITAVA, LISTA, NEMA_PRISTUPA, GRESKA }

enum class Sortiranje(val naziv: String) {
    XP("XP"),
    IME("Ime"),
    AKTIVNOST("Aktivnost"),
    SIMULACIJA("Simulacija"),
    SAVLADANO("Savladano")
}

data class SazetakRazreda(
    val ukupno: Int = 0,
    val aktivnih7: Int = 0,
    val aktivnihDanas: Int = 0,
    val prosjekSavladano: Int = 0,
    val saSimulacijom: Int = 0,
    val prosjekSimulacije: Int = 0,
    val prosjekXp: Int = 0
)

data class AdminUiState(
    val faza: AdminFaza = AdminFaza.PROVJERA,
    val pinUnos: String = "",
    val pinGreska: Boolean = false,
    val mojUid: String = "",
    val razred: Int = 8,
    val skola: String = "",
    val skole: List<String> = emptyList(),
    val sortiranje: Sortiranje = Sortiranje.XP,
    val pretraga: String = "",
    val svi: List<UcenikPregled> = emptyList(),
    val greska: String = ""
) {
    val filtrirani: List<UcenikPregled>
        get() {
            val p = pretraga.trim().lowercase()
            val lista = svi.filter { (skola.isBlank() || it.skola == skola) && (p.isEmpty() || it.ime.lowercase().contains(p)) }
            return when (sortiranje) {
                Sortiranje.XP -> lista.sortedByDescending { it.xp }
                Sortiranje.IME -> lista.sortedBy { it.ime.lowercase() }
                Sortiranje.AKTIVNOST -> lista.sortedByDescending { it.zadnjaAktivnostMs ?: 0L }
                Sortiranje.SIMULACIJA -> lista.sortedWith(compareByDescending<UcenikPregled> { it.simTacnih ?: -1 }.thenBy { it.simTrajanjeMs ?: Long.MAX_VALUE })
                Sortiranje.SAVLADANO -> lista.sortedByDescending { it.savladano }
            }
        }

    val sazetak: SazetakRazreda
        get() {
            val l = filtrirani
            if (l.isEmpty()) return SazetakRazreda()
            val sada = sadaMs()
            val dan = 24L * 3600 * 1000
            val saSim = l.filter { it.simTacnih != null }
            return SazetakRazreda(
                ukupno = l.size,
                aktivnih7 = l.count { (it.zadnjaAktivnostMs ?: 0L) >= sada - 7 * dan },
                aktivnihDanas = l.count { (it.zadnjaAktivnostMs ?: 0L) >= sada - dan },
                prosjekSavladano = l.sumOf { it.savladano } / l.size,
                saSimulacijom = saSim.size,
                prosjekSimulacije = if (saSim.isEmpty()) 0 else saSim.sumOf { Takmicenje.procenat(it.simTacnih ?: 0, it.simUkupno ?: Takmicenje.BROJ_PITANJA) } / saSim.size,
                prosjekXp = l.sumOf { it.xp } / l.size
            )
        }
}

class AdminViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state

    init {
        viewModelScope.launch {
            val uid = container.firebaseSync.prijavi().orEmpty()
            val profil = container.profilStore.trenutni()
            val razred = if (profil.razred in 8..9) profil.razred else 8
            val otkljucan = container.profilStore.jeAdminOtkljucan()
            _state.update { it.copy(mojUid = uid, razred = razred, faza = if (otkljucan) AdminFaza.UCITAVA else AdminFaza.PIN) }
            if (otkljucan) ucitaj()
        }
    }

    fun promijeniPin(p: String) {
        if (p.length <= 8 && p.all { it.isDigit() }) _state.update { it.copy(pinUnos = p, pinGreska = false) }
    }

    fun potvrdiPin() {
        val unos = _state.value.pinUnos
        if (unos.isBlank()) return
        viewModelScope.launch {
            val pin = runCatching { container.adminFirestore.pin() }.getOrNull()
            if (pin == null) {
                _state.update { it.copy(faza = AdminFaza.GRESKA, greska = "Ne mogu pročitati config/admin.\nProvjeri internet i da dokument postoji u Firestoreu.") }
                return@launch
            }
            if (pin == unos) {
                container.profilStore.postaviAdmin(true)
                _state.update { it.copy(faza = AdminFaza.UCITAVA, pinUnos = "") }
                ucitaj()
            } else {
                _state.update { it.copy(pinGreska = true, pinUnos = "") }
            }
        }
    }

    fun zakljucaj() {
        viewModelScope.launch {
            container.profilStore.postaviAdmin(false)
            _state.update { AdminUiState(faza = AdminFaza.PIN, mojUid = it.mojUid, razred = it.razred) }
        }
    }

    fun ucitaj() {
        _state.update { it.copy(faza = AdminFaza.UCITAVA) }
        viewModelScope.launch {
            runCatching { container.adminFirestore.ucenici(_state.value.razred) }
                .onSuccess { lista ->
                    _state.update {
                        it.copy(
                            faza = AdminFaza.LISTA,
                            svi = lista,
                            skole = lista.map { u -> u.skola }.filter { s -> s.isNotBlank() }.distinct().sorted(),
                            skola = if (lista.any { u -> u.skola == it.skola }) it.skola else ""
                        )
                    }
                }
                .onFailure { e ->
                    if (e is NemaPristupaException) _state.update { it.copy(faza = AdminFaza.NEMA_PRISTUPA) }
                    else _state.update { it.copy(faza = AdminFaza.GRESKA, greska = e.message ?: "Greška") }
                }
        }
    }

    fun postaviRazred(r: Int) {
        if (r == _state.value.razred) return
        _state.update { it.copy(razred = r) }
        ucitaj()
    }

    fun postaviSkolu(s: String) = _state.update { it.copy(skola = s) }
    fun postaviSortiranje(s: Sortiranje) = _state.update { it.copy(sortiranje = s) }
    fun postaviPretragu(p: String) = _state.update { it.copy(pretraga = p) }

    /** CSV s tačka-zarezom (Excel na našem jeziku ga otvara direktno) i BOM-om zbog kvačica. */
    fun csv(): String {
        val s = _state.value
        val sb = StringBuilder("﻿")
        sb.append("Ime;Razred;Škola;XP;Dana u nizu;Savladano;Odgovora;Tačnih;Tačnost %;Simulacija (tačnih);Simulacija (vrijeme);Broj simulacija;Bedževi;Zadnja aktivnost;Verzija\n")
        s.filtrirani.forEach { u ->
            sb.append(
                listOf(
                    u.ime, u.razred, u.skola, u.xp, u.streak, u.savladano, u.odgovora, u.tacnih, u.procenatTacnih,
                    u.simTacnih?.let { "$it/${u.simUkupno ?: Takmicenje.BROJ_PITANJA}" } ?: "",
                    u.simTrajanjeMs?.let { Takmicenje.formatTrajanje(it) } ?: "",
                    u.brojSimulacija, u.brojBedzeva,
                    u.zadnjaAktivnostMs?.let { formatDatumVrijeme(it) } ?: "",
                    u.verzijaApp
                ).joinToString(";") { it.toString().replace(";", ",") }
            ).append('\n')
        }
        return sb.toString()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory { initializer { AdminViewModel(container) } }
    }
}

fun formatDatumVrijeme(ms: Long): String = ba.osgrivice.kviz.platform.formatDatumVrijeme(ms)   // "d. M. yyyy. HH:mm"

fun formatDatum(ms: Long): String = ba.osgrivice.kviz.platform.formatDatum(ms)   // "d. M. yyyy."

/** "danas", "juče", "prije 3 dana", "prije 2 sedm." … */
fun relativno(ms: Long?): String {
    if (ms == null || ms == 0L) return "nikad"
    val dana = ((sadaMs() - ms) / (24L * 3600 * 1000)).toInt()
    return when {
        dana <= 0 -> "danas"
        dana == 1 -> "juče"
        dana < 7 -> "prije $dana dana"
        dana < 30 -> "prije ${dana / 7} sedm."
        else -> formatDatum(ms)
    }
}
