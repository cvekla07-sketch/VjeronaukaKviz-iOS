package ba.osgrivice.kviz.ui.screens.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.data.repository.OsvojenBedz
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.kviz.Streak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ProfilUiState(
    val ucitano: Boolean = false,
    val ime: String = "",
    val razred: Int = 8,
    val dnevniCilj: Int = 10,
    val skola: String = "",
    val takmicenje: LocalDate? = null,
    val xp: Int = 0,
    val streak: Int = 0,
    val odgovora: Int = 0,
    val tacnih: Int = 0,
    val savladano: Int = 0,
    val ukupnoPitanja: Int = 0,
    val bedzevi: List<OsvojenBedz> = emptyList()
) {
    val nivo: Int get() = xp / 500 + 1
    val xpUNivou: Int get() = xp % 500
    val procenatTacnih: Int get() = if (odgovora == 0) 0 else tacnih * 100 / odgovora
}

class ProfilViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ProfilUiState())
    val state: StateFlow<ProfilUiState> = _state

    init { osvjezi() }

    fun osvjezi() {
        viewModelScope.launch {
            val p = container.profilStore.trenutni()
            val razred = if (p.razred in 8..9) p.razred else 8
            val n = container.napredakRepository
            _state.update {
                it.copy(
                    ucitano = true,
                    ime = p.ime,
                    razred = razred,
                    dnevniCilj = p.dnevniCilj,
                    skola = p.skola,
                    takmicenje = if (p.takmicenjeDan > 0) LocalDate.fromEpochDays(p.takmicenjeDan.toInt()) else null,
                    xp = n.ukupniXp(),
                    streak = Streak.izracunaj(n.aktivniDani(), RasporedPonavljanja.danasDan()),
                    odgovora = n.ukupnoOdgovora(),
                    tacnih = n.ukupnoTacnih(),
                    savladano = n.brojSavladanih(razred),
                    ukupnoPitanja = container.pitanjaRepository.brojZaRazred(razred),
                    bedzevi = container.bedzeviRepository.osvojeni()
                )
            }
        }
    }

    fun sacuvajImeIRazred(ime: String, razred: Int) {
        viewModelScope.launch { container.profilStore.sacuvajOsnovno(ime, razred); osvjezi() }
    }

    fun postaviSkolu(skola: String) {
        viewModelScope.launch { container.profilStore.postaviSkolu(skola); osvjezi() }
    }

    fun postaviCilj(cilj: Int) {
        viewModelScope.launch { container.profilStore.postaviCilj(cilj); osvjezi() }
    }

    fun postaviTakmicenje(datum: LocalDate) {
        viewModelScope.launch { container.profilStore.postaviTakmicenje(datum.toEpochDays().toLong()); osvjezi() }
    }

    fun resetujNapredak() {
        viewModelScope.launch {
            container.napredakRepository.resetuj()
            container.bedzeviRepository.obrisiSve()
            osvjezi()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory { initializer { ProfilViewModel(container) } }
    }
}
