package ba.osgrivice.kviz.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ba.osgrivice.kviz.domain.kviz.Bedz
import ba.osgrivice.kviz.domain.kviz.GrupaBedza
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.Noc3
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.NocTekst2
import ba.osgrivice.kviz.ui.theme.OsmokrakaShape
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Tirkiz

/** Boja bedža po grupi — šafran za stazu/odgovore, tirkiz za istrajnost, zlatnije za takmičenje. */
fun bojaBedza(grupa: GrupaBedza): Color = when (grupa) {
    GrupaBedza.STAZA -> Safran
    GrupaBedza.NIZ -> Tirkiz
    GrupaBedza.ODGOVORI -> Safran
    GrupaBedza.TAKMICENJE -> Color(0xFFFFD166)
}

/**
 * Osmokraka pločica bedža. Neosvojen = siva, prigušena.
 */
@Composable
fun BedzIkona(bedz: Bedz, osvojen: Boolean, velicina: Dp = 64.dp, modifier: Modifier = Modifier) {
    val boja = if (osvojen) bojaBedza(bedz.grupa) else Color.White.copy(alpha = 0.14f)
    Box(modifier.size(velicina), contentAlignment = Alignment.Center) {
        if (osvojen) Box(Modifier.size(velicina).background(boja.copy(alpha = 0.25f), OsmokrakaShape))
        Box(Modifier.size(velicina * 0.86f).background(boja, OsmokrakaShape))
        Box(Modifier.size(velicina * 0.6f).background(if (osvojen) Noc else Noc3.copy(alpha = 0.6f), OsmokrakaShape))
        Text(
            bedz.emoji,
            fontSize = (velicina.value * 0.32f).sp,
            modifier = Modifier.graphicsLayer { alpha = if (osvojen) 1f else 0.35f }
        )
    }
}

/**
 * Dodjela bedža preko cijelog ekrana — konfete, pločica koja "uskoči", naziv i uslov.
 * Za više bedževa pozivatelj prikazuje jedan po jedan (naDalje → sljedeći).
 */
@Composable
fun BedzDijalog(bedz: Bedz, preostalo: Int, naDalje: () -> Unit) {
    Dialog(onDismissRequest = naDalje, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var prikazan by remember(bedz.id) { mutableStateOf(false) }
        LaunchedEffect(bedz.id) { prikazan = true }
        val skala by animateFloatAsState(
            if (prikazan) 1f else 0.3f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "bedz"
        )
        val interaction = remember { MutableInteractionSource() }

        Box(
            Modifier
                .fillMaxSize()
                .background(Noc3.copy(alpha = 0.92f))
                .clickable(interactionSource = interaction, indication = null, onClick = naDalje),
            contentAlignment = Alignment.Center
        ) {
            Konfete()
            Column(
                Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Novi bedž!", style = MaterialTheme.typography.labelLarge, color = bojaBedza(bedz.grupa))
                Spacer(Modifier.height(18.dp))
                BedzIkona(
                    bedz, osvojen = true, velicina = 170.dp,
                    modifier = Modifier.graphicsLayer { scaleX = skala; scaleY = skala }
                )
                Spacer(Modifier.height(22.dp))
                Text(bedz.naziv, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                Text(bedz.opis, style = MaterialTheme.typography.bodyLarge, color = NocTekst2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Text(bedz.grupa.naziv, style = MaterialTheme.typography.labelSmall, color = NocTekst, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(34.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Safran)
                        .clickable(onClick = naDalje),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (preostalo > 0) "Dalje (još $preostalo)" else "Super!",
                        style = MaterialTheme.typography.titleMedium, color = Noc, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
