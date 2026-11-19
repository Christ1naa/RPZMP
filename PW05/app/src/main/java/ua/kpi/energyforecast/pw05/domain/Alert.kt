package ua.kpi.energyforecast.pw05.domain

enum class AlertCategory(val title: String) { SENSOR("Сенсори"), FORECAST("Прогноз"), REMINDER("Нагадування") }
enum class AlertStatus(val title: String) { SCHEDULED("Заплановано"), SHOWN("Показано"), BLOCKED("Заблоковано"), CONFIRMED("Підтверджено"), CANCELLED("Скасовано") }
data class Alert(val id: Int, val category: AlertCategory, val title: String, val text: String, val due: Long, val status: AlertStatus = AlertStatus.SCHEDULED, val revision: Int = 0)
enum class AlertAction { CONFIRM, CANCEL, SNOOZE }
object AlertRules {
    fun apply(item: Alert, action: AlertAction, now: Long): Alert {
        if (item.status == AlertStatus.CONFIRMED || item.status == AlertStatus.CANCELLED) return item
        return when (action) {
            AlertAction.CONFIRM -> item.copy(status = AlertStatus.CONFIRMED, revision = item.revision + 1)
            AlertAction.CANCEL -> item.copy(status = AlertStatus.CANCELLED, revision = item.revision + 1)
            AlertAction.SNOOZE -> item.copy(status = AlertStatus.SCHEDULED, due = Math.addExact(now, 300_000L), revision = item.revision + 1)
        }
    }
    fun deliverable(item: Alert, revision: Int, now: Long) =
        item.status == AlertStatus.SCHEDULED && item.revision == revision && now >= item.due
}

