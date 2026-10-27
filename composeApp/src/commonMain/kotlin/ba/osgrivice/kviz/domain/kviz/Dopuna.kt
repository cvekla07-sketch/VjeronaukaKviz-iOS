package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import kotlin.random.Random

/**
 * "Dopuni" pitanja u dvije faze (razmaknuto ponavljanje odlučuje o fazi):
 *
 *  UCENJE  — pitanje još nije savladano (kutija 0): dijete dodirom slaže ponuđene kartice
 *            (tačni dijelovi + pogrešni iz iste cjeline) u praznine.
 *  PISANJE — dijete je pitanje već jednom uradilo potpuno tačno (kutija ≥ 1): svaka praznina
 *            se kuca, kao na papiru. Savladano je tek kad se otkuca tačno.
 *
 * Bodovanje je djelimično, po praznini (npr. 2/5) — kao na testu.
 * Pitanje se za ponavljanje računa kao tačno samo kad su SVE praznine tačne.
 *
 * Format u bazi se ne mijenja:
 *  - dijelovi odgovora su odvojeni sa ";"
 *  - "POJAM — značenje" (crtica — ili –) daje polje s oznakom
 *  - broj "____" u tekstu = broj dijelova → praznine se prikazuju u samom tekstu
 */
enum class FazaDopune { UCENJE, PISANJE }

/** Jedna praznina: oznaka (npr. "Amin", "Sekta") ili null, i tačan odgovor. */
data class DioOdgovora(val oznaka: String?, val odgovor: String)

data class StrukturaDopune(
    val dijelovi: List<DioOdgovora>,
    /** Praznine su u tekstu pitanja (broj "____" == broj dijelova). */
    val uTekstu: Boolean,
    /** Redoslijed je bitan (ajet, hadis, sura, "redom", "poređaj", oznake, praznine u tekstu). */
    val redoslijed: Boolean,
    /** Uparivanje (sve stavke imaju oznaku) — i u fazi pisanja ostaje biranje kartica. */
    val uparivanje: Boolean,
    /** Bez pogrešnih kartica (poređaj, prijevod, uparivanje) — slažu se samo tačni dijelovi. */
    val bezPogresnih: Boolean
) {
    val brojPraznina: Int get() = dijelovi.size
}

/** Rezultat provjere: tačnost po praznini. */
data class OcjenaDopune(val poPraznini: List<Boolean>) {
    val tacnih: Int get() = poPraznini.count { it }
    val ukupno: Int get() = poPraznini.size
    val sveTacno: Boolean get() = ukupno > 0 && tacnih == ukupno
    val udio: Float get() = if (ukupno == 0) 0f else tacnih.toFloat() / ukupno
}

object Dopuna {

    private val PRAZNINA = Regex("_{3,}")
    private val CRTICA_OZNAKE = Regex("\\s[—–]\\s")
    private val RIJECI_REDOSLIJEDA = Regex(
        "redom|poređaj|poredaj|prijevod|ajet|hadis|sur[aeu]\\b|dov[aeu]\\b",
        RegexOption.IGNORE_CASE
    )
    private val RIJECI_BEZ_POGRESNIH = Regex("poređaj|poredaj|prijevod|upari", RegexOption.IGNORE_CASE)

    fun struktura(p: Pitanje): StrukturaDopune {
        val dijeloviTekst = p.tacno.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val saOznakom = dijeloviTekst.size >= 2 && dijeloviTekst.all { CRTICA_OZNAKE.containsMatchIn(it) }
        val dijelovi = dijeloviTekst.map { d ->
            if (saOznakom) {
                val m = CRTICA_OZNAKE.find(d)!!
                DioOdgovora(d.substring(0, m.range.first).trim(), d.substring(m.range.last + 1).trim())
            } else DioOdgovora(null, d)
        }
        val uTekstu = dijelovi.size >= 2 && PRAZNINA.findAll(p.tekst).count() == dijelovi.size
        val uparivanje = saOznakom
        return StrukturaDopune(
            dijelovi = dijelovi,
            uTekstu = uTekstu,
            redoslijed = saOznakom || uTekstu || RIJECI_REDOSLIJEDA.containsMatchIn(p.tekst),
            uparivanje = uparivanje,
            bezPogresnih = uparivanje || RIJECI_BEZ_POGRESNIH.containsMatchIn(p.tekst)
        )
    }

    /** Da li pitanje u fazi učenja ima kartice (višedijelno ili dugačak odgovor od 3+ riječi). */
    fun imaKartice(s: StrukturaDopune): Boolean =
        s.brojPraznina >= 2 || s.dijelovi.first().odgovor.split(Regex("\\s+")).size >= 3

    /** Faza po stanju ponavljanja: kutija ≥ 1 = dijete je već jednom tačno uradilo → piše. */
    fun faza(s: StrukturaDopune, kutija: Int): FazaDopune = when {
        !imaKartice(s) -> FazaDopune.PISANJE                 // kratak odgovor — kuca se kao i do sada
        s.uparivanje -> FazaDopune.UCENJE                    // uparivanje je i na papiru biranje
        kutija >= 1 -> FazaDopune.PISANJE
        else -> FazaDopune.UCENJE
    }

    /**
     * Kartice za fazu učenja: tačni dijelovi + do [brojPogresnih] pogrešnih iz [ostala]
     * (prvo ista cjelina, pa razred), izmiješano.
     */
    fun kartice(
        p: Pitanje,
        s: StrukturaDopune,
        ostala: List<Pitanje>,
        random: Random = Random.Default,
        brojPogresnih: Int = if (s.brojPraznina == 1) 3 else 2
    ): List<String> {
        val tacne = s.dijelovi.map { it.odgovor }
        if (s.bezPogresnih) return tacne.shuffled(random)
        val tacneNorm = tacne.map { ProvjeraOdgovora.normalizuj(it) }.toSet()
        fun kandidati(lista: List<Pitanje>) = lista.asSequence()
            .filter { it.id != p.id && it.tip == TipPitanja.DOPUNI }
            .flatMap { o -> struktura(o).dijelovi.map { it.odgovor } }
            .filter { ProvjeraOdgovora.normalizuj(it) !in tacneNorm }
            .distinctBy { ProvjeraOdgovora.normalizuj(it) }
            .toList()
        val istaCjelina = kandidati(ostala.filter { it.lekcijaId == p.lekcijaId })
        val razred = kandidati(ostala.filter { it.lekcijaId != p.lekcijaId })
        // sličnije dužine su uvjerljivije
        val prosjek = tacne.map { it.length }.average()
        fun izbor(l: List<String>) = l.shuffled(random).sortedBy { kotlin.math.abs(it.length - prosjek) }
        val pogresne = (izbor(istaCjelina) + izbor(razred)).distinct().take(brojPogresnih)
        return (tacne + pogresne).shuffled(random)
    }

    /**
     * Ocjena po praznini. [unosi] su po praznini (kartica ili otkucan tekst; null/prazno = nije popunjeno).
     * Kad redoslijed nije bitan, svaki tačan dio se može iskoristiti samo jednom.
     */
    fun ocijeni(s: StrukturaDopune, unosi: List<String?>): OcjenaDopune {
        val tacni = s.dijelovi.map { it.odgovor }
        if (s.redoslijed) {
            return OcjenaDopune(tacni.indices.map { i -> unosi.getOrNull(i)?.let { slicno(it, tacni[i]) } ?: false })
        }
        val iskoristeni = BooleanArray(tacni.size)
        val rez = tacni.indices.map { i ->
            val u = unosi.getOrNull(i)
            if (u.isNullOrBlank()) false else {
                val j = tacni.indices.firstOrNull { !iskoristeni[it] && slicno(u, tacni[it]) }
                if (j != null) { iskoristeni[j] = true; true } else false
            }
        }
        return OcjenaDopune(rez)
    }

    /**
     * Da li je otkucano "dovoljno isto" kao tačno — kao kod nastavnika na papiru:
     *  - velika/mala slova, kvačice, interpunkcija se ne gledaju (ProvjeraOdgovora.normalizuj),
     *  - isti broj riječi; po riječi dozvoljena sitna greška: do 4 slova 0, 5–9 slova 1, 10+ slova 2,
     *  - alternative "A / B" i sadržaj u zagradi "(…)" su prihvatljivi i odvojeno.
     */
    fun slicno(unos: String, tacno: String): Boolean {
        val u = ProvjeraOdgovora.normalizuj(unos)
        if (u.isEmpty()) return false
        return varijante(tacno).any { v -> rijeciSlicne(u, v) }
    }

    private fun varijante(tacno: String): List<String> {
        val out = linkedSetOf<String>()
        out += ProvjeraOdgovora.normalizuj(tacno)
        val bezZagrada = tacno.replace(Regex("\\([^)]*\\)"), " ")
        out += ProvjeraOdgovora.normalizuj(bezZagrada)
        Regex("\\(([^)]*)\\)").findAll(tacno).forEach { out += ProvjeraOdgovora.normalizuj(it.groupValues[1]) }
        if (tacno.contains("/")) {
            tacno.split("/").forEach { out += ProvjeraOdgovora.normalizuj(it); out += ProvjeraOdgovora.normalizuj(it.replace(Regex("\\([^)]*\\)"), " ")) }
        }
        return out.filter { it.isNotEmpty() }
    }

    private fun rijeciSlicne(a: String, b: String): Boolean {
        if (a == b) return true
        val ra = a.split(' ')
        val rb = b.split(' ')
        if (ra.size != rb.size) return false
        return ra.indices.all { i -> levenshtein(ra[i], rb[i]) <= dozvoljeno(rb[i].length) }
    }

    private fun dozvoljeno(duzina: Int): Int = when {
        duzina <= 4 -> 0
        duzina <= 9 -> 1
        else -> 2
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val c = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + c)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Tekst pitanja razbijen na dijelove oko praznina: [tekst0, tekst1, …] — praznina je između susjednih. */
    fun tekstOkoPraznina(tekst: String): List<String> = tekst.split(PRAZNINA)

    /** XP za djelimičan odgovor: srazmjerno tačnim prazninama (pun XP samo za sve tačno). */
    fun xp(ocjena: OcjenaDopune, uzastopnoTacnih: Int): Int =
        if (ocjena.sveTacno) Bodovanje.xpZaOdgovor(true, uzastopnoTacnih)
        else kotlin.math.round(Bodovanje.XP_TACAN * ocjena.udio).toInt()
}
