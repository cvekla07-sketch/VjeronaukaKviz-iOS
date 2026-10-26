package ba.osgrivice.kviz.ui.screens.takmicenje

import ba.osgrivice.kviz.platform.sadaMs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.data.repository.RezultatSimulacije
import ba.osgrivice.kviz.domain.kviz.Bedz
import ba.osgrivice.kviz.domain.kviz.Bodovanje
import ba.osgrivice.kviz.domain.kviz.ProvjeraOdgovora
import ba.osgrivice.kviz.domain.kviz.SesijaSazetak
import ba.osgrivice.kviz.domain.kviz.Takmicenje
import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.VrstaSesije
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TakmicenjeFaza { UVOD, UCITAVANJE, PITANJE, REZULTAT, PRAZNO }

/** Jedno pitanje u pregledu nakon simulacije. */
data class PregledPitanja(
    val redni: Int,
    val pitanje: Pitanje,
    val lekcija: Lekcija?,
    /** null = isteklo vrijeme */
    val odgovor: String?,
    val tacno: Boolean
)

data class TakmicenjeUiState(
    val faza: TakmicenjeFaza = TakmicenjeFaza.UVOD,
    val razred: Int = 8,
    val najbolja: RezultatSimulacije? = null,
    val brojOdradjenih: Int = 0,

    val pitanja: List<Pitanje> = emptyList(),
    val index: Int = 0,
    /** Po pitanju: null = još nije odgovoreno; "" = isteklo. */
    val odgovori: List<String?> = emptyList(),
    val rezultati: List<Boolean?> = emptyList(),
    val preostaloMs: Long = Takmicenje.MS_PO_PITANJU,
    /** Nakon odabira/isteka — kratka pauza prije sljedećeg; opcije zaključane. */
    val zakljucano: Boolean = false,
    val isteklo: Boolean = false,
    val uzastopnoTacnih: Int = 0,
    val xp: Int = 0,

    val trajanjeMs: Long = 0,
    val novaNajbolja: Boolean = false,
    val pregled: List<PregledPitanja> = emptyList(),
    val noviBedzevi: List<Bedz> = emptyList()
) {
    val trenutno: Pitanje? get() = pitanja.getOrNull(index)
    val ukupno: Int get() = pitanja.size
    val tacnih: Int get() = rezultati.count { it == true }
    val procenat: Int get() = Takmicenje.procenat(tacnih, ukupno)
    val udioVremena: Float get() = (preostaloMs.toFloat() / Takmicenje.MS_PO_PITANJU).coerceIn(0f, 1f)
}

class TakmicenjeViewModel(
    private val container: AppContainer,
    private val razred: Int
) : ViewModel() {

    private val _state = MutableStateFlow(TakmicenjeUiState(razred = razred))
    val state: StateFlow<TakmicenjeUiState> = _state

    private var sesijaId: Long = -1
    private var pocetakMs: Long = 0
    private var tajmer: Job? = null
    private var prelaz: Job? = null

    init { ucitajUvod() }

    private fun ucitajUvod() {
        viewModelScope.launch {
            container.inicijalizuj()
            val n = container.napredakRepository
            _state.update {
                it.copy(najbolja = n.najboljaSimulacija(razred), brojOdradjenih = n.brojSimulacija())
            }
        }
    }

    /** Dugme "Počni" na uvodnom ekranu. */
    fun pocni() {
        if (_state.value.faza != TakmicenjeFaza.UVOD) return
        _state.update { it.copy(faza = TakmicenjeFaza.UCITAVANJE) }
        viewModelScope.launch {
            val sva = container.pitanjaRepository.svaZaRazred(razred)
            val izabrana = Takmicenje.izaberi(sva)
            if (izabrana.size < 5) {
                _state.update { it.copy(faza = TakmicenjeFaza.PRAZNO) }
                return@launch
            }
            sesijaId = container.napredakRepository.zapocniSesiju(VrstaSesije.SIMULACIJA, razred, null, izabrana.size)
            pocetakMs = sadaMs()
            _state.update {
                it.copy(
                    faza = TakmicenjeFaza.PITANJE,
                    pitanja = izabrana,
                    index = 0,
                    odgovori = List(izabrana.size) { null },
                    rezultati = List(izabrana.size) { null },
                    preostaloMs = Takmicenje.MS_PO_PITANJU,
                    zakljucano = false,
                    isteklo = false,
                    uzastopnoTacnih = 0,
                    xp = 0
                )
            }
            pokreniTajmer()
        }
    }

    private fun pokreniTajmer() {
        tajmer?.cancel()
        tajmer = viewModelScope.launch {
            val kraj = sadaMs() + Takmicenje.MS_PO_PITANJU
            while (isActive) {
                val preostalo = kraj - sadaMs()
                if (preostalo <= 0) {
                    _state.update { it.copy(preostaloMs = 0) }
                    istekVremena()
                    break
                }
                _state.update { it.copy(preostaloMs = preostalo) }
                delay(100)
            }
        }
    }

    fun odgovori(odgovor: String) {
        val s = _state.value
        val p = s.trenutno ?: return
        if (s.faza != TakmicenjeFaza.PITANJE || s.zakljucano) return
        tajmer?.cancel()

        val tacno = ProvjeraOdgovora.jeTacan(p, odgovor)
        val niz = if (tacno) s.uzastopnoTacnih + 1 else 0
        _state.update {
            it.copy(
                odgovori = it.odgovori.toMutableList().also { o -> o[it.index] = odgovor },
                rezultati = it.rezultati.toMutableList().also { r -> r[it.index] = tacno },
                zakljucano = true,
                isteklo = false,
                uzastopnoTacnih = niz,
                xp = it.xp + Bodovanje.xpZaOdgovor(tacno, niz)
            )
        }
        viewModelScope.launch {
            runCatching { container.napredakRepository.zabiljeziOdgovor(sesijaId, p.id, tacno, odgovor) }
        }
        zakaziSljedece(Takmicenje.PAUZA_NAKON_ODGOVORA_MS)
    }

    private fun istekVremena() {
        val s = _state.value
        val p = s.trenutno ?: return
        if (s.zakljucano) return
        _state.update {
            it.copy(
                odgovori = it.odgovori.toMutableList().also { o -> o[it.index] = "" },
                rezultati = it.rezultati.toMutableList().also { r -> r[it.index] = false },
                zakljucano = true,
                isteklo = true,
                uzastopnoTacnih = 0
            )
        }
        viewModelScope.launch {
            runCatching {
                container.napredakRepository.zabiljeziOdgovor(
                    sesijaId, p.id, tacno = false, odgovor = Takmicenje.ODGOVOR_ISTEK, azurirajRaspored = false
                )
            }
        }
        zakaziSljedece(Takmicenje.PAUZA_NAKON_ISTEKA_MS)
    }

    private fun zakaziSljedece(pauzaMs: Long) {
        prelaz?.cancel()
        prelaz = viewModelScope.launch {
            delay(pauzaMs)
            sljedece()
        }
    }

    private fun sljedece() {
        val s = _state.value
        if (s.index + 1 >= s.ukupno) {
            zavrsi()
        } else {
            _state.update {
                it.copy(index = it.index + 1, preostaloMs = Takmicenje.MS_PO_PITANJU, zakljucano = false, isteklo = false)
            }
            pokreniTajmer()
        }
    }

    private fun zavrsi() {
        tajmer?.cancel()
        val s = _state.value
        val trajanje = sadaMs() - pocetakMs
        val ukupanXp = s.xp + Bodovanje.bonusKrajSesije(s.tacnih, s.ukupno)
        viewModelScope.launch {
            val n = container.napredakRepository
            val stara = n.najboljaSimulacija(razred)
            runCatching { n.zavrsiSesiju(sesijaId, s.tacnih, ukupanXp) }
            val noviBedzevi = runCatching {
                container.bedzeviRepository.provjeriIDodijeli(razred, SesijaSazetak(VrstaSesije.SIMULACIJA, s.tacnih, s.ukupno))
            }.getOrDefault(emptyList())
            launch { container.firebaseSync.sinhronizuj() }

            val lekcije = container.pitanjaRepository.lekcije(razred).associateBy { it.id }
            val pregled = s.pitanja.mapIndexed { i, p ->
                val o = s.odgovori[i]
                PregledPitanja(
                    redni = i + 1,
                    pitanje = p,
                    lekcija = p.lekcijaId?.let { lekcije[it] },
                    odgovor = if (o.isNullOrEmpty()) null else o,
                    tacno = s.rezultati[i] == true
                )
            }
            val novaNajbolja = Takmicenje.jeBolji(s.tacnih, trajanje, stara?.tacnih, stara?.trajanjeMs)

            _state.update {
                it.copy(
                    faza = TakmicenjeFaza.REZULTAT,
                    xp = ukupanXp,
                    trajanjeMs = trajanje,
                    novaNajbolja = novaNajbolja,
                    najbolja = n.najboljaSimulacija(razred),
                    brojOdradjenih = n.brojSimulacija(),
                    pregled = pregled,
                    noviBedzevi = noviBedzevi
                )
            }
        }
    }

    fun bedzPrikazan() {
        _state.update { it.copy(noviBedzevi = it.noviBedzevi.drop(1)) }
    }

    /** "Ponovo" s ekrana rezultata — nova sesija, isti razred. */
    fun ponovo() {
        if (_state.value.faza != TakmicenjeFaza.REZULTAT) return
        _state.update { TakmicenjeUiState(razred = razred, najbolja = it.najbolja, brojOdradjenih = it.brojOdradjenih) }
        pocni()
    }

    /** Izlazak usred simulacije — sesija ostaje nezavršena (ne računa se). */
    fun prekini() {
        tajmer?.cancel()
        prelaz?.cancel()
    }

    override fun onCleared() {
        tajmer?.cancel()
        prelaz?.cancel()
    }

    companion object {
        fun factory(container: AppContainer, razred: Int): ViewModelProvider.Factory =
            viewModelFactory { initializer { TakmicenjeViewModel(container, razred) } }
    }
}
