package ba.osgrivice.kviz.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Petokraka zvijezda sa "mekim" unutrašnjim radijusom — oznaka lekcija i odgovora. */
object ZvijezdaShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(zvijezdaPath(size, kraka = 5, unutrasnji = 0.5f))
}

/** Osmokraka zvijezda (rub-i-hizb motiv) — veliki rezultat. */
object OsmokrakaShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(zvijezdaPath(size, kraka = 8, unutrasnji = 0.72f))
}

fun zvijezdaPath(size: Size, kraka: Int, unutrasnji: Float): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(cx, cy)
    val path = Path()
    val korak = PI / kraka
    for (i in 0 until kraka * 2) {
        val rr = if (i % 2 == 0) r else r * unutrasnji
        val ugao = -PI / 2 + i * korak
        val x = cx + (rr * cos(ugao)).toFloat()
        val y = cy + (rr * sin(ugao)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun centarZvijezde(size: Size): Offset = Offset(size.width / 2f, size.height / 2f)
