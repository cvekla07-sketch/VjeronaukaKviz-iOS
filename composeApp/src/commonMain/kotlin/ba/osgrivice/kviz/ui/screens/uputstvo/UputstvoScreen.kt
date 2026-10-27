package ba.osgrivice.kviz.ui.screens.uputstvo

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran

private data class Stavka(val ikona: String, val naslov: String, val tekst: String)

private val STAVKE = listOf(
    Stavka("🗺", "Staza", "Nivoi idu redom. Pređi nivo i otvara se sljedeći."),
    Stavka("📖", "Prije kviza", "Otvori nivo i vidi sva pitanja s tačnim odgovorima. Pročitaj, pa klikni „Kreni na kviz“."),
    Stavka("🔁", "Ponavljanje", "Pitanja se vraćaju dok ih ne naučiš. Pogrešno odgovoreno dolazi odmah u sljedećoj vježbi. Tačno odgovoreno se vraća sutra, pa za 3, 7, 14 i 30 dana. Tako znanje ostaje do takmičenja."),
    Stavka("★", "Zvjezdice", "Nivo dobija zvjezdice kad pitanja odgovoriš tačno više puta, u različite dane. Za 3 zvjezdice moraš znati sva pitanja nivoa."),
    Stavka("✍️", "Dopuni", "Prvi put slažeš ponuđene odgovore dodirom. Kad pitanje jednom tačno uradiš, sljedeći put ga pišeš — kao na testu. Svaki tačan dio se boduje. Kvačice i velika/mala slova se ne računaju."),
    Stavka("🔥", "Svaki dan", "Odgovori bar 10 pitanja dnevno da ti vatrica raste."),
    Stavka("🏆", "Simulacija", "Pravi test kao na takmičenju: 20 pitanja, 20 sekundi po pitanju, bez gledanja. Najbolji rezultat ide na rang-listu.")
)

/**
 * „Kako se igra“ — kratko uputstvo za učenike.
 * Prikazuje se jednom nakon onboardinga (dugme „Kreni“) i iz profila (dugme „Razumijem“).
 */
@Composable
fun UputstvoScreen(prviPut: Boolean, naDalje: () -> Unit) {
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
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!prviPut) {
                    Box(
                        Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).clickable(onClick = naDalje),
                        contentAlignment = Alignment.Center
                    ) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text("Kako se igra", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Sedam stvari koje trebaš znati", style = MaterialTheme.typography.bodyMedium, color = NocTekst)
                }
            }
            Spacer(Modifier.height(22.dp))

            STAVKE.forEachIndexed { i, s ->
                Kartica(i + 1, s)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Safran)
                    .clickable(onClick = naDalje),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (prviPut) "Kreni" else "Razumijem",
                    style = MaterialTheme.typography.titleMedium,
                    color = Noc,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Kartica(redni: Int, s: Stavka) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(s.ikona, style = MaterialTheme.typography.titleLarge) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$redni.", style = MaterialTheme.typography.titleSmall, color = Safran, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(6.dp))
                Text(s.naslov, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(s.tekst, style = MaterialTheme.typography.bodyMedium, color = NocTekst2)
        }
    }
}
