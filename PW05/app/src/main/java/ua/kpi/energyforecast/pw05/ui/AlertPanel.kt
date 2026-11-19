package ua.kpi.energyforecast.pw05.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter
import java.text.DateFormat
import java.util.Date

@Composable
fun AlertPanel(center: AlertCenter, detailId: Int?) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val items by center.state.collectAsStateWithLifecycle()
    var refresh by remember { mutableIntStateOf(0) }
    var result by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(-1) }
    var minutes by rememberSaveable { mutableStateOf("1") }
    var selected by rememberSaveable(detailId) { mutableStateOf(detailId) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        result = if (allowed) "Дозвіл надано" else "Дозвіл не надано. Сповіщення можна дозволити в системних налаштуваннях."
        refresh++
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Text("Локальні сповіщення", style = MaterialTheme.typography.titleLarge)
    Text("Сенсорні події відстежуються лише коли застосунок видимий. Заплановані нагадування працюють незалежно від екрана. Система може затримати неточний таймер.")
    key(refresh) {
        Row { Switch(center.globallyEnabled(), { center.enable(null, it); refresh++ }, modifier = Modifier.semantics { contentDescription = "Увімкнути сповіщення" }); Text("Увімкнути сповіщення") }
        AlertCategory.entries.forEach { category ->
            Row { Checkbox(center.enabled(category), { center.enable(category, it); refresh++ }, modifier = Modifier.semantics { contentDescription = "Категорія: ${category.title}" }); Text(category.title) }
            Text(if (center.blocked(category)) "${category.title}: системний дозвіл або канал заблокований" else "${category.title}: системний канал доступний")
            TextButton({
                context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, category.name))
            }) { Text("Канал «${category.title}»: звук і пріоритет") }
        }
    }
    if (Build.VERSION.SDK_INT >= 33) Button({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Запитати дозвіл") }
    TextButton({ context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Системні налаштування сповіщень") }
    Button({
        val item = center.create(AlertCategory.FORECAST, "Прогноз споживання", "Навчальний базовий прогноз: 23,60 кВт·год на 24 години. Це не показник лічильника.")
        result = if (item == null) "Категорію вимкнено або досягнуто ліміту 30 активних подій" else "Подія №${item.id} записана в журнал"
    }) { Text("Повідомлення зараз") }
    val delay = minutes.toLongOrNull()?.takeIf { it in 1..43200 }
    OutlinedTextField(minutes, { minutes = it }, label = { Text("Через скільки хвилин (1–43200)") }, isError = delay == null, modifier = Modifier.fillMaxWidth())
    Button({
        var created = 0
        listOf(AlertCategory.REMINDER, AlertCategory.FORECAST).forEach { category ->
            if (center.create(category, category.title, "Перевірте демонстраційний прогноз та журнал сенсорів.", delay!! * 60_000) != null) created++
        }
        result = "Заплановано подій: $created. Час доставки неточний."
    }, enabled = delay != null) { Text("Запланувати дві незалежні події") }
    Text(result)
    Button({ filter = if (filter == 2) -1 else filter + 1 }) { Text("Журнал: ${AlertCategory.entries.getOrNull(filter)?.title ?: "Усі категорії"}") }
    Text("Журнал: ${items.size} подій; до 30 активних. Звук залежить від системи та режиму «Не турбувати».")
    items.asReversed().filter { filter < 0 || it.category.ordinal == filter }.forEach { item ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("№${item.id} · ${item.category.title} · ${item.status.title}")
                Text(item.title)
                Text("Не раніше: ${DateFormat.getDateTimeInstance().format(Date(item.due))}")
                if (selected == item.id) Text(item.text)
                TextButton({ selected = if(selected == item.id) null else item.id }) { Text("Деталі №${item.id}") }
                if (item.status !in listOf(AlertStatus.CONFIRMED, AlertStatus.CANCELLED)) {
                    TextButton({ center.act(item.id, item.revision, AlertAction.CONFIRM) }) { Text("Підтвердити №${item.id}") }
                    TextButton({ center.act(item.id, item.revision, AlertAction.SNOOZE) }) { Text("Відкласти №${item.id} на 5 хв") }
                    TextButton({ center.act(item.id, item.revision, AlertAction.CANCEL) }) { Text("Скасувати №${item.id}") }
                }
            }
        }
    }
}

