package ba.osgrivice.kviz.ui.screens.kviz

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.domain.kviz.Dopuna
import ba.osgrivice.kviz.domain.kviz.FazaDopune
import ba.osgrivice.kviz.domain.model.Pitanje
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.components.tresenje
import ba.osgrivice.kviz.ui.theme.Linija
import ba.osgrivice.kviz.ui.theme.Ne
import ba.osgrivice.kviz.ui.theme.NeBg
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.Ok
import ba.osgrivice.kviz.ui.theme.OkBg
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Siva
import ba.osgrivice.kviz.ui.theme.Tackica
import ba.osgrivice.kviz.ui.theme.Tinta

/** Akcije ekrana za "Dopuni" — vezane na KvizViewModel. */
class DopunaAkcije(
    val naKarticu: (Int) -> Unit,
    val naPrazninu: (Int) -> Unit,
    val naUnos: (Int, String) -> Unit,
    val naProvjeri: () -> Unit
)

/** Oznaka praznine: "Amin", "1.", "(1)". */
private fun oznaka(d: DopunaUi, i: Int): String =
    d.struktura.dijelovi[i].oznaka ?: if (d.struktura.uTekstu) "(${i + 1})" else "${i + 1}."

private fun bojaPraznine(d: DopunaUi, i: Int, aktivna: Boolean): Color = when {
    d.ocjena != null -> if (d.ocjena.poPraznini.getOrElse(i) { false }) Ok else Ne
    aktivna -> Safran
    else -> Linija
}

// ============================================================================
//  Tekst pitanja s prazninama u samom tekstu
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TekstSaPrazninama(p: Pitanje, d: DopunaUi, akcije: DopunaAkcije) {
    val dijelovi = Dopuna.tekstOkoPraznina(p.tekst)
    val prvaPrazna = d.odabrane.indexOfFirst { it == null }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dijelovi.forEachIndexed { i, segment ->
            segment.split(" ").filter { it.isNotEmpty() }.forEach { rijec ->
                Text(rijec, style = MaterialTheme.typography.titleLarge, color = Tinta, modifier = Modifier.padding(top = 4.dp))
            }
            if (i < dijelovi.lastIndex) {
                if (d.faza == FazaDopune.UCENJE) {
                    val vrijednost = d.vrijednosti.getOrNull(i)
                    PrazninaKartica(
                        tekst = vrijednost,
                        boja = bojaPraznine(d, i, aktivna = i == prvaPrazna),
                        naKlik = if (vrijednost != null && d.ocjena == null) ({ akcije.naPrazninu(i) }) else null
                    )
                } else {
                    // faza pisanja: u tekstu samo broj praznine, polja su ispod
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bojaPraznine(d, i, aktivna = false).copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("(${i + 1})", style = MaterialTheme.typography.titleMedium, color = Tinta, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun PrazninaKartica(tekst: String?, boja: Color, naKlik: (() -> Unit)?, modifier: Modifier = Modifier) {
    val okvir by animateColorAsState(boja, label = "okvir")
    Box(
        modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (tekst == null) Tackica.copy(alpha = 0.35f) else Color.White)
            .border(2.dp, okvir, RoundedCornerShape(10.dp))
            .then(if (naKlik != null) Modifier.clickable(onClick = naKlik) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(tekst ?: "", style = MaterialTheme.typography.titleMedium, color = Tinta)
    }
}

// ============================================================================
//  Unos ispod pitanja: kartice (učenje) ili polja (pisanje)
// ============================================================================

@Composable
internal fun DopunaUnos(state: KvizUiState, d: DopunaUi, akcije: DopunaAkcije) {
    Column(Modifier.tresenje(state.provjereno && !state.tacno)) {
        when (d.faza) {
            FazaDopune.UCENJE -> UcenjeKartice(d, akcije)
            FazaDopune.PISANJE -> PisanjePolja(d, akcije)
        }
        Spacer(Modifier.height(14.dp))
        if (!state.provjereno) {
            Dugme("Provjeri", omoguceno = d.spremno, pozadina = Noc, tekstBoja = Color.White, naKlik = akcije.naProvjeri)
            Spacer(Modifier.height(8.dp))
            Text(
                if (d.faza == FazaDopune.UCENJE)
                    "Dodirni karticu da je staviš u prazno polje. Dodirni popunjeno polje da je vratiš."
                else
                    "Napiši odgovor u svako polje. Velika i mala slova, kvačice i sitne greške u kucanju ne utiču na provjeru.",
                style = MaterialTheme.typography.bodySmall,
                color = Siva
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UcenjeKartice(d: DopunaUi, akcije: DopunaAkcije) {
    // Praznine kao lista (kad nisu u tekstu pitanja)
    if (!d.struktura.uTekstu) {
        val prvaPrazna = d.odabrane.indexOfFirst { it == null }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.struktura.dijelovi.indices.forEach { i ->
                val vrijednost = d.vrijednosti.getOrNull(i)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (d.struktura.brojPraznina > 1) {
                        Text(
                            oznaka(d, i),
                            style = MaterialTheme.typography.titleSmall,
                            color = Siva,
                            fontWeight = FontWeight.Bold,
                            // ista širina za sve oznake → polja poravnata
                            modifier = Modifier.width(if (d.struktura.dijelovi.any { it.oznaka != null }) 112.dp else 28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    PrazninaKartica(
                        tekst = vrijednost,
                        boja = bojaPraznine(d, i, aktivna = i == prvaPrazna),
                        naKlik = if (vrijednost != null && d.ocjena == null) ({ akcije.naPrazninu(i) }) else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (d.ocjena == null) {
        Text("Ponuđeni odgovori", style = MaterialTheme.typography.labelMedium, color = Siva)
        Spacer(Modifier.height(8.dp))
        val dugacke = d.kartice.any { it.length > 28 }
        val iskoristene = d.iskoristeneKartice
        if (dugacke) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.kartice.forEachIndexed { k, t -> if (k !in iskoristene) Kartica(t, Modifier.fillMaxWidth()) { akcije.naKarticu(k) } }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.kartice.forEachIndexed { k, t -> if (k !in iskoristene) Kartica(t) { akcije.naKarticu(k) } }
            }
        }
    }
}

@Composable
private fun Kartica(tekst: String, modifier: Modifier = Modifier, naKlik: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pritisak(interaction)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, Noc.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = naKlik)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(tekst, style = MaterialTheme.typography.titleMedium, color = Tinta)
    }
}

@Composable
private fun PisanjePolja(d: DopunaUi, akcije: DopunaAkcije) {
    val fokus = LocalFocusManager.current
    val provjereno = d.ocjena != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        d.struktura.dijelovi.indices.forEach { i ->
            val tacnoPolje = d.ocjena?.poPraznini?.getOrNull(i) == true
            val boja = bojaPraznine(d, i, aktivna = true)
            val zadnje = i == d.struktura.dijelovi.lastIndex
            OutlinedTextField(
                value = d.unosi.getOrElse(i) { "" },
                onValueChange = { akcije.naUnos(i, it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !provjereno,
                label = if (d.struktura.brojPraznina > 1) ({ Text(oznaka(d, i)) }) else null,
                placeholder = { Text("Upiši odgovor…", color = Siva) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = boja, unfocusedBorderColor = Linija, disabledBorderColor = boja,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    disabledContainerColor = if (tacnoPolje) OkBg else NeBg,
                    disabledTextColor = Tinta, focusedTextColor = Tinta, unfocusedTextColor = Tinta, cursorColor = Noc,
                    focusedLabelColor = Noc, unfocusedLabelColor = Siva, disabledLabelColor = Tinta
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = if (zadnje) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { fokus.moveFocus(FocusDirection.Down) },
                    onDone = { fokus.clearFocus(); if (d.spremno) akcije.naProvjeri() }
                )
            )
        }
    }
}

// ============================================================================
//  Povratna informacija: "2 / 5 tačno" + tačni dijelovi
// ============================================================================

@Composable
internal fun DopunaTacniDijelovi(d: DopunaUi) {
    val ocjena = d.ocjena ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        d.struktura.dijelovi.forEachIndexed { i, dio ->
            val ok = ocjena.poPraznini.getOrElse(i) { false }
            Row {
                Text(if (ok) "✓ " else "✕ ", color = if (ok) Ok else Ne, fontWeight = FontWeight.ExtraBold)
                val pref = if (d.struktura.brojPraznina > 1) "${oznaka(d, i)} " else ""
                Text(
                    pref + (if (dio.oznaka != null) "— " else "") + dio.odgovor,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tinta
                )
            }
        }
    }
}
