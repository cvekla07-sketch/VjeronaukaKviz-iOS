package ba.osgrivice.kviz.domain.model

/** Tip pitanja — vrijednost `kljuc` je ono što stoji u bazi i u seed JSON-u. */
enum class TipPitanja(val kljuc: String, val naziv: String) {
    ABCD("abcd", "A / B / C / D"),
    TACNO_NETACNO("tn", "Tačno / Netačno"),
    DOPUNI("dopuni", "Dopuni");

    companion object {
        fun izKljuca(k: String): TipPitanja = entries.firstOrNull { it.kljuc == k } ?: ABCD
    }
}

data class Lekcija(
    val id: String,
    val razred: Int,
    val redniBroj: Int,
    val naslov: String,
    val opis: String? = null
)

data class Pitanje(
    val id: String,
    val razred: Int,
    val lekcijaId: String?,
    val tip: TipPitanja,
    val tekst: String,
    /** ABCD: četiri opcije redom A,B,C,D. Ostali tipovi: prazna lista. */
    val opcije: List<String>,
    /** ABCD: "A".."D"; TN: "T"/"N"; DOPUNI: tekst odgovora. */
    val tacno: String,
    val objasnjenje: String?,
    val aktivno: Boolean = true,
    val verzija: Int = 1
)

/**
 * Stanje jednog pitanja za jednog učenika (razmaknuto ponavljanje).
 * kutija 0 = još neviđeno; raste sa svakim tačnim, pada na 0 (ili -1 kutija) sa netačnim.
 */
data class NapredakPitanja(
    val pitanjeId: String,
    val kutija: Int = 0,
    /** Epoch dan (broj dana od 1970-01-01) kada pitanje ponovo dospijeva. 0 = odmah. */
    val sljedeceDan: Long = 0,
    val tacnih: Int = 0,
    val netacnih: Int = 0,
    val zadnjiOdgovorMs: Long = 0
) {
    val vidjeno: Boolean get() = tacnih + netacnih > 0
}

enum class VrstaSesije(val kljuc: String) {
    DNEVNI_TRENING("dnevni"),
    LEKCIJA("lekcija"),
    PONAVLJANJE("ponavljanje"),
    GRESKE("greske"),
    SIMULACIJA("simulacija");

    companion object {
        fun izKljuca(k: String): VrstaSesije = entries.firstOrNull { it.kljuc == k } ?: DNEVNI_TRENING
    }
}
