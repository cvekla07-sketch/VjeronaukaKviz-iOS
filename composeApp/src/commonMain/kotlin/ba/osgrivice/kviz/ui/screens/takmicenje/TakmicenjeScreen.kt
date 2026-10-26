package ba.osgrivice.kviz.ui.screens.takmicenje

import ba.osgrivice.kviz.platform.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.kviz.ProvjeraOdgovora
import ba.osgrivice.kviz.domain.kviz.Takmicenje
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import ba.osgrivice.kviz.ui.components.BedzDijalog
import ba.osgrivice.kviz.ui.components.Konfete
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.components.animiraniBroj
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.theme.Krem
import ba.osgrivice.kviz.ui.theme.Ne
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.Ok
import ba.osgrivice.kviz.ui.theme.OsmokrakaShape
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.PozadinaRezultat
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Siva
import ba.osgrivice.kviz.ui.theme.Tackica
import ba.osgrivice.kviz.ui.theme.Tinta
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.Tirkiz2
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape

@Composable
fun TakmicenjeScreen(razred: Int, naIzlaz: () -> Unit) {
    val app = KvizApp
    val vm: TakmicenjeViewModel = viewModel(factory = TakmicenjeViewModel.factory(app.container, razred))
    val s by vm.state.collectAsStateWithLifecycle()

    var potvrdiIzlaz by remember { mutableStateOf(false) }
    val uToku = s.faza == TakmicenjeFaza.PITANJE
    BackHandler(enabled = uToku) { potvrdiIzlaz = true }

    if (potvrdiIzlaz) {
        AlertDialog(
            onDismissRequest = { potvrdiIzlaz = false },
            title = { Text("Prekinuti simulaciju?") },
            text = { Text("Ovaj pokušaj se neće računati.") },
            confirmButton = { TextButton(onClick = { potvrdiIzlaz = false; vm.prekini(); naIzlaz() }) { Text("Prekini", color = Ne) } },
            dismissButton = { TextButton(onClick = { potvrdiIzlaz = false }) { Text("Nastavi") } }
        )
    }

    when (s.faza) {
        TakmicenjeFaza.UVOD -> UvodEkran(s, naPocni = vm::pocni, naNazad = naIzlaz)
        TakmicenjeFaza.UCITAVANJE -> Box(Modifier.fillMaxSize().background(Krem), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Noc)
        }
        TakmicenjeFaza.PRAZNO -> PraznoStanje(naIzlaz)
        TakmicenjeFaza.PITANJE -> PitanjeEkran(s, naZatvori = { potvrdiIzlaz = true }, naOdgovor = vm::odgovori)
        TakmicenjeFaza.REZULTAT -> {
            RezultatEkran(s, naPonovo = vm::ponovo, naStazu = naIzlaz)
            s.noviBedzevi.firstOrNull()?.let { b ->
                BedzDijalog(bedz = b, preostalo = s.noviBedzevi.size - 1, naDalje = vm::bedzPrikazan)
            }
        }
    }
}

// ============================================================================
//  UVOD — noć
// ============================================================================

@Composable
private fun UvodEkran(s: TakmicenjeUiState, naPocni: () -> Unit, naNazad: () -> Unit) {
    Box(Modifier.fillMaxSize().background(PozadinaNoc)) {
        Ornament()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).clickable(onClick = naNazad),
                    contentAlignment = Alignment.Center
                ) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Simulacija takmičenja", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("${s.razred}. razred", style = MaterialTheme.typography.bodySmall, color = NocTekst)
                }
            }

            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).background(Safran.copy(alpha = 0.2f), OsmokrakaShape))
                    Box(Modifier.size(130.dp).background(Safran, OsmokrakaShape))
                    Box(Modifier.size(90.dp).background(Noc, OsmokrakaShape))
                    Text("🏆", style = MaterialTheme.typography.displaySmall)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Kao na pravom takmičenju", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(
                "Pitanja iz svih cjelina, vrijeme teče, a rezultat vidiš tek na kraju.",
                style = MaterialTheme.typography.bodyMedium, color = NocTekst, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(Modifier.height(22.dp))
            Pravilo("${Takmicenje.BROJ_PITANJA}", "pitanja", "iz svih cjelina ${s.razred}. razreda, ravnomjerno")
            Pravilo("${Takmicenje.SEKUNDI_PO_PITANJU} s", "po pitanju", "istek vremena računa se kao netačno")
            Pravilo("A/B/C/D", "i Tačno/Netačno", "bez dopunjavanja — kao na papiru")
            Pravilo("0", "povratnih informacija", "tačne odgovore i objašnjenja vidiš na kraju")

            Spacer(Modifier.height(18.dp))
            val nb = s.najbolja
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Tvoj najbolji rezultat", style = MaterialTheme.typography.labelSmall, color = NocTekst)
                if (nb == null) {
                    Text("Još nisi radio/la simulaciju", style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.padding(top = 2.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text("${nb.tacnih} / ${nb.ukupno}", style = MaterialTheme.typography.titleLarge, color = Safran)
                        Spacer(Modifier.width(10.dp))
                        Text("· ${Takmicenje.procenat(nb.tacnih, nb.ukupno)} % · ⏱ ${Takmicenje.formatTrajanje(nb.trajanjeMs)}", style = MaterialTheme.typography.bodySmall, color = NocTekst2, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text(
                        "${s.brojOdradjenih} ${if (s.brojOdradjenih == 1) "odrađena simulacija" else "odrađenih simulacija"}",
                        style = MaterialTheme.typography.labelSmall, color = NocTekst, modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Dugme("Počni simulaciju", pozadina = Safran, tekstBoja = Noc, naKlik = naPocni)
            Spacer(Modifier.height(10.dp))
            Text(
                "Nađi mirno mjesto — ${Takmicenje.BROJ_PITANJA} pitanja traje do ${Takmicenje.BROJ_PITANJA * Takmicenje.SEKUNDI_PO_PITANJU / 60} minuta.",
                style = MaterialTheme.typography.bodySmall, color = NocTekst, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Pravilo(vrijednost: String, naziv: String, opis: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(88.dp)) {
            Text(vrijednost, style = MaterialTheme.typography.titleMedium, color = Safran, fontWeight = FontWeight.ExtraBold)
            Text(naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst)
        }
        Spacer(Modifier.width(10.dp))
        Text(opis, style = MaterialTheme.typography.bodySmall, color = NocTekst2, modifier = Modifier.weight(1f))
    }
}

// ============================================================================
//  PITANJE — papir, s tajmerom
// ============================================================================

@Composable
private fun PitanjeEkran(s: TakmicenjeUiState, naZatvori: () -> Unit, naOdgovor: (String) -> Unit) {
    val p = s.trenutno ?: return
    val haptika = LocalHapticFeedback.current
    LaunchedEffect(s.isteklo, s.index) {
        if (s.isteklo) haptika.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // upozorenje u zadnjih 5 s
    val sekunde = (s.preostaloMs + 999) / 1000
    LaunchedEffect(sekunde, s.index) {
        if (!s.zakljucano && sekunde in 1..5) haptika.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(Modifier.fillMaxSize().background(Krem)) {
        Ornament(boja = Noc, alfa = 0.05f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            GornjaTraka(s, naZatvori)

            AnimatedContent(
                targetState = s.index,
                transitionSpec = {
                    (slideInHorizontally(tween(260)) { it / 2 } + fadeIn(tween(260)))
                        .togetherWith(slideOutHorizontally(tween(180)) { -it / 2 } + fadeOut(tween(180)))
                },
                modifier = Modifier.weight(1f),
                label = "pitanje"
            ) { idx ->
                val pit = s.pitanja.getOrNull(idx) ?: p
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pitanje ${idx + 1} od ${s.ukupno}", style = MaterialTheme.typography.labelMedium, color = Siva)
                        Spacer(Modifier.weight(1f))
                        Tajmer(s)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(pit.tekst, style = MaterialTheme.typography.headlineSmall, color = Tinta)
                    Spacer(Modifier.height(22.dp))
                    val odabrano = s.odgovori.getOrNull(idx)
                    when (pit.tip) {
                        TipPitanja.ABCD -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            pit.opcije.forEachIndexed { i, tekst ->
                                val slovo = "ABCD"[i].toString()
                                Opcija(slovo, tekst, odabrana = slovo.equals(odabrano, true), zakljucano = s.zakljucano) { naOdgovor(slovo) }
                            }
                        }
                        TipPitanja.TACNO_NETACNO -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Opcija("✓", "Tačno", odabrana = "T".equals(odabrano, true), zakljucano = s.zakljucano) { naOdgovor("T") }
                            Opcija("✕", "Netačno", odabrana = "N".equals(odabrano, true), zakljucano = s.zakljucano) { naOdgovor("N") }
                        }
                        TipPitanja.DOPUNI -> Unit // ne pojavljuje se u simulaciji
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = s.isteklo,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Row(
                Modifier
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .background(Ne, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text("Vrijeme je isteklo", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun GornjaTraka(s: TakmicenjeUiState, naZatvori: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).shadow(4.dp, CircleShape).background(Color.White, CircleShape).clickable(onClick = naZatvori),
            contentAlignment = Alignment.Center
        ) { Text("✕", color = Siva, style = MaterialTheme.typography.titleMedium) }

        Spacer(Modifier.width(12.dp))

        // Napredak bez otkrivanja tačno/netačno: odgovoreno = noć, trenutno = šafran
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            s.odgovori.forEachIndexed { i, o ->
                val boja = when {
                    i == s.index -> Safran
                    o != null -> Noc
                    else -> Tackica
                }
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(boja))
            }
        }

        Spacer(Modifier.width(12.dp))
        Box(Modifier.background(Noc, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("🏆 ${s.index + 1}/${s.ukupno}", style = MaterialTheme.typography.labelMedium, color = Safran, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Tajmer(s: TakmicenjeUiState) {
    val sekunde = ((s.preostaloMs + 999) / 1000).toInt()
    val ciljna = when {
        s.zakljucano -> Siva
        sekunde <= 5 -> Ne
        sekunde <= 10 -> Safran
        else -> Tirkiz2
    }
    val boja by animateColorAsState(ciljna, tween(250), label = "tajmer")
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val debljina = 5.dp.toPx()
            val r = size.minDimension / 2f - debljina / 2f
            val tl = Offset(size.width / 2f - r, size.height / 2f - r)
            val d = Size(r * 2, r * 2)
            drawArc(Tackica, -90f, 360f, false, tl, d, style = Stroke(debljina, cap = StrokeCap.Round))
            drawArc(boja, -90f, 360f * s.udioVremena, false, tl, d, style = Stroke(debljina, cap = StrokeCap.Round))
        }
        Text(
            sekunde.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = boja,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun Opcija(oznaka: String, tekst: String, odabrana: Boolean, zakljucano: Boolean, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val prigusena = zakljucano && !odabrana
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (prigusena) 0.45f else 1f }
            .pritisak(interaction)
            .shadow(if (zakljucano) 0.dp else 6.dp, RoundedCornerShape(18.dp), ambientColor = Tinta.copy(alpha = 0.2f), spotColor = Tinta.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(18.dp))
            .background(if (odabrana) Noc else Color.White)
            .border(2.dp, if (odabrana) Safran else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = !zakljucano, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(Safran, ZvijezdaShape), contentAlignment = Alignment.Center) {
            Text(oznaka, color = Noc, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            tekst,
            style = MaterialTheme.typography.bodyLarge,
            color = if (odabrana) Color.White else Tinta,
            fontWeight = if (odabrana) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================
//  REZULTAT — noć + pregled
// ============================================================================

@Composable
private fun RezultatEkran(s: TakmicenjeUiState, naPonovo: () -> Unit, naStazu: () -> Unit) {
    val tacnih = animiraniBroj(s.tacnih, 900)
    val xp = animiraniBroj(s.xp, 1100)
    var sva by remember { mutableStateOf(false) }
    val greske = s.pregled.filter { !it.tacno }

    Box(Modifier.fillMaxSize().background(PozadinaRezultat)) {
        Ornament()
        if (s.novaNajbolja && s.procenat >= 50) Konfete()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(190.dp).background(Safran.copy(alpha = 0.25f), OsmokrakaShape))
                Box(Modifier.size(170.dp).background(Safran, OsmokrakaShape))
                Box(Modifier.size(118.dp).background(Noc, OsmokrakaShape))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$tacnih", style = MaterialTheme.typography.displayMedium, color = Safran)
                    Text("od ${s.ukupno}", style = MaterialTheme.typography.labelMedium, color = NocTekst2)
                }
            }

            Spacer(Modifier.height(16.dp))
            if (s.novaNajbolja) {
                Box(
                    Modifier.background(Safran, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp)
                ) { Text("★ Novi najbolji rezultat", style = MaterialTheme.typography.labelLarge, color = Noc) }
                Spacer(Modifier.height(8.dp))
            }
            Text("Simulacija završena", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Text(Takmicenje.ocjena(s.procenat), style = MaterialTheme.typography.bodyMedium, color = NocTekst, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${s.procenat} %", "tačnost", Modifier.weight(1f))
                Stat("⏱ ${Takmicenje.formatTrajanje(s.trajanjeMs)}", "vrijeme", Modifier.weight(1f))
                Stat("+$xp", "XP", Modifier.weight(1f))
            }

            s.najbolja?.takeIf { !s.novaNajbolja }?.let { nb ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "Najbolji: ${nb.tacnih}/${nb.ukupno} · ⏱ ${Takmicenje.formatTrajanje(nb.trajanjeMs)}",
                    style = MaterialTheme.typography.labelSmall, color = NocTekst
                )
            }

            // ---- pregled ----
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (sva) "Sva pitanja" else if (greske.isEmpty()) "Bez grešaka!" else "Greške (${greske.size})",
                    style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f)
                )
                Text(
                    if (sva) "Samo greške" else "Prikaži sva",
                    style = MaterialTheme.typography.labelLarge, color = Tirkiz,
                    modifier = Modifier.clickable { sva = !sva }.padding(6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            (if (sva) s.pregled else greske).forEach { PregledRed(it) }
            if (!sva && greske.isEmpty()) {
                Text("Svaki odgovor je bio tačan. Mašallah!", style = MaterialTheme.typography.bodyMedium, color = NocTekst2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(26.dp))
            Dugme("Ponovo", pozadina = Safran, tekstBoja = Noc, naKlik = naPonovo)
            Spacer(Modifier.height(10.dp))
            Dugme("Na stazu", pozadina = Color.White.copy(alpha = 0.1f), tekstBoja = Color.White, naKlik = naStazu)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PregledRed(r: PregledPitanja) {
    val boja = if (r.tacno) Ok else Ne
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .border(1.dp, boja.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).background(boja, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (r.tacno) "✓" else "✕", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${r.redni}. ${r.pitanje.tekst}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                r.lekcija?.let { Text("Lekcija ${it.redniBroj} · ${it.naslov}", style = MaterialTheme.typography.labelSmall, color = Tirkiz) }
            }
        }
        if (!r.tacno) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (r.odgovor == null) "⏱ Vrijeme je isteklo" else "Tvoj odgovor: ${tekstOdgovora(r.pitanje, r.odgovor)}",
                style = MaterialTheme.typography.bodySmall, color = NocTekst
            )
            Text("Tačno: ${ProvjeraOdgovora.tekstTacnog(r.pitanje)}", style = MaterialTheme.typography.bodySmall, color = NocTekst2, fontWeight = FontWeight.SemiBold)
        }
        if (!r.pitanje.objasnjenje.isNullOrBlank() && !r.tacno) {
            Spacer(Modifier.height(4.dp))
            Text(r.pitanje.objasnjenje, style = MaterialTheme.typography.bodySmall, color = NocTekst)
        }
    }
}

private fun tekstOdgovora(p: Pitanje, odgovor: String): String = when (p.tip) {
    TipPitanja.ABCD -> {
        val i = "ABCD".indexOf(odgovor.trim().uppercase())
        if (i in p.opcije.indices) "${odgovor.uppercase()}) ${p.opcije[i]}" else odgovor
    }
    TipPitanja.TACNO_NETACNO -> if (odgovor.trim().uppercase() == "T") "Tačno" else "Netačno"
    TipPitanja.DOPUNI -> odgovor
}

// ============================================================================
//  zajedničko
// ============================================================================

@Composable
private fun Stat(vrijednost: String, naziv: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(vrijednost, style = MaterialTheme.typography.titleLarge, color = Safran, maxLines = 1)
        Text(naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Dugme(tekst: String, omoguceno: Boolean = true, pozadina: Color, tekstBoja: Color, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pritisak(interaction)
            .graphicsLayer { alpha = if (omoguceno) 1f else 0.4f }
            .clip(RoundedCornerShape(16.dp))
            .background(pozadina)
            .clickable(interactionSource = interaction, indication = null, enabled = omoguceno, onClick = naKlik),
        contentAlignment = Alignment.Center
    ) { Text(tekst, style = MaterialTheme.typography.titleMedium, color = tekstBoja, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun PraznoStanje(naIzlaz: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Krem).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nema dovoljno pitanja za simulaciju.", style = MaterialTheme.typography.titleMedium, color = Tinta, textAlign = TextAlign.Center)
        Text("Provjeri da je baza pitanja učitana.", style = MaterialTheme.typography.bodyMedium, color = Siva, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        Dugme("Na stazu", pozadina = Noc, tekstBoja = Color.White, naKlik = naIzlaz)
    }
}
