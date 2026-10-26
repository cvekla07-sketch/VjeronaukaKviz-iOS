package ba.osgrivice.kviz.ui.screens.kviz

import ba.osgrivice.kviz.platform.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.kviz.ProvjeraOdgovora
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.domain.model.TipPitanja
import ba.osgrivice.kviz.domain.model.VrstaSesije
import ba.osgrivice.kviz.ui.components.BedzDijalog
import ba.osgrivice.kviz.ui.components.Konfete
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.components.animiraniBroj
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.components.tresenje
import ba.osgrivice.kviz.ui.theme.Krem
import ba.osgrivice.kviz.ui.theme.Linija
import ba.osgrivice.kviz.ui.theme.Ne
import ba.osgrivice.kviz.ui.theme.NeBg
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.Ok
import ba.osgrivice.kviz.ui.theme.OkBg
import ba.osgrivice.kviz.ui.theme.OsmokrakaShape
import ba.osgrivice.kviz.ui.theme.PozadinaRezultat
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.SafranTamno
import ba.osgrivice.kviz.ui.theme.Siva
import ba.osgrivice.kviz.ui.theme.Tackica
import ba.osgrivice.kviz.ui.theme.Tinta
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.Tirkiz2
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape

@Composable
fun KvizScreen(
    razred: Int,
    vrstaKljuc: String,
    lekcijaId: String?,
    naIzlaz: () -> Unit,
    naSljedecu: (lekcijaId: String) -> Unit
) {
    val app = KvizApp
    val vrsta = VrstaSesije.izKljuca(vrstaKljuc)
    val vm: KvizViewModel = viewModel(factory = KvizViewModel.factory(app.container, razred, vrsta, lekcijaId))
    val state by vm.state.collectAsStateWithLifecycle()

    var potvrdiIzlaz by remember { mutableStateOf(false) }
    val uToku = state.faza == KvizFaza.PITANJE && (state.index > 0 || state.provjereno)
    BackHandler(enabled = uToku) { potvrdiIzlaz = true }

    if (potvrdiIzlaz) {
        AlertDialog(
            onDismissRequest = { potvrdiIzlaz = false },
            title = { Text("Napustiti vježbu?") },
            text = { Text("Odgovori do sada ostaju sačuvani.") },
            confirmButton = { TextButton(onClick = { potvrdiIzlaz = false; naIzlaz() }) { Text("Napusti") } },
            dismissButton = { TextButton(onClick = { potvrdiIzlaz = false }) { Text("Ostani") } }
        )
    }

    when (state.faza) {
        KvizFaza.UCITAVANJE -> Box(Modifier.fillMaxSize().background(Krem), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Noc)
        }
        KvizFaza.PRAZNO -> PraznoStanje(naIzlaz)
        KvizFaza.PITANJE -> PitanjeEkran(
            state = state,
            naZatvori = {
                if (uToku) {
                    potvrdiIzlaz = true
                } else {
                    naIzlaz()
                }
            },
            naOdgovor = vm::odgovori,
            naUnos = vm::promijeniUnos,
            naNastavi = vm::nastavi
        )
        KvizFaza.REZULTAT -> {
            RezultatEkran(state, naPocetna = naIzlaz, naSljedecu = naSljedecu)
            state.noviBedzevi.firstOrNull()?.let { b ->
                BedzDijalog(bedz = b, preostalo = state.noviBedzevi.size - 1, naDalje = vm::bedzPrikazan)
            }
        }
    }
}

// ============================================================================
//  PITANJE — "papir"
// ============================================================================

@Composable
private fun PitanjeEkran(
    state: KvizUiState,
    naZatvori: () -> Unit,
    naOdgovor: (String) -> Unit,
    naUnos: (String) -> Unit,
    naNastavi: () -> Unit
) {
    val p = state.trenutno ?: return
    val haptika = LocalHapticFeedback.current
    LaunchedEffect(state.provjereno, state.index) {
        if (state.provjereno) haptika.performHapticFeedback(if (state.tacno) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
    }

    Box(Modifier.fillMaxSize().background(Krem)) {
        Ornament(boja = Noc, alfa = 0.05f)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            GornjaTraka(state, naZatvori)

            AnimatedContent(
                targetState = state.index,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "pitanje"
            ) { idx ->
                val pit = state.pitanja.getOrNull(idx) ?: p
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    state.lekcija?.let { l ->
                        Row {
                            Text("Lekcija ${l.redniBroj}", style = MaterialTheme.typography.labelMedium, color = Siva)
                            Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = Siva)
                            Text(l.naslov, style = MaterialTheme.typography.labelMedium, color = Tirkiz2)
                        }
                    } ?: Text("Ponavljanje", style = MaterialTheme.typography.labelMedium, color = Tirkiz2)
                    Spacer(Modifier.height(8.dp))
                    Text(pit.tekst, style = MaterialTheme.typography.headlineSmall, color = Tinta)
                    Spacer(Modifier.height(22.dp))
                    when (pit.tip) {
                        TipPitanja.ABCD -> OpcijeAbcd(pit, state, naOdgovor)
                        TipPitanja.TACNO_NETACNO -> OpcijeTn(pit, state, naOdgovor)
                        TipPitanja.DOPUNI -> UnosDopuni(state, naUnos, naOdgovor)
                    }
                    Spacer(Modifier.height(220.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = state.provjereno,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(stiffness = 420f, dampingRatio = 0.82f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180))
        ) {
            FeedbackPanel(p, state, naNastavi)
        }
    }
}

@Composable
private fun GornjaTraka(state: KvizUiState, naZatvori: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .clickable(onClick = naZatvori),
            contentAlignment = Alignment.Center
        ) { Text("✕", color = Siva, style = MaterialTheme.typography.titleMedium) }

        Spacer(Modifier.width(12.dp))

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            state.rezultati.forEachIndexed { i, r ->
                val boja = when {
                    r == true -> Tirkiz2
                    r == false -> Ne
                    i == state.index -> Safran
                    else -> Tackica
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(boja)
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        val xp = animiraniBroj(state.xp, 500)
        Box(
            Modifier
                .background(Noc, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) { Text("+$xp XP", style = MaterialTheme.typography.labelMedium, color = Safran, fontWeight = FontWeight.Bold) }
    }
}

// ---- opcije ----

private enum class StanjeOpcije { NEUTRALNO, TACNO, NETACNO, PRIGUSENO }

private fun stanjeOpcije(kljuc: String, tacanKljuc: String, state: KvizUiState): StanjeOpcije {
    if (!state.provjereno) return StanjeOpcije.NEUTRALNO
    val jeTacna = kljuc.equals(tacanKljuc.trim(), ignoreCase = true)
    val jeOdabrana = kljuc.equals(state.odgovor, ignoreCase = true)
    return when {
        jeTacna -> StanjeOpcije.TACNO
        jeOdabrana -> StanjeOpcije.NETACNO
        else -> StanjeOpcije.PRIGUSENO
    }
}

@Composable
private fun OpcijeAbcd(p: Pitanje, state: KvizUiState, naOdgovor: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        p.opcije.forEachIndexed { i, tekst ->
            val slovo = "ABCD"[i].toString()
            Opcija(slovo, tekst, stanjeOpcije(slovo, p.tacno, state)) { naOdgovor(slovo) }
        }
    }
}

@Composable
private fun OpcijeTn(p: Pitanje, state: KvizUiState, naOdgovor: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Opcija("✓", "Tačno", stanjeOpcije("T", p.tacno, state)) { naOdgovor("T") }
        Opcija("✕", "Netačno", stanjeOpcije("N", p.tacno, state)) { naOdgovor("N") }
    }
}

@Composable
private fun Opcija(oznaka: String, tekst: String, stanje: StanjeOpcije, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pozadina = when (stanje) {
        StanjeOpcije.TACNO -> OkBg
        StanjeOpcije.NETACNO -> NeBg
        else -> Color.White
    }
    val okvir = when (stanje) {
        StanjeOpcije.TACNO -> Ok
        StanjeOpcije.NETACNO -> Ne
        else -> Color.Transparent
    }
    val zvijezda = when (stanje) {
        StanjeOpcije.TACNO -> Ok
        StanjeOpcije.NETACNO -> Ne
        else -> Safran
    }
    val alfa by animateFloatAsState(if (stanje == StanjeOpcije.PRIGUSENO) 0.45f else 1f, tween(300), label = "alfa")

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = alfa }
            .tresenje(stanje == StanjeOpcije.NETACNO)
            .pritisak(interaction)
            .shadow(if (stanje == StanjeOpcije.NEUTRALNO) 6.dp else 0.dp, RoundedCornerShape(18.dp), ambientColor = Tinta.copy(alpha = 0.2f), spotColor = Tinta.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(18.dp))
            .background(pozadina)
            .border(2.dp, okvir, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = stanje == StanjeOpcije.NEUTRALNO, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(zvijezda, ZvijezdaShape), contentAlignment = Alignment.Center) {
            Text(
                oznaka,
                color = if (stanje == StanjeOpcije.NEUTRALNO || stanje == StanjeOpcije.PRIGUSENO) Noc else Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            tekst,
            style = MaterialTheme.typography.bodyLarge,
            color = Tinta,
            fontWeight = if (stanje == StanjeOpcije.TACNO) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UnosDopuni(state: KvizUiState, naUnos: (String) -> Unit, naOdgovor: (String) -> Unit) {
    val boja = when {
        !state.provjereno -> Safran
        state.tacno -> Ok
        else -> Ne
    }
    Column(Modifier.tresenje(state.provjereno && !state.tacno)) {
        OutlinedTextField(
            value = state.unos,
            onValueChange = naUnos,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.provjereno,
            placeholder = { Text("Upiši odgovor…", color = Siva) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = boja, unfocusedBorderColor = Linija, disabledBorderColor = boja,
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                disabledContainerColor = if (state.tacno) OkBg else NeBg,
                disabledTextColor = Tinta, focusedTextColor = Tinta, unfocusedTextColor = Tinta, cursorColor = Noc
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { naOdgovor(state.unos) })
        )
        Spacer(Modifier.height(12.dp))
        if (!state.provjereno) {
            Dugme("Provjeri", omoguceno = state.unos.isNotBlank(), pozadina = Noc, tekstBoja = Color.White) { naOdgovor(state.unos) }
            Spacer(Modifier.height(8.dp))
            Text("Velika i mala slova i kvačice ne utiču na provjeru.", style = MaterialTheme.typography.bodySmall, color = Siva)
        }
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
private fun FeedbackPanel(p: Pitanje, state: KvizUiState, naNastavi: () -> Unit) {
    val boja = if (state.tacno) Ok else Ne
    val zadnje = state.index + 1 >= state.ukupno
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), shadowElevation = 20.dp) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(boja, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (state.tacno) "✓" else "✕", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (state.tacno) (if (state.uzastopnoTacnih >= 3) "Tačno! Niz od ${state.uzastopnoTacnih}" else "Tačno!") else "Nije tačno",
                    style = MaterialTheme.typography.titleLarge,
                    color = boja
                )
            }
            if (!state.tacno) {
                Spacer(Modifier.height(10.dp))
                Text("Tačan odgovor", style = MaterialTheme.typography.labelMedium, color = Siva)
                Text(ProvjeraOdgovora.tekstTacnog(p), style = MaterialTheme.typography.titleMedium, color = Tinta)
            }
            if (!p.objasnjenje.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(p.objasnjenje, style = MaterialTheme.typography.bodyMedium, color = Tinta)
            }
            state.lekcija?.let { l ->
                if (!state.tacno) {
                    Spacer(Modifier.height(6.dp))
                    Text("Lekcija ${l.redniBroj} · ${l.naslov}", style = MaterialTheme.typography.labelMedium, color = Tirkiz2)
                }
            }
            Spacer(Modifier.height(14.dp))
            Dugme(if (zadnje) "Vidi rezultat" else "Nastavi", pozadina = Noc, tekstBoja = Color.White, naKlik = naNastavi)
        }
    }
}

// ============================================================================
//  REZULTAT — noć
// ============================================================================

@Composable
private fun RezultatEkran(state: KvizUiState, naPocetna: () -> Unit, naSljedecu: (String) -> Unit) {
    val r = state.rezultat
    val lekcija = state.lekcija
    val zv = r.zvjezdice
    val naslov = when {
        lekcija == null -> "Ponavljanje završeno"
        zv == 3 -> "Tri zvjezdice!"
        zv == 2 -> "Dvije zvjezdice!"
        zv == 1 -> "Prva zvjezdica!"
        else -> "Dobar početak"
    }
    val pod = when {
        lekcija == null -> "${state.tacnih} od ${state.ukupno} tačno"
        zv == 3 -> "${lekcija.naslov} je savladana"
        else -> "${lekcija.naslov} · ${state.tacnih} od ${state.ukupno} tačno"
    }
    val proslava = zv > r.zvjezdicePrije || (lekcija == null && state.tacnih * 100 / state.ukupno.coerceAtLeast(1) >= 80)
    val tacnih = animiraniBroj(state.tacnih, 900)
    val xp = animiraniBroj(state.xp, 1100)

    Box(Modifier.fillMaxSize().background(PozadinaRezultat)) {
        Ornament()
        if (proslava) Konfete()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(190.dp).background(Safran.copy(alpha = 0.25f), OsmokrakaShape))
                Box(Modifier.size(170.dp).background(Safran, OsmokrakaShape))
                Box(Modifier.size(118.dp).background(Noc, OsmokrakaShape))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$tacnih", style = MaterialTheme.typography.displayMedium, color = Safran)
                    Text("od ${state.ukupno}", style = MaterialTheme.typography.labelMedium, color = NocTekst2)
                }
            }

            Spacer(Modifier.height(18.dp))
            if (lekcija != null) {
                Text("★".repeat(zv) + "☆".repeat(3 - zv), style = MaterialTheme.typography.headlineMedium, color = Safran)
            }
            Text(naslov, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Text(pod, style = MaterialTheme.typography.bodyMedium, color = NocTekst, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("+$xp", "XP", Modifier.weight(1f))
                Stat("🔥 ${r.streak}", if (r.streak == 1) "dan u nizu" else "dana u nizu", Modifier.weight(1f))
                Stat("${r.novihNaucenih}", if (r.novihNaucenih == 1) "novo naučeno" else "novih naučenih", Modifier.weight(1f))
            }

            if (r.zaSutra > 0 || r.sljedecaLekcija != null) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Tirkiz.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .border(1.dp, Tirkiz.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Sutra te čeka", style = MaterialTheme.typography.titleSmall, color = Tirkiz)
                    Text(
                        listOfNotNull(
                            r.zaSutra.takeIf { it > 0 }?.let { "$it ${if (it == 1) "pitanje" else "pitanja"} za ponavljanje" },
                            r.sljedecaLekcija?.let { "Lekcija ${it.redniBroj}: ${it.naslov}" }
                        ).joinToString(" + "),
                        style = MaterialTheme.typography.bodySmall,
                        color = NocTekst2
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            val sljedeca = r.sljedecaLekcija
            if (sljedeca != null && zv >= 1) {
                Dugme("Nastavi — ${sljedeca.naslov}", pozadina = Safran, tekstBoja = Noc) { naSljedecu(sljedeca.id) }
                Spacer(Modifier.height(10.dp))
                Dugme("Na stazu", pozadina = Color.White.copy(alpha = 0.1f), tekstBoja = Color.White, naKlik = naPocetna)
            } else {
                Dugme("Na stazu", pozadina = Safran, tekstBoja = Noc, naKlik = naPocetna)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Stat(vrijednost: String, naziv: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(vrijednost, style = MaterialTheme.typography.titleLarge, color = Safran)
        Text(naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PraznoStanje(naIzlaz: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Krem)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nema pitanja za ponavljanje.", style = MaterialTheme.typography.titleMedium, color = Tinta, textAlign = TextAlign.Center)
        Text("Odradi lekciju pa se vrati sutra.", style = MaterialTheme.typography.bodyMedium, color = Siva, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        Dugme("Na stazu", pozadina = Noc, tekstBoja = Color.White, naKlik = naIzlaz)
    }
}
