package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import ua.kpi.energyforecast.model.ForecastResult
import java.util.Locale

/**
 * Показує накопичену енергію: X — день, Y — добове споживання × день у кВт·год.
 * Однаковий режим щодня дає пряму лінію; кольори й дані оновлюються з Compose.
 * Перехід у Float потрібний лише для API графіка; розрахунки залишаються в Double.
 */
@Composable
fun ConsumptionChart(
    result: ForecastResult,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),

        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false

                // Графік інформаційний: масштабування поки не потрібне.
                setTouchEnabled(false)
                setScaleEnabled(false)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)

                axisLeft.axisMinimum = 0f

                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }

                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format(
                            Locale.forLanguageTag("uk-UA"),
                            "%.1f",
                            value
                        )
                    }
                }
            }
        },

        update = { chart ->
            val entries = (0..result.forecastDays).map { day ->
                Entry(
                    day.toFloat(),
                    (result.dailyConsumptionKwh * day).toFloat()
                )
            }

            val dataSet = LineDataSet(
                entries,
                "Накопичене споживання"
            ).apply {
                color = lineColor
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            chart.setBackgroundColor(backgroundColor)

            chart.xAxis.textColor = textColor
            chart.xAxis.axisLineColor = gridColor
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = result.forecastDays.toFloat()
            chart.xAxis.setLabelCount(
                minOf(result.forecastDays + 1, 7),
                false
            )

            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor
            chart.axisLeft.axisLineColor = gridColor

            chart.data = LineData(dataSet)
            chart.contentDescription =
                "Накопичене споживання за ${result.forecastDays} днів: " +
                        "${result.periodConsumptionKwh} кВт·год"

            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}