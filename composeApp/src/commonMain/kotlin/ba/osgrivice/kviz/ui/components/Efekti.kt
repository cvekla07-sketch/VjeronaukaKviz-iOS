package ba.osgrivice.kviz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import ba.osgrivice.kviz.ui.theme.Safran
import ba.osgrivice.kviz.ui.theme.Safran2
import ba.osgrivice.kviz.ui.theme.Tirkiz
import ba.osgrivice.kviz.ui.theme.Ok
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Skaliranje pri pritisku — osjećaj dugmeta u igri. */
fun Modifier.pritisak(interaction: MutableInteractionSource, skala: Float = 0.96f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) skala else 1f, spring(stiffness = 600f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Horizontalno tresenje — pokreće se kad `okidac` postane true. */
@Composable
fun Modifier.tresenje(okidac: Boolean): Modifier {
    val pomak = remember { Animatable(0f) }
    LaunchedEffect(okidac) {
        if (okidac) {
            pomak.animateTo(
                0f,
                keyframes {
                    durationMillis = 420
                    0f at 0
                    -14f at 60
                    12f at 120
                    -9f at 180
                    7f at 240
                    -4f at 300
                    0f at 420
                }
            )
        }
    }
    return graphicsLayer { translationX = pomak.value }
}

/** Broj koji "otkucava" do ciljne vrijednosti. */
@Composable
fun animiraniBroj(cilj: Int, trajanje: Int = 900): Int {
    val v by animateFloatAsState(cilj.toFloat(), tween(trajanje), label = "broj")
    return v.toInt()
}

private data class Cestica(
    val x0: Float, val y0: Float, val vx: Float, val vy: Float,
    val boja: Color, val vel: Float, val rot: Float
)

/** Konfete — jednokratna eksplozija iz gornjeg dijela ekrana. */
@Composable
fun Konfete(modifier: Modifier = Modifier, aktivno: Boolean = true) {
    val t = remember { Animatable(0f) }
    var velicina by remember { mutableStateOf(IntSize.Zero) }
    val cestice = remember(velicina) {
        if (velicina == IntSize.Zero) emptyList() else {
            val boje = listOf(Safran, Safran2, Tirkiz, Ok, Color.White)
            List(90) {
                val ugao = Random.nextFloat() * PI.toFloat() * 2f
                val brzina = 200f + Random.nextFloat() * 700f
                Cestica(
                    x0 = velicina.width / 2f,
                    y0 = velicina.height * 0.28f,
                    vx = cos(ugao) * brzina,
                    vy = sin(ugao) * brzina - 400f,
                    boja = boje[it % boje.size],
                    vel = 10f + Random.nextFloat() * 14f,
                    rot = Random.nextFloat() * 360f
                )
            }
        }
    }
    LaunchedEffect(aktivno, cestice) {
        if (aktivno && cestice.isNotEmpty()) {
            t.snapTo(0f)
            t.animateTo(1f, tween(2200, easing = LinearEasing))
        }
    }
    Canvas(modifier.fillMaxSize().onSizeChanged { velicina = it }) {
        val sek = t.value * 2.2f
        val alfa = (1f - t.value).coerceIn(0f, 1f)
        cestice.forEach { c ->
            val x = c.x0 + c.vx * sek
            val y = c.y0 + c.vy * sek + 0.5f * 1400f * sek * sek
            rotate(degrees = c.rot + sek * 360f, pivot = Offset(x, y)) {
                drawRect(c.boja.copy(alpha = alfa), topLeft = Offset(x, y), size = Size(c.vel, c.vel * 0.6f))
            }
        }
    }
}
