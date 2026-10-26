package ba.osgrivice.kviz.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.data.remote.BedzPregled
import ba.osgrivice.kviz.data.remote.LekcijaPregled
import ba.osgrivice.kviz.data.remote.NemaPristupaException
import ba.osgrivice.kviz.data.remote.SesijaPregled
import ba.osgrivice.kviz.domain.kviz.Bedzevi
import ba.osgrivice.kviz.domain.kviz.Takmicenje
import ba.osgrivice.kviz.domain.model.Lekcija
import ba.osgrivice.kviz.domain.model.VrstaSesije
import ba.osgrivice.kviz.ui.components.BedzIkona
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Tirkiz

private sealed interface Stanje {
    data object Ucitava : Stanje
    data class Greska(val tekst: String) : Stanje
    data class Podaci(
        val lekcije: List<Lekcija>,
        val napredak: Map<String, LekcijaPregled>,
        val sesije: List<SesijaPregled>,
        val bedzevi: List<BedzPregled>
    ) : Stanje
}

@Composable
fun AdminUcenikScreen(uid: String, ime: String, razred: Int, naNazad: () -> Unit) {
    val app = KvizApp
    val stanje by produceState<Stanje>(initialValue = Stanje.Ucitava, uid) {
        val c = app.container
        value = runCatching {
            Stanje.Podaci(
                lekcije = c.pitanjaRepository.lekcije(razred),
                napredak = c.adminFirestore.lekcije(uid).associateBy { it.lekcijaId },
                sesije = c.adminFirestore.sesije(uid),
                bedzevi = c.adminFirestore.bedzevi(uid)
            )
        }.getOrElse { e ->
            Stanje.Greska(if (e is NemaPristupaException) "Nema pristupa ovom učeniku." else "Greška pri čitanju:\n${e.message}")
        }
    }

    Box(Modifier.fillMaxSize().background(PozadinaNoc)) {
        Ornament()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Zaglavlje(naslov = ime, pod = "$razred. razred", naNazad = naNazad)
            Spacer(Modifier.height(12.dp))
            when (val st = stanje) {
                Stanje.Ucitava -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Safran) }
                is Stanje.Greska -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(st.tekst, color = NocTekst, textAlign = TextAlign.Center)
                }
                is Stanje.Podaci -> Sadrzaj(st)
            }
        }
    }
}

@Composable
private fun Sadrzaj(p: Stanje.Podaci) {
    val lekcijePoId = p.lekcije.associateBy { it.id }
    val zvjezdiceUkupno = p.napredak.values.sumOf { it.zvjezdice }
    val zavrsene = p.napredak.values.count { it.zvjezdice == 3 }
    val zapocete = p.napredak.values.count { it.vidjeno > 0 }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("$zapocete/${p.lekcije.size}", "lekcija započeto", Modifier.weight(1f))
                Stat("$zavrsene", "sa ★★★", Modifier.weight(1f))
                Stat("$zvjezdiceUkupno/${p.lekcije.size * 3}", "zvjezdica", Modifier.weight(1f))
            }
        }

        // ---- bedževi ----
        item {
            Spacer(Modifier.height(8.dp))
            Text("Bedževi (${p.bedzevi.size}/${Bedzevi.SVI.size})", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        if (p.bedzevi.isEmpty()) {
            item { Text("Još nema bedževa.", style = MaterialTheme.typography.bodySmall, color = NocTekst) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.bedzevi.sortedBy { it.dodijeljenMs }.take(7).forEach { b ->
                        Bedzevi.poId(b.id)?.let { BedzIkona(it, osvojen = true, velicina = 40.dp) }
                    }
                }
            }
            items(p.bedzevi.sortedByDescending { it.dodijeljenMs }, key = { "b" + it.id }) { b ->
                val def = Bedzevi.poId(b.id)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(def?.emoji ?: "🏅", modifier = Modifier.width(28.dp))
                    Text(def?.naziv ?: b.id, style = MaterialTheme.typography.bodyMedium, color = NocTekst2, modifier = Modifier.weight(1f))
                    Text(formatDatum(b.dodijeljenMs), style = MaterialTheme.typography.labelSmall, color = NocTekst)
                }
            }
        }

        // ---- staza ----
        item {
            Spacer(Modifier.height(8.dp))
            Text("Staza", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        items(p.lekcije, key = { "l" + it.id }) { l ->
            val n = p.napredak[l.id]
            val zv = n?.zvjezdice ?: 0
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = if (n == null) 0.04f else 0.07f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${l.redniBroj}.", style = MaterialTheme.typography.labelMedium, color = NocTekst, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.naslov, style = MaterialTheme.typography.bodyMedium, color = if (n == null) NocTekst else Color.White, maxLines = 1)
                    if (n != null) Text("savladano ${n.savladano}/${n.ukupno} · viđeno ${n.vidjeno}", style = MaterialTheme.typography.labelSmall, color = NocTekst)
                }
                Text(
                    if (n == null) "—" else "★".repeat(zv) + "☆".repeat(3 - zv),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (zv == 3) Tirkiz else Safran
                )
            }
        }

        // ---- sesije ----
        item {
            Spacer(Modifier.height(8.dp))
            Text("Zadnje sesije", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        if (p.sesije.isEmpty()) {
            item { Text("Nema sesija.", style = MaterialTheme.typography.bodySmall, color = NocTekst) }
        }
        items(p.sesije, key = { "s" + it.pocetakMs }) { s ->
            val vrsta = VrstaSesije.izKljuca(s.vrsta)
            val naziv = when (vrsta) {
                VrstaSesije.SIMULACIJA -> "🏆 Simulacija"
                VrstaSesije.PONAVLJANJE -> "🔁 Ponavljanje"
                VrstaSesije.GRESKE -> "✕ Greške"
                VrstaSesije.DNEVNI_TRENING -> "Dnevni trening"
                VrstaSesije.LEKCIJA -> s.lekcijaId?.let { lekcijePoId[it]?.naslov } ?: "Lekcija"
            }
            val trajanje = s.krajMs?.let { Takmicenje.formatTrajanje(it - s.pocetakMs) }
            Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(naziv, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1)
                    Text(
                        listOfNotNull(formatDatumVrijeme(s.pocetakMs), trajanje?.let { "⏱ $it" }).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = NocTekst
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.tacnih}/${s.ukupno}", style = MaterialTheme.typography.titleSmall, color = if (s.ukupno > 0 && s.tacnih == s.ukupno) Tirkiz else Safran)
                    Text("+${s.xp} XP", style = MaterialTheme.typography.labelSmall, color = NocTekst)
                }
            }
        }
    }
}
