package ba.osgrivice.kviz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ba.osgrivice.kviz.ui.theme.NocTekst
import ba.osgrivice.kviz.ui.theme.PozadinaNoc
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.VjeronaukaKvizTheme

/**
 * Dio 2 — privremeni ekran: provjera teme (boje, Roboto font).
 * U Dijelu 4 ovdje dolazi KvizNavHost() — isto kao MainActivity na Androidu.
 */
@Composable
fun App() {
    VjeronaukaKvizTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(PozadinaNoc),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vjeronauka Kviz", style = MaterialTheme.typography.headlineMedium, color = Safran)
            Text("iOS — tema i font", style = MaterialTheme.typography.bodyLarge, color = NocTekst)
        }
    }
}
