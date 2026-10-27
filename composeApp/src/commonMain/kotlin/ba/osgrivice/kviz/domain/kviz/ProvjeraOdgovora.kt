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
            // Ranije: višedijelni odgovor je bio priznat ako je dijete napisalo BILO KOJI jedan dio (greška).
            // Sada: svi dijelovi moraju biti tačni (djelimični bodovi se računaju u Dopuna.ocijeni).
            val s = Dopuna.struktura(pitanje)
            if (s.brojPraznina <= 1) Dopuna.slicno(odgovor, pitanje.tacno)
            else Dopuna.ocijeni(s, odgovor.split(";")).sveTacno
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
