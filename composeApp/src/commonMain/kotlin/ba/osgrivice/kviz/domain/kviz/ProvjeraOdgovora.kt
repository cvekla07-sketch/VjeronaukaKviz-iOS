package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import ba.osgrivice.kviz.platform.nfd

/** Čista logika provjere odgovora — bez Androida, lako testirati. */
object ProvjeraOdgovora {

    fun jeTacan(pitanje: Pitanje, odgovor: String): Boolean = when (pitanje.tip) {
        TipPitanja.ABCD, TipPitanja.TACNO_NETACNO ->
            odgovor.trim().equals(pitanje.tacno.trim(), ignoreCase = true)

        TipPitanja.DOPUNI -> {
            val uneseno = normalizuj(odgovor)
            if (uneseno.isEmpty()) false
            else {
                // Višedijelni odgovori su u bazi odvojeni sa ";" — prihvatamo cijeli ili bilo koji dio.
                val prihvatljivi = pitanje.tacno.split(";").map { normalizuj(it) }.filter { it.isNotEmpty() }
                prihvatljivi.any { it == uneseno } || normalizuj(pitanje.tacno) == uneseno
            }
        }
    }

    /** Tekst tačnog odgovora za prikaz učeniku. */
    fun tekstTacnog(pitanje: Pitanje): String = when (pitanje.tip) {
        TipPitanja.ABCD -> {
            val i = "ABCD".indexOf(pitanje.tacno.trim().uppercase())
            if (i in pitanje.opcije.indices) "${pitanje.tacno.uppercase()}) ${pitanje.opcije[i]}" else pitanje.tacno
        }
        TipPitanja.TACNO_NETACNO -> if (pitanje.tacno.trim().uppercase() == "T") "Tačno" else "Netačno"
        TipPitanja.DOPUNI -> pitanje.tacno.split(";").joinToString(" • ") { it.trim() }
    }

    private val BROJEVI = setOf(CharCategory.DECIMAL_DIGIT_NUMBER, CharCategory.LETTER_NUMBER, CharCategory.OTHER_NUMBER)

    /** mala slova, bez dijakritika (č→c, đ→dj, š→s…), bez interpunkcije, jedan razmak. */
    fun normalizuj(s: String): String {
        var t = s.trim().lowercase()
        t = t.replace("đ", "dj")
        // iOS: isto kao Normalizer NFD + uklanjanje \p{Mn} na Androidu
        t = nfd(t).filter { it.category != CharCategory.NON_SPACING_MARK }
        // isto kao "[^\p{L}\p{N}\s]+" → " "
        t = buildString { for (c in t) append(if (c.isLetter() || c.category in BROJEVI || c.isWhitespace()) c else ' ') }
        return t.replace("\\s+".toRegex(), " ").trim()
    }
}
