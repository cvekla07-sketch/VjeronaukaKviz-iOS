package ba.osgrivice.kviz.ui.screens.staza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.data.repository.RezultatSimulacije
import ba.osgrivice.kviz.domain.kviz.BiracPitanja
import ba.osgrivice.kviz.domain.kviz.Lekcije
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.kviz.StanjeLekcije
import ba.osgrivice.kviz.domain.kviz.StatusLekcije
import ba.osgrivice.kviz.domain.kviz.Streak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class StazaUiState(
    val ucitano: Boolean = false,
    val ime: String = "",
    val razred: Int = 8,
    val streak: Int = 0,
    val danasOdgovora: Int = 0,
    val dnevniCilj: Int = 10,
    val savladano: Int = 0,
    val ukupnoPitanja: Int = 0,
    val daniDoTakmicenja: Long = 0,
    val brojDospjelih: Int = 0,
    val lekcije: List<StanjeLekcije> = emptyList(),
    val aktivna: StanjeLekcije? = null,
    val planNovih: Int = 0,
    val planPonavljanja: Int = 0,
    val najboljaSimulacija: RezultatSimulacije? = null,
    val brojSimulacija: Int = 0
) {
    val sveZavrseno: Boolean get() = ucitano && aktivna == null && lekcije.isNotEmpty()
}

class StazaViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(StazaUiState())
    val state: StateFlow<StazaUiState> = _state

    fun osvjezi() {
        viewModelScope.launch { container.firebaseSync.sinhronizuj() }
        viewModelScope.launch { ucitaj() }
        // ispravke pitanja iz oblaka — ako je nešto stiglo, osvježi stazu
        viewModelScope.launch {
            container.inicijalizuj()
            if (container.cloudSadrzaj.sinhronizuj()) ucitaj()
        }
    }

    private suspend fun ucitaj() {
        run {
            container.inicijalizuj()
            val profil = container.profilStore.trenutni()
            val razred = if (profil.razred in 8..9) profil.razred else 8
            val pitanjaRepo = container.pitanjaRepository
            val napredakRepo = container.napredakRepository

            val lekcije = pitanjaRepo.lekcije(razred)
            val sva = pitanjaRepo.svaZaRazred(razred)
            val napredak = napredakRepo.napredak(sva.map { it.id })
            val poLekciji = sva.filter { it.lekcijaId != null }.groupBy({ it.lekcijaId!! }, { it.id })

            val staza = Lekcije.izracunajStazu(lekcije, poLekciji, napredak)
            val aktivna = staza.firstOrNull { it.status == StatusLekcije.AKTIVNA }

            val dospjelaIds = napredakRepo.dospjelaIds(razred)
            val dospjela = pitanjaRepo.poIds(dospjelaIds)

            val plan = aktivna?.let { a ->
                BiracPitanja.zaLekciju(sva.filter { it.lekcijaId == a.lekcija.id }, dospjela, napredak)
            }

            val danas = RasporedPonavljanja.danasDan()
            val takmicenje = if (profil.takmicenjeDan > 0) profil.takmicenjeDan else podrazumijevanoTakmicenje(danas)

            _state.update {
                it.copy(
                    ucitano = true,
                    ime = profil.ime,
                    razred = razred,
                    streak = Streak.izracunaj(napredakRepo.aktivniDani(), danas),
                    danasOdgovora = napredakRepo.brojOdgovoraDanas(),
                    dnevniCilj = profil.dnevniCilj,
                    savladano = napredak.values.count { it.kutija >= 1 },   // zadnji put tačno — isto kao u pregledu nivoa
                    ukupnoPitanja = sva.size,
                    daniDoTakmicenja = (takmicenje - danas).coerceAtLeast(0),
                    brojDospjelih = dospjela.size,
                    lekcije = staza,
                    aktivna = aktivna,
                    planNovih = plan?.novih ?: 0,
                    planPonavljanja = plan?.ponavljanja ?: 0,
                    najboljaSimulacija = napredakRepo.najboljaSimulacija(razred),
                    brojSimulacija = napredakRepo.brojSimulacija()
                )
            }
        }
    }

    /** Takmičenje je u martu: 1. mart tekuće ili sljedeće školske godine. */
    private fun podrazumijevanoTakmicenje(danas: Long): Long {
        val d = LocalDate.fromEpochDays(danas.toInt())
        var t = LocalDate(d.year, 3, 1)
        if (t <= d) t = t.plus(1, DateTimeUnit.YEAR)
        return t.toEpochDays().toLong()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory { initializer { StazaViewModel(container) } }
    }
}
