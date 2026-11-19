package ua.kpi.energyforecast.pw05.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import ua.kpi.energyforecast.pw05.MainActivity
import ua.kpi.energyforecast.pw05.domain.*

/** Єдине сховище для Activity і receiver у стандартному процесі застосунку.
 * Операції серіалізовано; журнал записується перед плануванням системного alarm. */
class AlertCenter private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("alerts", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private fun read(): List<Alert> = runCatching {
        val a = JSONArray(prefs.getString("journal", "[]"))
        (0 until a.length()).map { index ->
            val o = a.getJSONObject(index)
            Alert(o.getInt("id"), AlertCategory.valueOf(o.getString("category")), o.getString("title"), o.getString("text"), o.getLong("due"), AlertStatus.valueOf(o.getString("status")), o.optInt("revision"))
        }
    }.getOrDefault(emptyList())
    private fun save(items: List<Alert>) {
        val a = JSONArray()
        items.forEach { item -> a.put(JSONObject().put("id", item.id).put("category", item.category.name).put("title", item.title).put("text", item.text).put("due", item.due).put("status", item.status.name).put("revision", item.revision)) }
        check(prefs.edit().putString("journal", a.toString()).commit()) { "Не вдалося зберегти журнал" }
        mutable.value = items
    }
    init {
        AlertCategory.entries.forEach { category ->
            val channel = NotificationChannel(category.name, category.title,
                if (category == AlertCategory.SENSOR) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Локальні події: ${category.title}"
            channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            manager.createNotificationChannel(channel)
        }
    }
    fun enabled(category: AlertCategory) = prefs.getBoolean("enabled", true) && prefs.getBoolean(category.name, true)
    fun blocked(category: AlertCategory): Boolean =
        !manager.areNotificationsEnabled() || manager.getNotificationChannel(category.name)?.importance == NotificationManager.IMPORTANCE_NONE ||
        (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
    @Synchronized fun enable(category: AlertCategory?, enabled: Boolean) {
        prefs.edit().putBoolean(category?.name ?: "enabled", enabled).commit()
        if (!enabled) mutable.value.filter { category == null || it.category == category }.forEach {
            if (it.status in listOf(AlertStatus.SCHEDULED, AlertStatus.SHOWN, AlertStatus.BLOCKED)) act(it.id, it.revision, AlertAction.CANCEL)
        }
    }
    fun globallyEnabled() = prefs.getBoolean("enabled", true)
    private fun pending(item: Alert, action: String): PendingIntent {
        val intent = Intent(context, AlertReceiver::class.java).setAction(action)
            .setData(Uri.parse("energyforecast://alert/${item.id}/${item.revision}/$action"))
            .putExtra("id", item.id).putExtra("revision", item.revision)
        return PendingIntent.getBroadcast(context, item.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun schedule(item: Alert) {
        // Неточний alarm: Android може відкласти доставку через Doze. Спеціальний дозвіл exact alarm не потрібний.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.due, pending(item, "DELIVER"))
    }
    @Synchronized fun create(category: AlertCategory, title: String, text: String, delayMillis: Long = 0): Alert? {
        require(delayMillis in 0..2_592_000_000L)
        if (!enabled(category)) return null
        val active = mutable.value.count { it.status in listOf(AlertStatus.SCHEDULED, AlertStatus.SHOWN, AlertStatus.BLOCKED) }
        if (active >= 30) return null
        val next = prefs.getInt("next", 1)
        check(next < Int.MAX_VALUE)
        check(prefs.edit().putInt("next", next + 1).commit())
        val item = Alert(next, category, title.take(160), text.take(1000), System.currentTimeMillis() + delayMillis)
        val retained = mutable.value.filter { it.status !in listOf(AlertStatus.CONFIRMED, AlertStatus.CANCELLED) } +
            mutable.value.filter { it.status in listOf(AlertStatus.CONFIRMED, AlertStatus.CANCELLED) }.takeLast(70)
        save(retained + item)
        if (delayMillis == 0L) deliver(item.id, item.revision) else schedule(item)
        return item
    }
    @Synchronized fun sensor(kind: SensorKind, reading: SensorReading, threshold: Float) {
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_${kind.name}", 0)
        if (now - last < 60_000 || !enabled(AlertCategory.SENSOR) || blocked(AlertCategory.SENSOR)) return
        if (create(AlertCategory.SENSOR, "Перевищення: ${kind.title}",
            "Модуль ${reading.magnitude} ${kind.unit}; поріг $threshold. Час сенсора: ${reading.timestampNanos / 1_000_000} мс.") != null)
            prefs.edit().putLong("last_${kind.name}", now).commit()
    }
    @Synchronized fun act(id: Int, revision: Int, action: AlertAction) {
        val item = mutable.value.find { it.id == id && it.revision == revision } ?: return
        val updated = AlertRules.apply(item, action, System.currentTimeMillis())
        if (updated == item) return
        alarms.cancel(pending(item, "DELIVER"))
        manager.cancel(id)
        save(mutable.value.map { if (it.id == id) updated else it })
        if (updated.status == AlertStatus.SCHEDULED && enabled(updated.category)) schedule(updated)
    }
    @Synchronized fun deliver(id: Int, revision: Int) {
        val item = mutable.value.find { it.id == id } ?: return
        if (!AlertRules.deliverable(item, revision, System.currentTimeMillis())) return
        if (!enabled(item.category)) { act(id, revision, AlertAction.CANCEL); return }
        val details = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java)
            .setData(Uri.parse("energyforecast://details/$id")).putExtra("alert_id", id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, item.category.name)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(item.title).setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text)).setContentIntent(details)
            .setOnlyAlertOnce(true).setAutoCancel(false)
            .setPriority(if (item.category == AlertCategory.SENSOR) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Підтвердити", pending(item, "CONFIRM"))
            .addAction(0, "Відкласти на 5 хв", pending(item, "SNOOZE"))
            .addAction(0, "Деталі", details)
            .setDeleteIntent(pending(item, "CANCEL")).build()
        val shown = if (blocked(item.category)) false else try { manager.notify(id, notification); true } catch (_: SecurityException) { false }
        save(mutable.value.map { if (it.id == id) it.copy(status = if (shown) AlertStatus.SHOWN else AlertStatus.BLOCKED) else it })
    }
    @Synchronized fun restore() {
        mutable.value.filter { it.status == AlertStatus.SCHEDULED }.forEach { schedule(it) }
    }
    companion object {
        @Volatile private var instance: AlertCenter? = null
        fun get(context: Context): AlertCenter = instance ?: synchronized(this) {
            instance ?: AlertCenter(context.applicationContext).also { instance = it }
        }
    }
}
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val center = AlertCenter.get(context)
        if (intent.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) { center.restore(); return }
        val id = intent.getIntExtra("id", -1); val revision = intent.getIntExtra("revision", -1)
        when (intent.action) {
            "DELIVER" -> center.deliver(id, revision)
            "CONFIRM" -> center.act(id, revision, AlertAction.CONFIRM)
            "SNOOZE" -> center.act(id, revision, AlertAction.SNOOZE)
            "CANCEL" -> center.act(id, revision, AlertAction.CANCEL)
        }
    }
}

