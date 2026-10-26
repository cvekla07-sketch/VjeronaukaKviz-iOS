package ba.osgrivice.kviz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ba.osgrivice.kviz.resources.Res
import ba.osgrivice.kviz.resources.roboto_bold
import ba.osgrivice.kviz.resources.roboto_extrabold
import ba.osgrivice.kviz.resources.roboto_italic
import ba.osgrivice.kviz.resources.roboto_medium
import ba.osgrivice.kviz.resources.roboto_regular
import ba.osgrivice.kviz.resources.roboto_semibold
import org.jetbrains.compose.resources.Font

private val Shema = lightColorScheme(
    primary = Noc,
    onPrimary = Color.White,
    primaryContainer = Noc2,
    onPrimaryContainer = Color.White,
    secondary = Safran,
    onSecondary = Noc,
    secondaryContainer = Safran,
    onSecondaryContainer = Noc,
    tertiary = Tirkiz2,
    onTertiary = Color.White,
    background = Krem,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Krem,
    onSurfaceVariant = Siva,
    outline = Linija,
    error = Ne,
    onError = Color.White,
    errorContainer = NeBg,
    onErrorContainer = Ne,
)

/** iOS: Roboto (Androidov sistemski font) da tekst izgleda isto kao na Android aplikaciji. */
@Composable
private fun roboto(): FontFamily = FontFamily(
    Font(Res.font.roboto_regular, FontWeight.Normal),
    Font(Res.font.roboto_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.roboto_medium, FontWeight.Medium),
    Font(Res.font.roboto_semibold, FontWeight.SemiBold),
    Font(Res.font.roboto_bold, FontWeight.Bold),
    Font(Res.font.roboto_extrabold, FontWeight.ExtraBold),
)

private fun Typography.saFontom(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun VjeronaukaKvizTheme(content: @Composable () -> Unit) {
    val f = roboto()
    // puni naziv: val "Typography" iz Type.kt (isti kao na Androidu)
    val tipografija = remember(f) { ba.osgrivice.kviz.ui.theme.Typography.saFontom(f) }
    MaterialTheme(colorScheme = Shema, typography = tipografija, content = content)
}
