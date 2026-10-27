package ba.osgrivice.kviz.ui.screens.kviz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ba.osgrivice.kviz.AppContainer
import ba.osgrivice.kviz.domain.kviz.Bedz
import ba.osgrivice.kviz.domain.kviz.BiracPitanja
import ba.osgrivice.kviz.domain.kviz.Bodovanje
import ba.osgrivice.kviz.domain.kviz.Dopuna
import ba.osgrivice.kviz.domain.kviz.FazaDopune
import ba.osgrivice.kviz.domain.kviz.OcjenaDopune
import ba.osgrivice.kviz.domain.kviz.StrukturaDopune
import ba.osgrivice.kviz.domain.kviz.Lekcije
import ba.osgrivice.kviz.domain.kviz.ProvjeraOdgovora
import ba.osgrivice.kviz.domain.kviz.RasporedPonavljanja
import ba.osgrivice.kviz.domain.kviz.SesijaSazetak
import ba.osgrivice.kviz.domain.kviz.Streak
import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import ba.osgrivice.kviz.domain.model.VrstaSesije
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KvizFaza { UCITAVANJE, PITANJE, PRAZNO, REZULTAT }

data class Rezultat(
    val zvjezdice: Int = 0,
    val zvjezdicePrije: Int = 0,
    val streak: Int = 0,
    val novihNaucenih: Int = 0,
    val zaSutra: Int = 0,
    val sljedecaLekcija: Lekcija? = null
)

/**
 * Stanje "Dopuni" pitanja (vidi Dopuna.kt): faza učenja (kartice) ili pisanja (polja za kucanje).
 */
data class DopunaUi(
    val struktura: StrukturaDopune,
    val faza: FazaDopune,
    /** Ponuđene kartice (samo faza UCENJE). */
    val kartice: List<String> = emptyList(),
    /** UCENJE: indeks kartice u svakoj praznini (null = prazno). */
    val odabrane: List<Int?> = List(struktura.brojPraznina) { null },
    /** PISANJE: otkucan tekst u svakoj praznini. */
    val unosi: List<String> = List(struktura.brojPraznina) { "" },
    /** Popunjeno nakon provjere. */
    val ocjena: OcjenaDopune? = null
) {
    val vrijednosti: List<String?>
        get() = if (faza == FazaDopune.UCENJE) odabrane.map { it?.let { k -> kartice.getOrNull(k) } } else unosi

    val iskoristeneKartice: Set<Int> get() = odabrane.filterNotNull().toSet()

    /** UCENJE: sve praznine popunjene; PISANJE: bar jedno polje. */
    val spremno: Boolean
        get() = if (faza == FazaDopune.UCENJE) odabrane.all { it != null } else unosi.any { it.isNotBlank() }
}

data class KvizUiState(
    val faza: KvizFaza = KvizFaza.UCITAVANJE,
    val vrsta: VrstaSesije = VrstaSesije.LEKCIJA,
    val lekcija: Lekcija? = null,
    val pitanja: List<Pitanje> = emptyList(),
    val index: Int = 0,
    /** Rezultat po pitanju: null = još nije odgovoreno. */
    val rezultati: List<Boolean?> = emptyList(),
    val odgovor: String? = null,
    val unos: String = "",
    val provjereno: Boolean = false,
    val tacno: Boolean = false,
    val uzastopnoTacnih: Int = 0,
    val xp: Int = 0,
    val rezultat: Rezultat = Rezultat(),
    /** Za "Dopuni" pitanja (null za ABCD i T/N). */
    val dopuna: DopunaUi? = null,
    /** Bedževi osvojeni ovom sesijom — prikazuju se jedan po jedan preko ekrana rezultata. */
    val noviBedzevi: List<Bedz> = emptyList()
) {
    val trenutno: Pitanje? get() = pitanja.getOrNull(index)
    val ukupno: Int get() = pitanja.size
    val tacnih: Int get() = rezultati.count { it == true }
}

class KvizViewModel(
    private val container: AppContainer,
    private val razred: Int,
    private val vrsta: VrstaSesije,
    private val lekcijaId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(KvizUiState(vrsta = vrsta))
    val state: StateFlow<KvizUiState> = _state

    private var sesijaId: Long = -1
    private var zvjezdicePrije = 0
    private var novaOdgovorenaTacno = 0
    private val napredakPrije = HashMap<String, Int>()
    /** Sva pitanja razreda — izvor pogrešnih kartica za "Dopuni". */
    private var svaPitanjaRazreda: List<Pitanje> = emptyList()

    init { ucitaj() }

    private fun ucitaj() {
        viewModelScope.launch {
            container.inicijalizuj()
            val pitanjaRepo = container.pitanjaRepository
            val napredakRepo = container.napredakRepository

            val lekcija = lekcijaId?.let { pitanjaRepo.lekcija(it) }
            val dospjela = pitanjaRepo.poIds(napredakRepo.dospjelaIds(razred))

            val plan = if (vrsta == VrstaSesije.LEKCIJA && lekcija != null) {
                val pitanjaLekcije = pitanjaRepo.zaLekciju(lekcija.id)
                val napredak = napredakRepo.napredak(pitanjaLekcije.map { it.id })
                zvjezdicePrije = Lekcije.zvjezdice(pitanjaLekcije.size, pitanjaLekcije.map { napredak[it.id]?.kutija ?: 0 })
                BiracPitanja.zaLekciju(pitanjaLekcije, dospjela, napredak)
            } else {
                BiracPitanja.zaPonavljanje(dospjela)
            }

            if (plan.pitanja.isEmpty()) {
                _state.update { it.copy(faza = KvizFaza.PRAZNO, lekcija = lekcija) }
                return@launch
            }

            napredakRepo.napredak(plan.pitanja.map { it.id }).forEach { (id, n) -> napredakPrije[id] = n.kutija }
            svaPitanjaRazreda = runCatching { pitanjaRepo.svaZaRazred(razred) }.getOrDefault(emptyList())
            sesijaId = napredakRepo.zapocniSesiju(vrsta, razred, lekcija?.id, plan.pitanja.size)
            _state.update {
                it.copy(
                    faza = KvizFaza.PITANJE,
                    lekcija = lekcija,
                    pitanja = plan.pitanja,
                    rezultati = List(plan.pitanja.size) { null },
                    dopuna = pripremiDopunu(plan.pitanja.first())
                )
            }
        }
    }

    fun promijeniUnos(tekst: String) {
        if (_state.value.provjereno) return
        _state.update { it.copy(unos = tekst) }
    }

    fun odgovori(odgovor: String) {
        val s = _state.value
        val p = s.trenutno ?: return
        if (s.provjereno || odgovor.isBlank()) return

        val tacno = ProvjeraOdgovora.jeTacan(p, odgovor)
        val niz = if (tacno) s.uzastopnoTacnih + 1 else 0
        zabiljezi(p, odgovor, tacno, niz, Bodovanje.xpZaOdgovor(tacno, niz), null)
    }

    // ---------------------------------------------------------------- "Dopuni" (kartice / pisanje)

    private fun pripremiDopunu(p: Pitanje): DopunaUi? {
        if (p.tip != TipPitanja.DOPUNI) return null
        val struktura = Dopuna.struktura(p)
        val faza = Dopuna.faza(struktura, napredakPrije[p.id] ?: 0)
        val kartice = if (faza == FazaDopune.UCENJE) Dopuna.kartice(p, struktura, svaPitanjaRazreda) else emptyList()
        return DopunaUi(struktura = struktura, faza = faza, kartice = kartice)
    }

    /** Dodir na karticu → u prvu praznu prazninu. */
    fun odaberiKarticu(indeks: Int) {
        val d = _state.value.dopuna ?: return
        if (_state.value.provjereno || indeks in d.iskoristeneKartice) return
        val prazna = d.odabrane.indexOfFirst { it == null }
        if (prazna < 0) return
        _state.update { it.copy(dopuna = d.copy(odabrane = d.odabrane.toMutableList().also { l -> l[prazna] = indeks })) }
    }

    /** Dodir na popunjenu prazninu → kartica se vraća. */
    fun isprazniPrazninu(praznina: Int) {
        val d = _state.value.dopuna ?: return
        if (_state.value.provjereno) return
        _state.update { it.copy(dopuna = d.copy(odabrane = d.odabrane.toMutableList().also { l -> l[praznina] = null })) }
    }

    fun promijeniUnosPraznine(praznina: Int, tekst: String) {
        val d = _state.value.dopuna ?: return
        if (_state.value.provjereno) return
        _state.update { it.copy(dopuna = d.copy(unosi = d.unosi.toMutableList().also { l -> l[praznina] = tekst })) }
    }

    fun provjeriDopunu() {
        val s = _state.value
        val p = s.trenutno ?: return
        val d = s.dopuna ?: return
        if (s.provjereno || !d.spremno) return

        val ocjena = Dopuna.ocijeni(d.struktura, d.vrijednosti)
        val tacno = ocjena.sveTacno
        val niz = if (tacno) s.uzastopnoTacnih + 1 else 0
        val tekst = d.vrijednosti.joinToString("; ") { it?.trim().orEmpty() }
        zabiljezi(p, tekst, tacno, niz, Dopuna.xp(ocjena, niz), d.copy(ocjena = ocjena))
    }

    private fun zabiljezi(p: Pitanje, odgovor: String, tacno: Boolean, niz: Int, xp: Int, dopuna: DopunaUi?) {
        if (tacno && (napredakPrije[p.id] ?: 0) == 0) novaOdgovorenaTacno++
        _state.update {
            it.copy(
                odgovor = odgovor,
                provjereno = true,
                tacno = tacno,
                rezultati = it.rezultati.toMutableList().also { r -> r[it.index] = tacno },
                uzastopnoTacnih = niz,
                xp = it.xp + xp,
                dopuna = dopuna ?: it.dopuna
            )
        }
        viewModelScope.launch {
            runCatching { container.napredakRepository.zabiljeziOdgovor(sesijaId, p.id, tacno, odgovor) }
        }
    }

    fun nastavi() {
        val s = _state.value
        if (!s.provjereno) return
        if (s.index + 1 >= s.ukupno) zavrsi()
        else _state.update {
            it.copy(
                index = it.index + 1, odgovor = null, unos = "", provjereno = false, tacno = false,
                dopuna = it.pitanja.getOrNull(it.index + 1)?.let { p -> pripremiDopunu(p) }
            )
        }
    }

    private fun zavrsi() {
        val s = _state.value
        val ukupanXp = s.xp + Bodovanje.bonusKrajSesije(s.tacnih, s.ukupno)
        viewModelScope.launch {
            val napredakRepo = container.napredakRepository
            val pitanjaRepo = container.pitanjaRepository
            runCatching { napredakRepo.zavrsiSesiju(sesijaId, s.tacnih, ukupanXp) }
            val noviBedzevi = runCatching {
                container.bedzeviRepository.provjeriIDodijeli(razred, SesijaSazetak(vrsta, s.tacnih, s.ukupno))
            }.getOrDefault(emptyList())
            launch { container.firebaseSync.sinhronizuj() }

            var zv = 0
            var sljedeca: Lekcija? = null
            s.lekcija?.let { l ->
                val pit = pitanjaRepo.zaLekciju(l.id)
                val nap = napredakRepo.napredak(pit.map { it.id })
                zv = Lekcije.zvjezdice(pit.size, pit.map { nap[it.id]?.kutija ?: 0 })
                sljedeca = pitanjaRepo.lekcije(razred).firstOrNull { it.redniBroj == l.redniBroj + 1 }
            }
            val streak = Streak.izracunaj(napredakRepo.aktivniDani(), RasporedPonavljanja.danasDan())

            _state.update {
                it.copy(
                    faza = KvizFaza.REZULTAT,
                    xp = ukupanXp,
                    rezultat = Rezultat(
                        zvjezdice = zv,
                        zvjezdicePrije = zvjezdicePrije,
                        streak = streak,
                        novihNaucenih = novaOdgovorenaTacno,
                        zaSutra = s.tacnih,
                        sljedecaLekcija = if (zv >= 1) sljedeca else null
                    ),
                    noviBedzevi = noviBedzevi
                )
            }
        }
    }

    /** Zatvoren dijalog bedža — prikaži sljedeći ako ga ima. */
    fun bedzPrikazan() {
        _state.update { it.copy(noviBedzevi = it.noviBedzevi.drop(1)) }
    }

    companion object {
        fun factory(container: AppContainer, razred: Int, vrsta: VrstaSesije, lekcijaId: String?): ViewModelProvider.Factory =
            viewModelFactory { initializer { KvizViewModel(container, razred, vrsta, lekcijaId) } }
    }
}
