package ba.osgrivice.kviz

import androidx.compose.runtime.Composable
import ba.osgrivice.kviz.ui.navigation.KvizNavHost
import ba.osgrivice.kviz.ui.theme.VjeronaukaKvizTheme

/** Isto kao MainActivity.setContent { ... } na Androidu. */
@Composable
fun App() {
    VjeronaukaKvizTheme {
        KvizNavHost()
    }
}
