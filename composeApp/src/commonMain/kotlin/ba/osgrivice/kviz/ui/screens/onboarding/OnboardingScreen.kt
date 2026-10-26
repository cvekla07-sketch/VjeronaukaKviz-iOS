package ba.osgrivice.kviz.ui.screens.onboarding

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.ui.components.Ornament
import ba.osgrivice.kviz.ui.components.pritisak
import ba.osgrivice.kviz.ui.theme.Noc
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.SafranTamno
import ba.osgrivice.kviz.ui.theme.ZvijezdaShape
import kotlinx.coroutines.launch

/** Prvi ekran, samo jednom: ime i razred. */
@Composable
fun OnboardingScreen(naDalje: () -> Unit) {
    val app = KvizApp
    val scope = rememberCoroutineScope()
    var ime by rememberSaveable { mutableStateOf("") }
    var razred by rememberSaveable { mutableIntStateOf(0) }
    var spremam by remember { mutableStateOf(false) }
    val moze = ime.trim().length >= 2 && razred in 8..9 && !spremam

    Box(
        Modifier
            .fillMaxSize()
            .background(PozadinaNoc)
    ) {
        Ornament()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(56.dp))
            Box(
                Modifier
                    .size(72.dp)
                    .background(Safran, ZvijezdaShape),
                contentAlignment = Alignment.Center
            ) { Text("۩", color = Noc, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge) }

            Spacer(Modifier.height(24.dp))
            Text("Selam alejk!", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(
                "Vježbaj vjeronauku nekoliko minuta dnevno i dođi spreman na takmičenje.",
                style = MaterialTheme.typography.bodyLarge,
                color = NocTekst,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(36.dp))
            Text("Kako se zoveš?", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = ime,
                onValueChange = { if (it.length <= 24) ime = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ime ili nadimak", color = NocTekst) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Safran,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Safran,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
            )

            Spacer(Modifier.height(28.dp))
            Text("Koji si razred?", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(8, 9).forEach { r ->
                    val odabran = razred == r
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(84.dp)
                            .pritisak(interaction)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (odabran) Safran else Color.White.copy(alpha = 0.08f))
                            .clickable(interactionSource = interaction, indication = null) { razred = r },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (r == 8) "VIII" else "IX",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (odabran) Noc else Color.White
                            )
                            Text("$r. razred", style = MaterialTheme.typography.labelMedium, color = if (odabran) SafranTamno else NocTekst)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .pritisak(interaction)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (moze) Safran else Color.White.copy(alpha = 0.12f))
                    .clickable(interactionSource = interaction, indication = null, enabled = moze) {
                        spremam = true
                        scope.launch {
                            app.container.profilStore.sacuvajOsnovno(ime, razred)
                            naDalje()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Kreni", style = MaterialTheme.typography.titleMedium, color = if (moze) Noc else NocTekst, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
