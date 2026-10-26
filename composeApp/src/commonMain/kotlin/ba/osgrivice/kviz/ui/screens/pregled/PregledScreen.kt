package ba.osgrivice.kviz.ui.screens.pregled

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.model.TipPitanja
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.theme.Krem
import ba.osgrivice.kviz.ui.theme.Linija
import ba.osgrivice.kviz.ui.theme.Ne
import ba.osgrivice.kviz.ui.theme.NeBg
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.Ok
import ba.osgrivice.kviz.ui.theme.OkBg
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Siva
import ba.osgrivice.kviz.ui.theme.Tinta
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.Tirkiz2

/**
 * Pregled nivoa prije kviza — sva pitanja s tačnim odgovorom i objašnjenjem, na "papiru".
 * Učenik čita, pa dolje klikne "Kreni na kviz".
 */
@Composable
fun PregledScreen(
    razred: Int,
    lekcijaId: String,
    naNazad: () -> Unit,
    naKviz: () -> Unit
) {
    val app = KvizApp
    val vm: PregledViewModel = viewModel(
        key = "pregled_$lekcijaId",
        factory = PregledViewModel.factory(app.container, lekcijaId)
    )
    val s by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Krem)) {
        Ornament(boja = Noc, alfa = 0.05f)

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Zaglavlje(s, razred, naNazad)

            if (!s.ucitano) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Safran) }
            } else if (s.stavke.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("U ovom nivou još nema pitanja.", color = Siva, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(s.stavke, key = { _, st -> st.pitanje.id }) { i, st -> Kartica(i + 1, st) }
                }
            }
        }

        if (s.ucitano && s.stavke.isNotEmpty()) {
            DonjeDugme(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                naKlik = naKviz
            )
        }
    }
}

@Composable
private fun Zaglavlje(s: PregledUiState, razred: Int, naNazad: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .clickable(onClick = naNazad),
            contentAlignment = Alignment.Center
        ) { Text("‹", color = Siva, style = MaterialTheme.typography.headlineSmall) }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                s.lekcija?.naslov ?: "Pregled",
                style = MaterialTheme.typography.titleLarge,
                color = Tinta,
                maxLines = 1
            )
            val pod = if (s.ucitano) "$razred. razred · tačno ${s.tacnih} od ${s.ukupno}" else "$razred. razred"
            Text(pod, style = MaterialTheme.typography.bodySmall, color = Siva)
            if (s.ucitano && s.ukupno > 0) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Linija)) {
                    Box(
                        Modifier
                            .fillMaxWidth(s.tacnih.toFloat() / s.ukupno)
                            .height(6.dp)
                            .background(Tirkiz2, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun Kartica(redni: Int, st: StavkaPregleda) {
    val p = st.pitanje
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Tinta.copy(alpha = 0.15f), spotColor = Tinta.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(26.dp).background(Noc, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(redni.toString(), color = Safran, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.width(8.dp))
            Text(nazivTipa(p.tip), style = MaterialTheme.typography.labelSmall, color = Siva)
            Spacer(Modifier.weight(1f))
            when (st.zadnjiTacan) {
                true -> Oznaka("✓", Ok, OkBg)
                false -> Oznaka("✗", Ne, NeBg)
                null -> {}
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(p.tekst, style = MaterialTheme.typography.titleMedium, color = Tinta)

        if (p.tip == TipPitanja.ABCD && p.opcije.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val tacno = p.tacno.trim().uppercase()
            p.opcije.forEachIndexed { i, tekst ->
                val slovo = "ABCD"[i].toString()
                val jeTacna = slovo == tacno
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        "$slovo)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (jeTacna) Tirkiz2 else Siva,
                        fontWeight = if (jeTacna) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        tekst,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (jeTacna) Tinta else Siva,
                        fontWeight = if (jeTacna) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Tirkiz.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✓", color = Tirkiz2, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Text(st.tacanOdgovor, style = MaterialTheme.typography.bodyMedium, color = Tinta, fontWeight = FontWeight.SemiBold)
        }

        val obj = p.objasnjenje?.trim().orEmpty()
        if (obj.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Linija))
            Spacer(Modifier.height(8.dp))
            Text(obj, style = MaterialTheme.typography.bodySmall, color = Siva, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun Oznaka(znak: String, boja: Color, pozadina: Color) {
    Box(
        Modifier.size(26.dp).background(pozadina, CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(znak, color = boja, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun DonjeDugme(modifier: Modifier, naKlik: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Safran.copy(alpha = 0.4f), spotColor = Safran.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(20.dp))
            .background(Safran)
            .clickable(onClick = naKlik)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("▶  Kreni na kviz", style = MaterialTheme.typography.labelLarge, color = Noc, fontWeight = FontWeight.ExtraBold)
    }
}

private fun nazivTipa(tip: TipPitanja): String = when (tip) {
    TipPitanja.ABCD -> "A / B / C / D"
    TipPitanja.TACNO_NETACNO -> "Tačno / Netačno"
    TipPitanja.DOPUNI -> "Dopuni"
}
