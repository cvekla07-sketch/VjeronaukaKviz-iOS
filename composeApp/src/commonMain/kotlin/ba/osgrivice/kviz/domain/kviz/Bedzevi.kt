package ba.osgrivice.kviz.domain.kviz

import ba.osgrivice.kviz.domain.model.VrstaSesije

enum class GrupaBedza(val naziv: String) {
    STAZA("Staza"),
    NIZ("Istrajnost"),
    ODGOVORI("Odgovori"),
    TAKMICENJE("Takmičenje")
}

/** Definicija bedža — ID je ono što stoji u bazi i Firestoreu, nikad ga ne mijenjati. */
data class Bedz(
    val id: String,
    val naziv: String,
    /** Uslov, napisan učeniku ("7 dana u nizu"). */
    val opis: String,
    val emoji: String,
    val grupa: GrupaBedza
)

/** Sažetak zadnje završene sesije — ulaz za bedževe vezane uz jednu sesiju. */
data class SesijaSazetak(val vrsta: VrstaSesije, val tacnih: Int, val ukupno: Int)

/** Sve što je potrebno da se izračuna koji su bedževi ispunjeni — čisti podaci, bez Androida. */
data class StanjeZaBedzeve(
    /** Zvjezdice po lekciji razreda (0–3), redom staze. */
    val zvjezdicePoLekciji: List<Int>,
    val streak: Int,
    val ukupnoTacnih: Int,
    val brojSimulacija: Int,
    val najboljaSimulacijaTacnih: Int?,
    val najboljaSimulacijaUkupno: Int?,
    val zadnjaSesija: SesijaSazetak?
)

/**
 * Pravila dodjele bedževa na jednom mjestu. Bedž se dodjeljuje jednom i ne oduzima se
 * (ni reset streak-a ga ne briše) — jedino "Resetuj napredak" briše sve.
 */
object Bedzevi {

    const val PRVA_LEKCIJA = "prva_lekcija"
    const val TRI_ZVJEZDICE = "tri_zvjezdice"
    const val POLA_STAZE = "pola_staze"
    const val CIJELA_STAZA = "cijela_staza"
    const val SVE_ZLATNO = "sve_zlatno"
    const val NIZ_3 = "niz_3"
    const val NIZ_7 = "niz_7"
    const val NIZ_30 = "niz_30"
    const val TACNIH_100 = "tacnih_100"
    const val TACNIH_500 = "tacnih_500"
    const val BEZ_GRESKE = "bez_greske"
    const val PRVA_SIMULACIJA = "prva_simulacija"
    const val SIMULACIJA_15 = "simulacija_15"
    const val SIMULACIJA_20 = "simulacija_20"

    val SVI: List<Bedz> = listOf(
        Bedz(PRVA_LEKCIJA, "Prvi korak", "Završi prvu lekciju", "🌙", GrupaBedza.STAZA),
        Bedz(TRI_ZVJEZDICE, "Zlatna lekcija", "Osvoji 3 zvjezdice na jednoj lekciji", "⭐", GrupaBedza.STAZA),
        Bedz(POLA_STAZE, "Pola puta", "Pređi polovinu staze", "🧭", GrupaBedza.STAZA),
        Bedz(CIJELA_STAZA, "Kraj staze", "Pređi cijelu stazu", "🏁", GrupaBedza.STAZA),
        Bedz(SVE_ZLATNO, "Hafiz staze", "Sve lekcije sa 3 zvjezdice", "👑", GrupaBedza.STAZA),

        Bedz(NIZ_3, "Istrajnost I", "3 dana u nizu", "🔥", GrupaBedza.NIZ),
        Bedz(NIZ_7, "Istrajnost II", "7 dana u nizu", "🔥", GrupaBedza.NIZ),
        Bedz(NIZ_30, "Istrajnost III", "30 dana u nizu", "🔥", GrupaBedza.NIZ),

        Bedz(TACNIH_100, "Stotka", "100 tačnih odgovora", "💯", GrupaBedza.ODGOVORI),
        Bedz(TACNIH_500, "Petstotka", "500 tačnih odgovora", "🎯", GrupaBedza.ODGOVORI),
        Bedz(BEZ_GRESKE, "Bez greške", "Sesija bez ijedne greške", "✨", GrupaBedza.ODGOVORI),

        Bedz(PRVA_SIMULACIJA, "Prvo takmičenje", "Završi prvu simulaciju", "🏆", GrupaBedza.TAKMICENJE),
        Bedz(SIMULACIJA_15, "Takmičar", "15 ili više od 20 u simulaciji", "🥈", GrupaBedza.TAKMICENJE),
        Bedz(SIMULACIJA_20, "Šampion", "20 od 20 u simulaciji", "🥇", GrupaBedza.TAKMICENJE)
    )

    private val poId: Map<String, Bedz> = SVI.associateBy { it.id }

    fun poId(id: String): Bedz? = poId[id]

    fun uslovIspunjen(id: String, s: StanjeZaBedzeve): Boolean {
        val lekcija = s.zvjezdicePoLekciji
        val ukupnoLekcija = lekcija.size
        val barJedna = lekcija.count { it >= 1 }
        val sim = s.najboljaSimulacijaTacnih
        val simUkupno = s.najboljaSimulacijaUkupno ?: Takmicenje.BROJ_PITANJA
        return when (id) {
            PRVA_LEKCIJA -> barJedna >= 1
            TRI_ZVJEZDICE -> lekcija.any { it == 3 }
            POLA_STAZE -> ukupnoLekcija > 0 && barJedna * 2 >= ukupnoLekcija
            CIJELA_STAZA -> ukupnoLekcija > 0 && barJedna == ukupnoLekcija
            SVE_ZLATNO -> ukupnoLekcija > 0 && lekcija.all { it == 3 }
            NIZ_3 -> s.streak >= 3
            NIZ_7 -> s.streak >= 7
            NIZ_30 -> s.streak >= 30
            TACNIH_100 -> s.ukupnoTacnih >= 100
            TACNIH_500 -> s.ukupnoTacnih >= 500
            BEZ_GRESKE -> s.zadnjaSesija?.let { it.ukupno >= 10 && it.tacnih == it.ukupno } == true
            PRVA_SIMULACIJA -> s.brojSimulacija >= 1
            SIMULACIJA_15 -> sim != null && sim * 20 >= simUkupno * 15
            SIMULACIJA_20 -> sim != null && simUkupno > 0 && sim == simUkupno
            else -> false
        }
    }

    /** Bedževi koje učenik tek sada ispunjava, a još ih nema — redom kako su definisani. */
    fun provjeri(s: StanjeZaBedzeve, osvojeni: Set<String>): List<Bedz> =
        SVI.filter { it.id !in osvojeni && uslovIspunjen(it.id, s) }
}
