package ba.osgrivice.kviz.ui.screens.staza

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.kviz.Takmicenje
import ba.osgrivice.kviz.domain.kviz.StanjeLekcije
import ba.osgrivice.kviz.domain.kviz.StatusLekcije
import ba.osgrivice.kviz.domain.model.VrstaSesije
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.SafranTamno
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.Tirkiz2
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape

private val VISINA_CVORA = 118.dp
private val POZICIJE = floatArrayOf(0.5f, 0.26f, 0.5f, 0.74f)

@Composable
fun StazaScreen(
    naKviz: (razred: Int, vrsta: String, lekcijaId: String?) -> Unit,
    naPregled: (razred: Int, lekcijaId: String) -> Unit,
    naProfil: () -> Unit,
    naRang: (razred: Int) -> Unit,
    naTakmicenje: (razred: Int) -> Unit
) {
    val app = KvizApp
    val vm: StazaViewModel = viewModel(factory = StazaViewModel.factory(app.container))
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.osvjezi() }

    val lista = rememberLazyListState()
    LaunchedEffect(s.ucitano, s.aktivna?.lekcija?.id) {
        val idx = s.lekcije.indexOfFirst { it.status == StatusLekcije.AKTIVNA }
        if (idx > 1) lista.scrollToItem(idx - 1)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PozadinaNoc)
    ) {
        Ornament()

        LazyColumn(
            state = lista,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
        ) {
            item { Zaglavlje(s, naProfil, { naRang(s.razred) }) }
            item { Trake(s, naPonavljanje = { naKviz(s.razred, VrstaSesije.PONAVLJANJE.kljuc, null) }) }
            item { if (s.ucitano) TakmicenjeKartica(s, naKlik = { naTakmicenje(s.razred) }) }
            item { Spacer(Modifier.height(18.dp)) }
            itemsIndexed(s.lekcije, key = { _, l -> l.lekcija.id }) { i, l ->
                Cvor(
                    stanje = l,
                    index = i,
                    posljednji = i == s.lekcije.lastIndex,
                    naKlik = { naPregled(s.razred, l.lekcija.id) }
                )
            }
        }

        if (s.ucitano) {
            DonjaAkcija(
                s = s,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                naKlik = {
                    val a = s.aktivna
                    if (a != null) naPregled(s.razred, a.lekcija.id)
                    else naKviz(s.razred, VrstaSesije.PONAVLJANJE.kljuc, null)
                }
            )
        }
    }
}

@Composable
private fun Zaglavlje(s: StazaUiState, naProfil: () -> Unit, naRang: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            if (s.ime.isBlank()) {
                Text("Selam alejk!", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            } else {
                Text("Selam alejk,", style = MaterialTheme.typography.bodyLarge, color = NocTekst)
                Text(
                    s.ime,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                when (s.daniDoTakmicenja) {
                    0L -> "Takmičenje je danas!"
                    1L -> "Takmičenje je sutra"
                    else -> "Do takmičenja još ${s.daniDoTakmicenja} dana"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NocTekst,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(
            Modifier
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔥 ${s.streak}", style = MaterialTheme.typography.titleLarge, color = Safran)
            Text(if (s.streak == 1) "dan u nizu" else "dana u nizu", style = MaterialTheme.typography.labelSmall, color = NocTekst)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = naProfil),
                contentAlignment = Alignment.Center
            ) { Text("☰", color = Color.White, style = MaterialTheme.typography.titleMedium) }
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = naRang),
                contentAlignment = Alignment.Center
            ) { Text("🏆", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun Trake(s: StazaUiState, naPonavljanje: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Traka(
            naslov = "Današnji cilj",
            vrijednost = "${s.danasOdgovora.coerceAtMost(s.dnevniCilj)} / ${s.dnevniCilj} pitanja",
            udio = if (s.dnevniCilj == 0) 0f else s.danasOdgovora.toFloat() / s.dnevniCilj,
            modifier = Modifier.weight(1f)
        )
        Traka(
            naslov = "Tačno",
            vrijednost = "${s.savladano} od ${s.ukupnoPitanja}",
            udio = if (s.ukupnoPitanja == 0) 0f else s.savladano.toFloat() / s.ukupnoPitanja,
            modifier = Modifier.weight(1f)
        )
    }
    if (s.brojDospjelih > 0) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .pritisak(interaction)
                .clip(RoundedCornerShape(50))
                .background(Tirkiz.copy(alpha = 0.14f))
                .clickable(interactionSource = interaction, indication = null, onClick = naPonavljanje)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔁", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                "${s.brojDospjelih} ${if (s.brojDospjelih == 1) "pitanje čeka" else "pitanja čekaju"} ponavljanje",
                style = MaterialTheme.typography.labelLarge,
                color = Tirkiz
            )
        }
    }
}

/** Ulaz u simulaciju takmičenja — kartica ispod traka (korak 6). */
@Composable
private fun TakmicenjeKartica(s: StazaUiState, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val nb = s.najboljaSimulacija
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .pritisak(interaction)
            .clip(RoundedCornerShape(16.dp))
            .background(Safran.copy(alpha = 0.14f))
            .clickable(interactionSource = interaction, indication = null, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(Safran, ZvijezdaShape), contentAlignment = Alignment.Center) {
            Text("🏆", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Simulacija takmičenja", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(
                if (nb == null) "${Takmicenje.BROJ_PITANJA} pitanja · ${Takmicenje.SEKUNDI_PO_PITANJU} s po pitanju · sve cjeline"
                else "Najbolje ${nb.tacnih}/${nb.ukupno} · ⏱ ${Takmicenje.formatTrajanje(nb.trajanjeMs)} · ${s.brojSimulacija}× odrađeno",
                style = MaterialTheme.typography.bodySmall,
                color = NocTekst
            )
        }
        Text("›", color = Safran, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Traka(naslov: String, vrijednost: String, udio: Float, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(naslov, style = MaterialTheme.typography.labelSmall, color = NocTekst)
        Text(vrijednost, style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.padding(top = 2.dp))
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(udio.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Tirkiz)
            )
        }
    }
}

@Composable
private fun Cvor(stanje: StanjeLekcije, index: Int, posljednji: Boolean, naKlik: () -> Unit) {
    val x = POZICIJE[index % POZICIJE.size]
    val xPrije = if (index == 0) x else POZICIJE[(index - 1) % POZICIJE.size]
    val xPoslije = if (posljednji) x else POZICIJE[(index + 1) % POZICIJE.size]
    val aktivan = stanje.status == StatusLekcije.AKTIVNA
    val zakljucan = stanje.status == StatusLekcije.ZAKLJUCANA
    val velicina: Dp = if (aktivan) 84.dp else 62.dp

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(VISINA_CVORA)
    ) {
        val sirina = maxWidth
        val visina = maxHeight

        Canvas(Modifier.fillMaxSize()) {
            val boja = Color.White.copy(alpha = 0.2f)
            val efekat = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 10.dp.toPx()))
            val cx = size.width * x
            val cy = size.height / 2f
            if (index > 0) {
                drawLine(boja, Offset(size.width * (x + xPrije) / 2f, 0f), Offset(cx, cy), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = efekat)
            }
            if (!posljednji) {
                drawLine(boja, Offset(cx, cy), Offset(size.width * (x + xPoslije) / 2f, size.height), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = efekat)
            }
        }

        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .offset(x = sirina * x - velicina / 2, y = visina / 2 - velicina / 2)
                .size(velicina)
                .pritisak(interaction)
                .clickable(interactionSource = interaction, indication = null, enabled = !zakljucan, onClick = naKlik),
            contentAlignment = Alignment.Center
        ) {
            if (aktivan) {
                Box(
                    Modifier
                        .size(velicina)
                        .background(Safran.copy(alpha = 0.28f), CircleShape)
                )
            }
            Box(
                Modifier
                    .size(if (aktivan) velicina - 10.dp else velicina)
                    .background(
                        when (stanje.status) {
                            StatusLekcije.ZAVRSENA -> Tirkiz
                            StatusLekcije.ZAKLJUCANA -> Color.White.copy(alpha = 0.14f)
                            else -> Safran
                        },
                        ZvijezdaShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (stanje.status == StatusLekcije.ZAVRSENA) "✓" else stanje.lekcija.redniBroj.toString(),
                    style = if (aktivan) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = if (zakljucan) NocTekst else Noc,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // natpis pod čvorom
        Column(
            Modifier
                .offset(x = sirina * x - 70.dp, y = visina / 2 + velicina / 2 + 2.dp)
                .width(140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stanje.lekcija.naslov,
                style = MaterialTheme.typography.labelMedium,
                color = if (zakljucan) NocTekst else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (!zakljucan) {
                Row {
                    repeat(3) { i ->
                        val puna = i < stanje.zvjezdice
                        Text(
                            if (puna) "★" else "☆",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                !puna -> NocTekst
                                stanje.status == StatusLekcije.ZAVRSENA -> Tirkiz
                                else -> Safran
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonjaAkcija(s: StazaUiState, modifier: Modifier, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val a = s.aktivna
    val naslov = when {
        a != null -> "Nastavi — ${a.lekcija.naslov}"
        s.brojDospjelih > 0 -> "Ponavljanje"
        else -> "Sve savladano!"
    }
    val pod = when {
        a != null && s.planNovih + s.planPonavljanja > 0 ->
            listOfNotNull(
                s.planNovih.takeIf { it > 0 }?.let { "$it ${if (it == 1) "novo" else "novih"}" },
                s.planPonavljanja.takeIf { it > 0 }?.let { "$it za ponavljanje" }
            ).joinToString(" + ")
        a != null -> "10 pitanja"
        s.brojDospjelih > 0 -> "${s.brojDospjelih} pitanja dospjelo"
        else -> "Vrati se sutra za ponavljanje"
    }
    val omoguceno = a != null || s.brojDospjelih > 0

    Row(
        modifier
            .fillMaxWidth()
            .pritisak(interaction)
            .clip(RoundedCornerShape(18.dp))
            .background(if (omoguceno) Safran else Color.White.copy(alpha = 0.12f))
            .clickable(interactionSource = interaction, indication = null, enabled = omoguceno, onClick = naKlik)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(naslov, style = MaterialTheme.typography.titleMedium, color = if (omoguceno) Noc else NocTekst2, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(pod, style = MaterialTheme.typography.bodySmall, color = if (omoguceno) SafranTamno else NocTekst)
        }
        Box(
            Modifier
                .size(40.dp)
                .background(if (omoguceno) Noc else Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("›", color = if (omoguceno) Safran else NocTekst, style = MaterialTheme.typography.titleLarge) }
    }
}
