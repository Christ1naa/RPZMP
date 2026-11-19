package ua.kpi.energyforecast.pw05.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw05.domain.*
import kotlin.math.hypot

@Composable
fun SensorVisuals(sensors: SensorPanelState, style: DashboardState, update: (DashboardState) -> Unit) {
    Text("Власні сенсорні компоненти", style = MaterialTheme.typography.titleLarge)
    Text("Проєкція XY у системі осей пристрою: X праворуч, Y вгору. Акселерометр включає гравітацію; це не калібрований кут. Z наведено текстом.")
    Text("Масштаб: ${style.scale}")
    Slider(style.scale, { update(style.copy(scale = it)) }, valueRange = 0.5f..2f)
    Button({ update(style.copy(alternate = !style.alternate)) }) { Text("Змінити кольори") }
    Button({ update(style.copy(large = !style.large)) }) { Text("Розмір: ${if (style.large) "великий" else "звичайний"}") }
    val accent = if (style.alternate) Color(0xFF793F99) else Color(0xFF235FC1)
    val second = if (style.alternate) Color(0xFF257243) else Color(0xFFB34A18)
    val accel = sensors.snapshots.find { it.kind == SensorKind.ACCELEROMETER && it.status == SensorStatus.LIVE }?.readings?.lastOrNull()
    val gyro = sensors.snapshots.find { it.kind == SensorKind.GYROSCOPE && it.status == SensorStatus.LIVE }?.readings?.lastOrNull()
    Text("Акселерометр: ${accel?.values?.joinToString { energy(it.toDouble()) } ?: "немає актуальних даних"} м/с²")
    Text("Гіроскоп: ${gyro?.values?.joinToString { energy(it.toDouble()) } ?: "немає актуальних даних"} рад/с")
    val av = accel?.let { normalizedVector(it.values[0], it.values[1], 9.81f, style.scale) } ?: (0f to 0f)
    val gv = gyro?.let { normalizedVector(it.values[0], it.values[1], 2f, style.scale) } ?: (0f to 0f)
    // Анімації читаються в draw-фазі. Немає окремих SensorManager-підписок у компонентах.
    val ax = animateFloatAsState(av.first, tween(150), label = "accelX")
    val ay = animateFloatAsState(av.second, tween(150), label = "accelY")
    val gx = animateFloatAsState(gv.first, tween(150), label = "gyroX")
    val gy = animateFloatAsState(gv.second, tween(150), label = "gyroY")
    val fill = animateFloatAsState(hypot(av.first, av.second).coerceIn(0f, 1f), tween(150), label = "arc")
    Text("Коло — нахил / XY-прискорення; дуга 0–100% шкали XY; лінії — два нормовані вектори.")
    Canvas(Modifier.fillMaxWidth().height(if (style.large) 320.dp else 240.dp).semantics {
        contentDescription = "Індикатор нахилу, дуга та вектори. Точні компоненти наведено текстом вище."
    }) {
        val r = minOf(size.width, size.height) * 0.36f
        val c = center
        drawCircle(Color.LightGray, r, c, style = Stroke(2.dp.toPx()))
        drawLine(Color.Gray, c - Offset(r, 0f), c + Offset(r, 0f))
        drawLine(Color.Gray, c - Offset(0f, r), c + Offset(0f, r))
        if (accel != null) {
            drawArc(accent, -90f, fill.value * 360, false, c - Offset(r + 10.dp.toPx(), r + 10.dp.toPx()), Size((r + 10.dp.toPx()) * 2, (r + 10.dp.toPx()) * 2), style = Stroke(5.dp.toPx()))
            val end = c + Offset(ax.value * r, -ay.value * r)
            drawLine(accent, c, end, 4.dp.toPx())
            drawCircle(accent, 8.dp.toPx(), end)
        }
        if (gyro != null) {
            val end = c + Offset(gx.value * r, -gy.value * r)
            drawLine(second, c, end, 7.dp.toPx())
            drawCircle(second, 5.dp.toPx(), end)
        }
    }
    Text("Легенда: ${if(style.alternate) "фіолетовий" else "синій"} — акселерометр (9,81 м/с² = повна шкала); ${if(style.alternate) "зелений" else "помаранчевий"} — гіроскоп (2 рад/с = повна шкала). Вектори обмежені колом.")
}

