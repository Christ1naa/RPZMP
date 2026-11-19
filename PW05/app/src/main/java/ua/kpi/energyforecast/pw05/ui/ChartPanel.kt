package ua.kpi.energyforecast.pw05.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw05.domain.*
import java.util.Locale

fun energy(value: Double) = String.format(Locale.forLanguageTag("uk-UA"), "%.2f", value)
@Composable
fun ChartPanel(state: DashboardState, all: List<ChartPoint>, update: (DashboardState) -> Unit) {
    Text("Графіки споживання", style = MaterialTheme.typography.titleLarge)
    Text("Демонстраційна історія 30 діб. Стовпчики й лінія показують той самий набір кВт·год.")
    ChartPeriod.entries.forEach { period ->
        FilterChip(state.period == period, { update(state.copy(period = period, offset = 0)) }, { Text(period.title) })
    }
    Button({ update(state.copy(generation = state.generation + 1)) }) { Text("Оновити демонстраційний сценарій") }
    Text("Масштаб: ${state.zoom}×")
    Slider(state.zoom.toFloat(), { update(state.copy(zoom = it.toInt(), offset = 0)) }, valueRange = 1f..4f, steps = 2)
    val visible = ChartData.window(all, state.zoom, state.offset)
    val maxStart = (all.size - visible.size).coerceAtLeast(0)
    if (maxStart > 0) {
        Text("Початок діапазону: ${state.offset.coerceAtMost(maxStart) + 1}")
        Slider(state.offset.coerceAtMost(maxStart).toFloat(), { update(state.copy(offset = it.toInt())) }, valueRange = 0f..maxStart.toFloat())
    }
    ChartData.summary(visible)?.let { Text("Видимий діапазон: сума ${energy(it.totalKwh)}, середнє ${energy(it.meanKwh)}, пік ${energy(it.peakKwh)} кВт·год за інтервал.") }
    var mode by rememberSaveable { mutableIntStateOf(2) }
    Button({ mode = (mode + 1) % 3 }) { Text("Вигляд: ${listOf("Лінія", "Стовпчики", "Комбінований")[mode]}") }
    EnergyChart(visible, mode)
    Text("Категорії за видимий діапазон", style = MaterialTheme.typography.titleMedium)
    val total = visible.sumOf { it.kwh }
    val zones = listOf(ChartPoint("Житлова", total * 0.5), ChartPoint("Кухня", total * 0.3), ChartPoint("Інше", total * 0.2))
    Text("Навчальний розподіл: 50% / 30% / 20% суми того самого діапазону.")
    EnergyChart(zones, 1)
}
@Composable
fun EnergyChart(points: List<ChartPoint>, mode: Int) {
    var selected by rememberSaveable(points) { mutableIntStateOf(-1) }
    val max = (points.maxOfOrNull { it.kwh } ?: 0.0).coerceAtLeast(1.0)
    if (points.isEmpty()) { Text("Немає даних"); return }
    Text("Y: енергія, кВт·год (0…${energy(max)}). X: інтервали / категорії")
    Text(when(mode) { 0 -> "Легенда: синя лінія. Натисніть точку."; 1 -> "Легенда: зелені стовпчики. Натисніть стовпчик."; else -> "Легенда: синя лінія, зелені стовпчики. Натисніть точку або стовпчик." })
    val density = LocalDensity.current
    val paint = remember(density) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = with(density) { 11.sp.toPx() }
    } }
    // Одна координатна модель для малювання та hit testing; адаптується до ширини.
    Canvas(Modifier.fillMaxWidth().height(260.dp)
        .semantics { contentDescription = "Графік енергії. ${points.size} інтервалів. Точні значення доступні кнопкою під графіком." }
        .pointerInput(points) { detectTapGestures { pos ->
            val left = 56.dp.toPx(); val width = size.width - left - 12.dp.toPx()
            selected = ((pos.x - left) / width * points.size).toInt().coerceIn(points.indices)
        } }) {
        val left = 56.dp.toPx(); val right = size.width - 12.dp.toPx()
        val top = 12.dp.toPx(); val bottom = size.height - 30.dp.toPx()
        val step = (right - left) / points.size
        fun at(i: Int) = Offset(left + step * (i + 0.5f), bottom - (points[i].kwh / max).toFloat() * (bottom - top))
        for (i in 0..4) {
            val y = bottom - (bottom - top) * i / 4
            drawLine(Color.LightGray, Offset(left, y), Offset(right, y), 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(String.format(Locale.forLanguageTag("uk-UA"), "%.1f", max * i / 4), 0f, y + paint.textSize * 0.3f, paint)
        }
        drawLine(Color.DarkGray, Offset(left, top), Offset(left, bottom), 2.dp.toPx())
        drawLine(Color.DarkGray, Offset(left, bottom), Offset(right, bottom), 2.dp.toPx())
        listOf(0, points.lastIndex / 2, points.lastIndex).distinct().forEach { i ->
            val label = points[i].label
            val width = paint.measureText(label)
            val x = (at(i).x - width / 2).coerceIn(left, (right - width).coerceAtLeast(left))
            drawContext.canvas.nativeCanvas.drawText(label, x, size.height - 4.dp.toPx(), paint)
        }
        points.indices.forEach { i ->
            val p = at(i)
            if (mode != 0) drawRect(Color(0xFF278C72).copy(alpha = 0.65f), Offset(p.x - step * 0.3f, p.y), Size(step * 0.6f, bottom - p.y))
            if (mode != 1) {
                if (i > 0) drawLine(Color(0xFF235FC1), at(i - 1), p, 2.dp.toPx())
                drawCircle(Color(0xFF235FC1), 3.dp.toPx(), p)
            }
            if (selected == i) drawLine(Color.Black, Offset(p.x, top), Offset(p.x, bottom), 2.dp.toPx())
        }
    }
    Text("X: ${points.first().label} → ${points.last().label}; поділки сітки Y: ${(0..4).joinToString { energy(max * it / 4) }}")
    Button({ selected = (selected + 1) % points.size }) { Text("Наступне значення / підказка") }
    points.getOrNull(selected)?.let { Text("Підказка: ${it.label} — ${energy(it.kwh)} кВт·год") }
}

