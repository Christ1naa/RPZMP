"""Validate original evidence hashes and generate the Ukrainian PW04 gallery."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "docs/evidence/final-checks"
CAPTIONS = {
 "01-monitor-live": "Моніторинг: обидва джерела LIVE, потужність і добовий прогноз.",
 "02-catalog-no-key": "Каталог без ключа: інструкція замість Maps, список доступний.",
 "03-filter-panel": "Комбіновані фільтри: Офіс + Активний + 30–50 кВт.",
 "04-filtered-office": "Один office-1 після комбінованого запиту.",
 "05-details-top": "Деталі об'єкта: поля, одиниці, дати, демонстраційні координати.",
 "06-details-bottom": "Деталі після команди прокрутки; окремого зсуву, якщо все вмістилося, не доводить.",
 "07-empty-search": "Нуль збігів за пошуком і наявними фільтрами; доступне скидання.",
 "10-permission-precise-dialog": "Справжній системний діалог A54: Precise, до надання дозволу.",
 "11-permission-approximate-dialog": "Справжній діалог: Approximate; coarse без fine підтверджено assertions.",
 "12-permission-denial-dialog": "Системний діалог перед першою відмовою.",
 "13-permission-denied-app": "Застосунок після першої відмови; список і Maps доступні.",
 "14-permission-permanently-denied-app": "Після двох відмов/третього запиту; відсутність нового діалогу перевірена assertions.",
 "15-own-app-settings": "Системний App info саме PW04, перед поверненням у застосунок.",
 "16-permission-revoked-app": "Дозвіл grant → revoke до запуску нової Activity; повідомлення без аварії.",
 "17-location-disabled": "Системна Location вимкнена; наступне повернення із GPS settings перевірено тестом.",
 "20-meter-retrying": "Збій лічильника: RETRYING, останній прогноз застарілий.",
 "20-model-retrying": "Збій моделі: RETRYING, лічильник продовжує роботу.",
 "21-meter-recovered": "Лічильник автоматично відновився, обидва джерела LIVE.",
 "21-model-recovered": "Модель автоматично відновилася, обидва джерела LIVE.",
 "22-invalid-range": "Мінімум 50 перевищує максимум 10; видима помилка валідації.",
 "24-landscape": "Справжній landscape Configuration, карта прихована, список прокручено.",
 "25-portrait-restored": "Повернення у portrait після повороту; стан каталогу збережено.",
 "26-system-large-font": "Системний font_scale=1.6, список прокручується до останнього об'єкта.",
 "27-large-font-filter-reset": "Великий системний шрифт: доступна дія Скинути.",
 "28-large-font-filter-done": "Великий системний шрифт: доступна дія Готово.",
 "30-labelled-synthetic-position": "Свіжа тестова позиція ±5 м; це початковий кадр, тайли можуть ще завантажуватися.",
 "31-labelled-position-updated": "Другий системний mock fix Києва ±6 м і зелений маркер, явно тестова позиція.",
 "32-providers-disabled": "Обидва тестові провайдери вимкнено; актуальна позиція очищена.",
 "33-providers-recovered": "Після повторного увімкнення на тих самих координатах: ±7 м і зелений маркер.",
 "40-invalid-key-map-failed": "Навмисно неправильний ключ у MainActivity: timeout і Повторити мапу.",
 "41-invalid-key-map-retrying": "Після Повторити мапу створено нову генерацію, показано завантаження.",
 "42-invalid-key-catalogue-still-usable": "При неправильному ключі каталог/деталі залишаються доступними.",
 "50-network-off-map-failed": "Реальні Wi-Fi/mobile data вимкнено; ізольований EnergyMap, timeout.",
 "51-network-retry-loading": "Повтор завантаження до повернення мережі, ізольований EnergyMap.",
 "52-network-restored-map-loaded": "Мережа відновлена: справжні тайли тестової ділянки Рейк'явіка, не приватне місцезнаходження.",
 "60-play-services-unavailable": "Окремий AVD із тимчасово вимкненими GMS: чесне повідомлення й доступний каталог.",
 "61-services-unavailable-details-work": "Деталі працюють на AVD без GMS; стан services відновлено після тесту.",
 "80-controlled-waiting": "Позначений контрольований UI-стан: WAITING/немає даних, не нульовий прогноз.",
 "81-controlled-failed": "Позначений контрольований UI-стан: FAILED, останні дані застарілі, ручне відновлення.",
 "82-controlled-unknown-id": "Позначений контрольований UI-стан: невідомий ID.",
 "83-controlled-empty-dataset": "Позначений контрольований UI-стан: порожня колекція, не нуль пошукових збігів.",
 "01-map-loaded": "SDK snapshot: справжні тайли та 6 демонстраційних маркерів.",
 "02-marker-selected": "SDK snapshot: вибраний блакитний marker та багаторядковий InfoWindow.",
 "03-camera-refocused": "SDK snapshot: кінцевий стан після повторного фокусу; послідовність доводять assertions.",
 "04-filtered-map": "SDK snapshot: один office-1 після Офіс + Активний + 30–50 кВт.",
 "05-empty-map": "SDK snapshot: пошук дав 0, маркери споживачів видалено; це не весь Compose-екран.",
}
lines=["# Остаточний каталог доказів PW04", "", "[Галерея з прев’ю](gallery.html).", "", "Дата: 16.11.2026. A54/SM-A546B, Android 16/API 36; окремий Pixel_6 AVD/API 36 лише для unavailable services. Оригінальні PNG через UiAutomation.takeScreenshot або GoogleMap.snapshot, без ретушування/обрізання. Автоматичні дії й окремий огляд зображень; ручна демонстрація студенткою не заявляється.", "", "Повний облік: [final-checks.md](../../final-checks.md), [результати методів](../../verification-results.json). Реальні приватні координати та ключ не зберігалися. Зелений маркер/accuracy/рух — позначені системні test providers Києва; реальний немокований network fix перевірено без знімка приватної позиції. Мережеві кадри — ізольований EnergyMap над тестовою ділянкою Рейк'явіка. Контрольовані UI-стани мають явний підпис.", ""]
count=0
for manifest in sorted(BASE.glob("*/manifest.json")):
 meta=json.loads(manifest.read_text()); assert meta["passed"], manifest
 folder=manifest.parent
 lines += ["## " + folder.name, "", f"Оригінали й SHA-256/UTC: [manifest.json]({folder.name}/manifest.json).", ""]
 if not meta["files"]:
  lines += ["Цей сценарій не зберігає кадри: реальна приватна позиція залишається на пристрої; adapter-сценарій перевіряє callback-контракт. Успіх підтверджений AndroidJUnitRunner.", ""]; continue
 lines += ["| Кадр | Що доводить |", "|---|---|"]
 for entry in meta["files"]:
  path=folder/entry["file"]; data=path.read_bytes()
  assert hashlib.sha256(data).hexdigest()==entry["sha256"],path
  assert data.startswith(b"\x89PNG\r\n\x1a\n"),path
  stem=path.stem.split("--")[-1]
  caption=CAPTIONS.get(stem)
  if stem.startswith("23-sort-"):
   caption="Сортування " + stem.removeprefix("23-sort-").rsplit("-",1)[0] + (" за спаданням." if stem.endswith("desc") else " за зростанням.")
  assert caption,stem
  lines.append(f"| [{stem}]({folder.name}/{path.name}) | {caption} |")
  count+=1
 lines += [""]
lines += ["## Окремі SDK snapshots і історія", "", "П'ять актуальних [SDK snapshots](../maps-phone/README.md) мають власний manifest і є частково повторними доказами тих самих Maps-сценаріїв. Початкові [сім кадрів етапу 8](../README.md) збережені окремо як історія; це не додаткові унікальні тести.", "", f"У цьому каталозі **{count} оригінальних PNG**; повторні конфігурації мають окремі кадри, тому кількість файлів не дорівнює кількості унікальних сценаріїв. Попередні невдалі й перехідні діагностичні кадри зберігаються лише локально в ignored build/verification. Файл 30 може бути початковим loading-кадром; для видимого зеленого маркера використовувати 31/33.", "", "Динамічні виміри між кадрами можуть змінюватися. Команда прокрутки не гарантує зсуву, якщо весь вміст уже вмістився. Зображення не доводить послідовність взаємодій самостійно: її перевіряють assertions відповідного тесту. Process death, TalkBack, foldable й супутниковий GNSS цими кадрами не підтверджуються.", ""]
(BASE/"README.md").write_text("\n".join(lines),encoding="utf-8")
print("Validated and catalogued",count,"original PNGs")
