package ba.osgrivice.kviz.ui.screens.profil

import androidx.compose.foundation.background
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.kviz.Bedz
import ba.osgrivice.kviz.domain.kviz.Bedzevi
import ba.osgrivice.kviz.ui.components.BedzIkona
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.theme.Ne
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape
import ba.osgrivice.kviz.platform.formatDatum
import ba.osgrivice.kviz.platform.formatLokalniDatum
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(naNazad: () -> Unit, naAdmin: () -> Unit, naUputstvo: () -> Unit) {
    val app = KvizApp
    val vm: ProfilViewModel = viewModel(factory = ProfilViewModel.factory(app.container))
    val s by vm.state.collectAsStateWithLifecycle()

    var urediIme by remember { mutableStateOf(false) }
    var urediCilj by remember { mutableStateOf(false) }
    var urediSkolu by remember { mutableStateOf(false) }
    var urediDatum by remember { mutableStateOf(false) }
    var potvrdiReset by remember { mutableStateOf(false) }
    var detaljBedza by remember { mutableStateOf<Bedz?>(null) }
    var tapoviVerzije by remember { mutableIntStateOf(0) }

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
                    Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .clickable(onClick = naNazad),
                    contentAlignment = Alignment.Center
                ) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.width(12.dp))
                Text("Profil", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Spacer(Modifier.height(22.dp))

            // Ime + nivo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).background(Safran, ZvijezdaShape), contentAlignment = Alignment.Center) {
                    Text(s.nivo.toString(), style = MaterialTheme.typography.headlineSmall, color = Noc)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.ime.ifBlank { "Učenik" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("${s.razred}. razred · nivo ${s.nivo}", style = MaterialTheme.typography.bodyMedium, color = NocTekst)
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
                                .fillMaxWidth(s.xpUNivou / 500f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Safran)
                        )
                    }
                    Text("${s.xpUNivou} / 500 XP do sljedećeg nivoa", style = MaterialTheme.typography.labelSmall, color = NocTekst, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${s.xp}", "XP ukupno", Modifier.weight(1f))
                Stat("🔥 ${s.streak}", "dana u nizu", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${s.savladano}/${s.ukupnoPitanja}", "savladano", Modifier.weight(1f))
                Stat("${s.procenatTacnih}%", "tačnih od ${s.odgovora}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            val osvojeniIds = remember(s.bedzevi) { s.bedzevi.map { it.bedz.id }.toSet() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bedževi", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                Text("${osvojeniIds.size} / ${Bedzevi.SVI.size}", style = MaterialTheme.typography.labelLarge, color = Safran)
            }
            Spacer(Modifier.height(10.dp))
            Bedzevi.SVI.chunked(3).forEach { red ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    red.forEach { b -> BedzCelija(b, osvojen = b.id in osvojeniIds, Modifier.weight(1f)) { detaljBedza = b } }
                    repeat(3 - red.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Postavke", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Postavka("Ime i razred", "${s.ime.ifBlank { "—" }} · ${s.razred}. razred") { urediIme = true }
            Postavka("Škola", s.skola.ifBlank { "Nije upisana" }) { urediSkolu = true }
            Postavka("Dnevni cilj", "${s.dnevniCilj} pitanja dnevno") { urediCilj = true }
            Postavka(
                "Datum takmičenja",
                s.takmicenje?.let { formatLokalniDatum(it) } ?: "1. mart (podrazumijevano)"
            ) { urediDatum = true }
            Postavka("Kako se igra", "Kratko uputstvo za kviz") { naUputstvo() }

            Spacer(Modifier.height(28.dp))
            Postavka("Resetuj napredak", "Briše sve odgovore, zvjezdice i niz dana", boja = Ne) { potvrdiReset = true }
            Spacer(Modifier.height(18.dp))
            // Skriveni ulaz u nastavnički pregled: 5 uzastopnih dodira na verziju.
            Text(
                "Vjeronauka Kviz · verzija ${app.container.verzija}",
                style = MaterialTheme.typography.labelSmall,
                color = NocTekst,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        tapoviVerzije++
                        if (tapoviVerzije >= 5) { tapoviVerzije = 0; naAdmin() }
                    }
                    .padding(8.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (urediIme) {
        var ime by remember { mutableStateOf(s.ime) }
        var razred by remember { mutableIntStateOf(s.razred) }
        AlertDialog(
            onDismissRequest = { urediIme = false },
            title = { Text("Ime i razred") },
            text = {
                Column {
                    OutlinedTextField(value = ime, onValueChange = { if (it.length <= 24) ime = it }, singleLine = true, label = { Text("Ime") })
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(8, 9).forEach { r ->
                            val od = razred == r
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (od) Noc else Color(0xFFEDEBE5))
                                    .clickable { razred = r },
                                contentAlignment = Alignment.Center
                            ) { Text("$r. razred", color = if (od) Color.White else Noc, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text(
                        "Promjena razreda ne briše napredak — svaki razred ima svoju stazu.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = ime.trim().length >= 2, onClick = { vm.sacuvajImeIRazred(ime, razred); urediIme = false }) { Text("Sačuvaj") }
            },
            dismissButton = { TextButton(onClick = { urediIme = false }) { Text("Odustani") } }
        )
    }

    if (urediSkolu) {
        var skola by remember { mutableStateOf(s.skola) }
        AlertDialog(
            onDismissRequest = { urediSkolu = false },
            title = { Text("Škola") },
            text = {
                Column {
                    OutlinedTextField(value = skola, onValueChange = { if (it.length <= 40) skola = it }, singleLine = true, label = { Text("Naziv škole") })
                    Text("Prikazuje se na rang-listi uz tvoje ime.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { vm.postaviSkolu(skola); urediSkolu = false }) { Text("Sačuvaj") } },
            dismissButton = { TextButton(onClick = { urediSkolu = false }) { Text("Odustani") } }
        )
    }

    if (urediCilj) {
        AlertDialog(
            onDismissRequest = { urediCilj = false },
            title = { Text("Dnevni cilj") },
            text = {
                Column {
                    Text("Koliko pitanja dnevno?")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 20, 30).forEach { c ->
                            val od = s.dnevniCilj == c
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (od) Noc else Color(0xFFEDEBE5))
                                    .clickable { vm.postaviCilj(c); urediCilj = false },
                                contentAlignment = Alignment.Center
                            ) { Text("$c", color = if (od) Color.White else Noc, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { urediCilj = false }) { Text("Zatvori") } }
        )
    }

    if (urediDatum) {
        val pocetni = (s.takmicenje ?: ba.osgrivice.kviz.platform.danasLokalno().plus(1, DateTimeUnit.MONTH)).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val stanje = rememberDatePickerState(initialSelectedDateMillis = pocetni)
        DatePickerDialog(
            onDismissRequest = { urediDatum = false },
            confirmButton = {
                TextButton(onClick = {
                    stanje.selectedDateMillis?.let { ms ->
                        vm.postaviTakmicenje(Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date)
                    }
                    urediDatum = false
                }) { Text("Sačuvaj") }
            },
            dismissButton = { TextButton(onClick = { urediDatum = false }) { Text("Odustani") } }
        ) { DatePicker(state = stanje) }
    }

    detaljBedza?.let { b ->
        val osvojen = s.bedzevi.firstOrNull { it.bedz.id == b.id }
        AlertDialog(
            onDismissRequest = { detaljBedza = null },
            icon = { BedzIkona(b, osvojen = osvojen != null, velicina = 72.dp) },
            title = { Text(b.naziv, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text(
                    if (osvojen != null) "${b.opis}\n\nOsvojen ${formatDatum(osvojen.dodijeljenMs)}"
                    else "${b.opis}\n\nJoš nije osvojen.",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { detaljBedza = null }) { Text("Zatvori") } }
        )
    }

    if (potvrdiReset) {
        AlertDialog(
            onDismissRequest = { potvrdiReset = false },
            title = { Text("Resetovati napredak?") },
            text = { Text("Brišu se svi odgovori, zvjezdice, XP i niz dana. Ovo se ne može vratiti.") },
            confirmButton = { TextButton(onClick = { vm.resetujNapredak(); potvrdiReset = false }) { Text("Resetuj", color = Ne) } },
            dismissButton = { TextButton(onClick = { potvrdiReset = false }) { Text("Odustani") } }
        )
    }
}

@Composable
private fun Stat(vrijednost: String, naziv: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(vrijednost, style = MaterialTheme.typography.titleLarge, color = Safran, textAlign = TextAlign.Center)
        Text(naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst)
    }
}

@Composable
private fun BedzCelija(b: Bedz, osvojen: Boolean, modifier: Modifier = Modifier, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .pritisak(interaction)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = if (osvojen) 0.1f else 0.05f))
            .clickable(interactionSource = interaction, indication = null, onClick = naKlik)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BedzIkona(b, osvojen = osvojen, velicina = 56.dp)
        Text(
            b.naziv,
            style = MaterialTheme.typography.labelSmall,
            color = if (osvojen) Color.White else NocTekst,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun Postavka(naslov: String, vrijednost: String, boja: Color = Color.White, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .pritisak(interaction)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(interactionSource = interaction, indication = null, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(naslov, style = MaterialTheme.typography.titleSmall, color = boja)
            Text(vrijednost, style = MaterialTheme.typography.bodySmall, color = NocTekst2)
        }
        Text("›", color = if (boja == Ne) Ne else Tirkiz, style = MaterialTheme.typography.titleLarge)
    }
}
