package ba.osgrivice.kviz.ui.screens.admin

import ba.osgrivice.kviz.platform.sadaMs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.data.remote.UcenikPregled
import ba.osgrivice.kviz.domain.kviz.Takmicenje
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
import ba.osgrivice.kviz.platform.podijeliFajl

@Composable
fun AdminScreen(naNazad: () -> Unit, naUcenik: (uid: String, ime: String, razred: Int) -> Unit) {
    val app = KvizApp
    val vm: AdminViewModel = viewModel(factory = AdminViewModel.factory(app.container))
    val s by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(PozadinaNoc)) {
        Ornament()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Zaglavlje(
                naslov = "Nastavnički pregled",
                pod = when (s.faza) {
                    AdminFaza.LISTA -> "${s.razred}. razred · ${s.filtrirani.size} učenika"
                    else -> "samo za nastavnike"
                },
                naNazad = naNazad,
                desno = {
                    if (s.faza == AdminFaza.LISTA) {
                        Ikonica("⇪") {
                            runCatching { podijeliFajl(vm.csv(), "ucenici_${s.razred}_razred.csv", "text/csv", "Izvoz učenika") }
                        }
                        Spacer(Modifier.width(8.dp))
                        Ikonica("🔒", naKlik = vm::zakljucaj)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            when (s.faza) {
                AdminFaza.PROVJERA, AdminFaza.UCITAVA ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Safran) }
                AdminFaza.PIN -> PinEkran(s, vm::promijeniPin, vm::potvrdiPin)
                AdminFaza.NEMA_PRISTUPA -> NemaPristupa(s.mojUid, naPonovo = vm::ucitaj, naZakljucaj = vm::zakljucaj)
                AdminFaza.GRESKA -> Poruka(s.greska, naPonovo = vm::ucitaj)
                AdminFaza.LISTA -> Lista(s, vm, naUcenik)
            }
        }
    }
}

// ---------------------------------------------------------------- PIN

@Composable
private fun PinEkran(s: AdminUiState, naUnos: (String) -> Unit, naPotvrdi: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).background(Safran, ZvijezdaShape), contentAlignment = Alignment.Center) {
            Text("🔑", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(18.dp))
        Text("Unesi nastavnički PIN", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text("PIN se mijenja u Firestoreu: config/admin → pin", style = MaterialTheme.typography.bodySmall, color = NocTekst, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = s.pinUnos,
            onValueChange = naUnos,
            singleLine = true,
            isError = s.pinGreska,
            visualTransformation = PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { naPotvrdi() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Safran, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), errorBorderColor = Ne,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, errorTextColor = Color.White, cursorColor = Safran
            ),
            modifier = Modifier.width(200.dp)
        )
        if (s.pinGreska) Text("Pogrešan PIN", color = Ne, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
        Dugme("Otključaj", omoguceno = s.pinUnos.isNotBlank(), naKlik = naPotvrdi)
    }
}

// ---------------------------------------------------------------- nema pristupa

@Composable
private fun NemaPristupa(uid: String, naPonovo: () -> Unit, naZakljucaj: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Firestore ne dozvoljava čitanje", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
        Text(
            "PIN je tačan, ali ovaj telefon nije na listi nastavnika.\nU Firebase konzoli otvori dokument config/admin i u polje uids (array) dodaj ovaj ID:",
            style = MaterialTheme.typography.bodyMedium, color = NocTekst, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(uid.ifBlank { "— nema prijave (internet?) —" }, style = MaterialTheme.typography.bodyMedium, color = Safran, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Dugme("Kopiraj ID", omoguceno = uid.isNotBlank()) { clipboard.setText(AnnotatedString(uid)) }
        }
        Spacer(Modifier.height(16.dp))
        Dugme("Pokušaj ponovo", naKlik = naPonovo)
        Spacer(Modifier.height(8.dp))
        Text("Zaključaj", color = NocTekst, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = naZakljucaj).padding(8.dp))
    }
}

@Composable
private fun Poruka(tekst: String, naPonovo: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tekst, color = NocTekst, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Dugme("Pokušaj ponovo", naKlik = naPonovo)
    }
}

// ---------------------------------------------------------------- lista

@Composable
private fun Lista(s: AdminUiState, vm: AdminViewModel, naUcenik: (String, String, Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // razred + škola
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 9).forEach { r -> Cip("$r. razred", s.razred == r) { vm.postaviRazred(r) } }
            if (s.skole.isNotEmpty()) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cip("Sve škole", s.skola.isBlank()) { vm.postaviSkolu("") }
                    s.skole.forEach { sk -> Cip(sk, s.skola == sk) { vm.postaviSkolu(sk) } }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // sortiranje
        Row(Modifier.padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sortiraj:", style = MaterialTheme.typography.labelMedium, color = NocTekst, modifier = Modifier.align(Alignment.CenterVertically))
            Sortiranje.entries.forEach { so -> Cip(so.naziv, s.sortiranje == so, mali = true) { vm.postaviSortiranje(so) } }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = s.pretraga,
            onValueChange = vm::postaviPretragu,
            singleLine = true,
            placeholder = { Text("Traži po imenu…", color = NocTekst) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Safran, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Safran
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SazetakKartica(s.sazetak) }
            if (s.filtrirani.isEmpty()) {
                item { Text("Nema učenika za ovaj izbor.", color = NocTekst, modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
            }
            items(s.filtrirani, key = { it.uid }) { u -> UcenikRed(u) { naUcenik(u.uid, u.ime, u.razred) } }
        }
    }
}

@Composable
private fun SazetakKartica(z: SazetakRazreda) {
    Column(Modifier.fillMaxWidth().background(Safran.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("Sažetak", style = MaterialTheme.typography.titleSmall, color = Safran)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${z.ukupno}", "učenika", Modifier.weight(1f))
            Stat("${z.aktivnih7}", "aktivno 7 dana", Modifier.weight(1f))
            Stat("${z.aktivnihDanas}", "aktivno danas", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${z.prosjekXp}", "prosjek XP", Modifier.weight(1f))
            Stat("${z.prosjekSavladano}", "prosj. savladano", Modifier.weight(1f))
            Stat(if (z.saSimulacijom == 0) "—" else "${z.prosjekSimulacije} %", "simulacija (${z.saSimulacijom})", Modifier.weight(1f))
        }
    }
}

@Composable
private fun UcenikRed(u: UcenikPregled, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val aktivanNedavno = (u.zadnjaAktivnostMs ?: 0L) >= sadaMs() - 7L * 24 * 3600 * 1000
    Row(
        Modifier
            .fillMaxWidth()
            .pritisak(interaction)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(interactionSource = interaction, indication = null, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(if (aktivanNedavno) Tirkiz else Color.White.copy(alpha = 0.2f), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(u.ime, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
            Text(
                listOfNotNull(u.skola.takeIf { it.isNotBlank() }, relativno(u.zadnjaAktivnostMs)).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = NocTekst, maxLines = 1
            )
            Text(
                "savladano ${u.savladano} · tačnost ${u.procenatTacnih} % · 🔥 ${u.streak} · bedževa ${u.brojBedzeva}",
                style = MaterialTheme.typography.labelSmall, color = NocTekst2, maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${u.xp} XP", style = MaterialTheme.typography.titleSmall, color = Safran)
            Text(
                u.simTacnih?.let { "🏆 $it/${u.simUkupno ?: Takmicenje.BROJ_PITANJA}" } ?: "🏆 —",
                style = MaterialTheme.typography.labelSmall, color = Tirkiz
            )
        }
    }
}

// ---------------------------------------------------------------- zajedničko

@Composable
internal fun Zaglavlje(naslov: String, pod: String, naNazad: () -> Unit, desno: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Ikonica("‹", naKlik = naNazad)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(naslov, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
            Text(pod, style = MaterialTheme.typography.bodySmall, color = NocTekst, maxLines = 1)
        }
        desno()
    }
}

@Composable
internal fun Ikonica(znak: String, naKlik: () -> Unit) {
    Box(
        Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).clickable(onClick = naKlik),
        contentAlignment = Alignment.Center
    ) { Text(znak, color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

@Composable
internal fun Cip(tekst: String, aktivan: Boolean, mali: Boolean = false, naKlik: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (aktivan) Safran else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = naKlik)
            .padding(horizontal = if (mali) 10.dp else 14.dp, vertical = if (mali) 6.dp else 8.dp)
    ) {
        Text(
            tekst,
            style = if (mali) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = if (aktivan) Noc else Color.White,
            maxLines = 1
        )
    }
}

@Composable
internal fun Stat(vrijednost: String, naziv: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(vrijednost, style = MaterialTheme.typography.titleMedium, color = Safran, maxLines = 1)
        Text(naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
internal fun Dugme(tekst: String, omoguceno: Boolean = true, naKlik: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (omoguceno) Safran else Color.White.copy(alpha = 0.12f))
            .clickable(enabled = omoguceno, onClick = naKlik),
        contentAlignment = Alignment.Center
    ) { Text(tekst, style = MaterialTheme.typography.titleMedium, color = if (omoguceno) Noc else NocTekst, fontWeight = FontWeight.ExtraBold) }
}

// iOS: podijeliCsv → platform.podijeliFajl (UIActivityViewController — "Podijeli" prozor)
