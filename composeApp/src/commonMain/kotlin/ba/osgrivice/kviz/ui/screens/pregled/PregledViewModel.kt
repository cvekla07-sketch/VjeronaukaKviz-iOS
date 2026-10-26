package ba.osgrivice.kviz.ui.screens.pregled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Zadnji odgovor učenika: true = tačno, false = pogrešno, null = još nije odgovarao. */
data class StavkaPregleda(val pitanje: Pitanje, val zadnjiTacan: Boolean?) {
    /** Tačan odgovor spreman za prikaz: ABCD → "A · tekst", TN → Tačno/Netačno, dopuni → tekst. */
    val tacanOdgovor: String
        get() = when (pitanje.tip) {
            TipPitanja.ABCD -> {
                val slovo = pitanje.tacno.trim().uppercase()
                val idx = "ABCD".indexOf(slovo.firstOrNull() ?: ' ')
                val tekst = pitanje.opcije.getOrNull(idx)
                if (tekst != null) "$slovo · $tekst" else slovo
            }
            TipPitanja.TACNO_NETACNO -> if (pitanje.tacno.trim().uppercase().startsWith("T")) "Tačno" else "Netačno"
            TipPitanja.DOPUNI -> pitanje.tacno.trim()
        }
}

data class PregledUiState(
    val ucitano: Boolean = false,
    val lekcija: Lekcija? = null,
    val stavke: List<StavkaPregleda> = emptyList()
) {
    val ukupno: Int get() = stavke.size
    val tacnih: Int get() = stavke.count { it.zadnjiTacan == true }
}

/** Pregled svih pitanja jednog nivoa s tačnim odgovorima i objašnjenjima — za učenje prije kviza. */
class PregledViewModel(
    private val container: AppContainer,
    private val lekcijaId: String
) : ViewModel() {

    private val _state = MutableStateFlow(PregledUiState())
    val state: StateFlow<PregledUiState> = _state

    init {
        viewModelScope.launch { ucitaj() }
    }

    private suspend fun ucitaj() {
        container.inicijalizuj()
        val lekcija = container.pitanjaRepository.lekcija(lekcijaId)
        val pitanja = container.pitanjaRepository.zaLekciju(lekcijaId).filter { it.aktivno }
        val napredak = container.napredakRepository.napredak(pitanja.map { it.id })
        val stavke = pitanja.map { p ->
            val n = napredak[p.id]
            StavkaPregleda(p, zadnjiTacan = if (n == null || !n.vidjeno) null else n.kutija >= 1)
        }
        _state.value = PregledUiState(ucitano = true, lekcija = lekcija, stavke = stavke)
    }

    companion object {
        fun factory(container: AppContainer, lekcijaId: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { PregledViewModel(container, lekcijaId) } }
    }
}
