package ba.osgrivice.kviz.data.seed

import kotlinx.serialization.Serializable

/** Format fajla assets/pitanja_seed.json. */
@Serializable
data class SeedFajl(
    val verzija: Int,
    val lekcije: List<SeedLekcija> = emptyList(),
    val pitanja: List<SeedPitanje>
)

@Serializable
data class SeedLekcija(
    val id: String,
    val razred: Int,
    val redniBroj: Int,
    val naslov: String,
    val opis: String? = null
)

@Serializable
data class SeedPitanje(
    val id: String,
    val razred: Int,
    val lekcija: String? = null,
    val tip: String,
    val tekst: String,
    val opcije: List<String> = emptyList(),
    val tacno: String,
    val objasnjenje: String? = null,
    /** false = alat ga je povukao (ostaje u bazi radi historije odgovora, ne prikazuje se). */
    val aktivno: Boolean = true
)
