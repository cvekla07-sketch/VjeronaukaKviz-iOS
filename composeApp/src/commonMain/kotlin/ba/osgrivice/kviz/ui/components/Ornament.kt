package ba.osgrivice.kviz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import ba.osgrivice.kviz.ui.theme.zvijezdaPath

/**
 * Geometrijski ornament (osmokraka zvijezda + romb + krug) ponavljan preko cijele pozadine.
 * Crta se jednom po veličini; alfa drži ispod 10% da ostane tekstura, ne dekoracija.
 */
@Composable
fun Ornament(modifier: Modifier = Modifier, boja: Color = Color.White, alfa: Float = 0.07f) {
    Canvas(modifier.fillMaxSize()) {
        val tile = 72.dp.toPx()
        val stroke = Stroke(width = 1.2.dp.toPx())
        val zvijezda = zvijezdaPath(Size(tile * 0.9f, tile * 0.9f), kraka = 8, unutrasnji = 0.72f)
        val c = boja.copy(alpha = alfa)
        var y = -tile
        while (y < size.height + tile) {
            var x = -tile
            while (x < size.width + tile) {
                translate(x + tile * 0.05f, y + tile * 0.05f) {
                    drawPath(zvijezda, c, style = stroke)
                }
                drawCircle(c, radius = tile * 0.08f, center = Offset(x + tile / 2f, y + tile / 2f), style = stroke)
                x += tile
            }
            y += tile
        }
    }
}
