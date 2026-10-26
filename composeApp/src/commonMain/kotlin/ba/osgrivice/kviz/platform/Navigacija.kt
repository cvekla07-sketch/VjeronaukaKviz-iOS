package ba.osgrivice.kviz.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Zamjena za androidx.activity.compose.BackHandler.
 * iOS: reaguje na povlačenje prstom s lijeve ivice ekrana ("nazad" gest).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
}
