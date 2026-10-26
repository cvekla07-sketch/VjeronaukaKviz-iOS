package ba.osgrivice.kviz.ui.screens.rang

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.data.remote.RangUnos
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape

private sealed interface Stanje {
    data object Ucitava : Stanje
    data class Lista(val unosi: List<RangUnos>, val mojUid: String?) : Stanje
    data object Greska : Stanje
}

@Composable
fun RangScreen(razred: Int, naNazad: () -> Unit) {
    val app = KvizApp
    val stanje by produceState<Stanje>(initialValue = Stanje.Ucitava) {
        val sync = app.container.firebaseSync
        sync.sinhronizuj()
        value = runCatching { Stanje.Lista(sync.rangLista(razred), sync.uid) }.getOrElse { Stanje.Greska }
    }

    Box(Modifier.fillMaxSize().background(PozadinaNoc)) {
        Ornament()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).clickable(onClick = naNazad),
                    contentAlignment = Alignment.Center
                ) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Rang-lista", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("$razred. razred · po XP", style = MaterialTheme.typography.bodySmall, color = NocTekst)
                }
            }
            Spacer(Modifier.height(16.dp))

            when (val st = stanje) {
                Stanje.Ucitava -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Safran) }
                Stanje.Greska -> Poruka("Rang-lista trenutno nije dostupna.\nProvjeri internet i pokušaj ponovo.")
                is Stanje.Lista -> if (st.unosi.isEmpty()) Poruka("Još niko nije na listi.\nOdradi lekciju i budi prvi!") else {
                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(st.unosi, key = { it.uid }) { u -> Red(u, moj = u.uid == st.mojUid) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Red(u: RangUnos, moj: Boolean) {
    val top = u.mjesto <= 3
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (moj) Safran.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).background(if (top) Safran else Color.White.copy(alpha = 0.12f), if (top) ZvijezdaShape else CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(u.mjesto.toString(), color = if (top) Noc else Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (moj) "${u.ime} (ti)" else u.ime, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
            if (u.skola.isNotBlank()) Text(u.skola, style = MaterialTheme.typography.labelSmall, color = NocTekst, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${u.xp} XP", style = MaterialTheme.typography.titleSmall, color = Safran)
            Text("🔥 ${u.streak}", style = MaterialTheme.typography.labelSmall, color = Tirkiz)
        }
    }
}

@Composable
private fun Poruka(tekst: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(tekst, color = NocTekst, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}
