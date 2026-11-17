"""Build PW04 report from reviewed Markdown, original PNGs and a retained PW03 style reference.

Run with the bundled document Python runtime. Export and page review are separate steps.
"""
from pathlib import Path
from copy import deepcopy
import hashlib
import json
import re
import textwrap
import zipfile
from lxml import etree
import xml.etree.ElementTree as ET

from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
REFERENCE = HERE / 'report-template.docx'
SOURCE = HERE / 'PW04_Report.md'
WORK = ROOT / 'build/report-work'
WORK.mkdir(parents=True, exist_ok=True)
FINAL = HERE / 'evidence/final-checks'


def enforce_black_text(path):
    """Use explicit black for all Word text and styles, independent of theme colors.

    Image bytes and table fills/borders are preserved. Character styles, TOC,
    field text, equations, headers and footers are covered as well as body text.
    """
    with zipfile.ZipFile(path) as archive:
        members = [(info, archive.read(info.filename)) for info in archive.infolist()]
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
          'm': 'http://schemas.openxmlformats.org/officeDocument/2006/math'}
    def black(properties):
        node = properties.find(qn('w:color'))
        if node is None:
            node = OxmlElement('w:color'); properties.append(node)
        node.attrib.clear(); node.set(qn('w:val'), '000000')
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as archive:
        for info, data in members:
            if info.filename.startswith('word/') and info.filename.endswith('.xml'):
                tree = etree.fromstring(data)
                for node in tree.xpath('//w:color', namespaces=ns):
                    node.attrib.clear(); node.set(qn('w:val'), '000000')
                for parent in tree.xpath('//w:style | //w:r | //m:r | //w:rPrDefault', namespaces=ns):
                    properties = parent.find(qn('w:rPr'))
                    if properties is None:
                        properties = OxmlElement('w:rPr')
                    else:
                        parent.remove(properties)
                    if parent.tag == qn('w:style'):
                        index = next((i for i, child in enumerate(parent) if child.tag in
                                      [qn('w:tblPr'), qn('w:trPr'), qn('w:tcPr'), qn('w:tblStylePr')]), len(parent))
                    else:
                        index = 1 if parent.tag == qn('m:r') and parent.find(qn('m:rPr')) is not None else 0
                    parent.insert(index, properties)
                    black(properties)
                data = etree.tostring(tree, xml_declaration=True, encoding='UTF-8', standalone=True)
            archive.writestr(info, data)


def shot(folder, stem):
    found = list((FINAL / folder).glob('*' + stem + '.png'))
    if len(found) != 1:
        raise ValueError((folder, stem, found))
    return found[0]


def general(stem):
    return shot('general-valid', stem)


def sdk(stem):
    return HERE / 'evidence/maps-phone' / (stem + '.png')


# Each item records the true capture route; originals are never cropped or retouched.
FIGURES = [
    ('Моніторинг і каталог', [shot('general-absent', '01-monitor-live'), shot('denyTwiceThenOpenOwnSettingsAndReturn', '13-permission-denied-app')],
     'а — обидва джерела LIVE, навантаження й добовий прогноз; б — каталог MainActivity зі справжньою Maps після відмови у location-дозволі. Дані споживачів демонстраційні.'),
    ('Картка та окремий екран деталей', [shot('general-absent', '04-filtered-office'), shot('general-absent', '05-details-top')],
     'а — office-1 після комбінованого фільтра; б — деталі цього об’єкта: опис, статус, потужність, режим, прогноз, дата опису й час виміру. MainActivity на A54.'),
    ('Незалежні збої потоків', [general('20-meter-retrying'), general('20-model-retrying')],
     'а — RETRYING лічильника; б — RETRYING моделі при працездатному лічильнику. Обидва кадри справжньої MainActivity, останні узгоджені дані позначено застарілими.'),
    ('Автоматичне відновлення джерел', [general('21-meter-recovered'), general('21-model-recovered')],
     'а — відновлення лічильника; б — відновлення моделі. Обидва джерела LIVE; послідовність збій → відновлення перевірено assertions, значення між кадрами змінюються.'),
    ('Фільтри та валідація', [shot('general-absent', '03-filter-panel'), general('22-invalid-range')],
     'а — Офіс + Активний + 30–50 кВт; б — помилка min=50 > max=10 після введення меж. У другому кадрі частина полів прокручена; введені значення перевірені тестом.'),
    ('Справжні маркери та InfoWindow', [sdk('01-map-loaded'), sdk('02-marker-selected')],
     'а — огляд шести об’єктів; б — вибраний блакитний office-2 і багаторядковий InfoWindow. GoogleMap.snapshot у SDK test host, production EnergyMap зі сталими тестовими показниками; це не весь MainActivity.'),
    ('Фільтрація маркерів', [sdk('04-filtered-map'), sdk('05-empty-map')],
     'а — один office-1 після Офіс + Активний + 30–50; б — після reset і пошуку «Неіснуючий» маркерів немає. SDK snapshots короткого слоту; кількість карток і скидання перевірено окремими assertions.'),
    ('Системний вибір точності', [shot('precisePermissionThroughSystemDialog', '10-permission-precise-dialog'), shot('approximatePermissionThroughSystemDialog', '11-permission-approximate-dialog')],
     'а — Precise; б — Approximate у справжньому діалозі Samsung A54. Після надання приблизного дозволу тест підтвердив coarse=true, fine=false. Приватні координати не знімалися.'),
    ('Відмова і налаштування дозволів', [shot('denyTwiceThenOpenOwnSettingsAndReturn', '14-permission-permanently-denied-app'), shot('denyTwiceThenOpenOwnSettingsAndReturn', '15-own-app-settings')],
     'а — PW04 після двох відмов і третього запиту без нового діалогу; б — власний App info. Сталість відмови й повернення доводить сценарій тесту, не лише статичний кадр.'),
    ('Вимкнена Location і відновлення позиції', [shot('location-disabled-settings', '17-location-disabled'), shot('synthetic-position-ui', '33-providers-recovered')],
     'а — системне визначення позиції вимкнене; б — зелений маркер після повернення test providers, ±7 м, явний підпис «тестова позиція». Синтетичні координати Києва, не фізичний GNSS.'),
    ('Оновлення та очищення координат', [shot('synthetic-position-ui', '31-labelled-position-updated'), shot('synthetic-position-ui', '32-providers-disabled')],
     'а — другий синтетичний fix, ±6 м і зелений маркер; б — GPS і network test providers вимкнено, поточна позиція очищена. Production LocationSource і Maps, явно тестова позиція.'),
    ('Неправильний Maps ключ і повтор', [shot('invalid-key-retry', '40-invalid-key-map-failed'), shot('invalid-key-retry', '41-invalid-key-map-retrying')],
     'а — timeout у MainActivity з навмисно неправильним ключем; б — нове завантаження після «Повторити мапу». Значення справжнього ключа не показується, список і деталі залишаються доступними.'),
    ('Втрата і відновлення мережі', [shot('network-recovery', '50-network-off-map-failed'), shot('network-recovery', '52-network-restored-map-loaded')],
     'а — Wi-Fi/mobile data справді вимкнено, показано timeout; б — після повтору й відновлення мережі завантажено тайли Рейк’явіка. Ізольований EnergyMap без приватної позиції, не MainActivity. Початкові налаштування відновлено.'),
    ('Недоступні сервіси та контрольований стан помилки', [shot('services-unavailable-emulator', '60-play-services-unavailable'), shot('controlled-states', '81-controlled-failed')],
     'а — Pixel_6 AVD з тимчасово вимкненими Play services; б — A54, production-компонент із явно позначеним контрольованим FAILED-станом. Другий кадр не є природним збоєм вимірювального обладнання.'),
    ('Зміна орієнтації', [general('24-landscape'), general('25-portrait-restored')],
     'а — фактичний landscape Configuration; б — повернення portrait зі збереженим запитом і прихованою мапою. Список навмисно прокручено. Це поворот Activity, не process death.'),
    ('Збільшений системний шрифт', [shot('system-large-font', '26-system-large-font'), shot('system-large-font', '28-large-font-filter-done')],
     'а — список прокручується до останнього об’єкта; б — доступні «Скинути» та «Готово». Системний font_scale=1.6 на A54, після тесту відновлено початкове значення.'),
]


def expand_source():
    text = SOURCE.read_text(encoding='utf-8')
    # The exact teacher questions are extracted once from the supplied PDF, not invented.
    from pypdf import PdfReader
    pdf = next((ROOT.parent / '.codex-attachments').rglob('PW_04 RPZMP.pdf'))
    pages = PdfReader(pdf).pages
    answer_sections = re.split(r'^## Заняття[^\n]*\n', (HERE / 'control-questions.md').read_text(encoding='utf-8'), flags=re.M)[1:]
    blocks = []
    for idx, (lesson, page_numbers) in enumerate([(13, [14]), (14, [26]), (15, [37]), (16, [49, 50])]):
        raw = '\n'.join(pages[n-1].extract_text() for n in page_numbers)
        raw = raw.split('Контрольні питання', 1)[1].split('Правильні відповіді', 1)[0]
        raw = re.sub(r'^\s*(?:49|50)\s*$', '', raw, flags=re.M)
        questions = re.findall(r'(?:^|\n)\s*(\d+)\.\s+(.*?)(?=\n\s*\d+\.\s|\Z)', raw, flags=re.S)
        answers = re.findall(r'^(\d+)\. (.+)$', answer_sections[idx], flags=re.M)
        assert len(questions) == len(answers) == 12, (lesson, questions)
        blocks.append(f'### 12.{idx+1} Заняття {lesson}\n')
        for (n, question), (an, answer) in zip(questions, answers):
            assert n == an
            blocks.append(f'**{n}. {" ".join(question.split())}**\n\n{answer}\n')
    text = re.sub(r'(## 12 Відповіді на контрольні питання\n).*?(?=\n## 13 )',
                  lambda m: m[1] + '\n' + '\n'.join(blocks), text, flags=re.S)
    figure_source = ['Оригінали отримані 16.11.2026 на Samsung A54/API 36; окремо позначено AVD, SDK hosts та контрольовані UI-стани. Динамічні виміри між кадрами можуть змінюватися. Масштаб змінено лише для розміщення на сторінці, вміст оригіналів не редагувався.\n']
    for n, (title, images, caption) in enumerate(FIGURES, 1):
        figure_source += [f'### Рисунок {n} {title}\n']
        figure_source += [f'![Рисунок {n} {i+1}]({image.relative_to(HERE).as_posix()})\n' for i, image in enumerate(images)]
        figure_source += [f'Рисунок {n} — {caption}\n']
    text = re.sub(r'(## Додаток А Знімки екрана застосунку\n).*?(?=\n## Додаток Б)',
                  lambda m: m[1] + '\n' + '\n'.join(figure_source), text, flags=re.S)
    SOURCE.write_text(text, encoding='utf-8')
    return text


def inline(p, text):
    for part in re.split(r'(\*\*.*?\*\*|`[^`]+`)', text):
        if not part:
            continue
        run = p.add_run(part[2:-2] if part.startswith('**') else part.strip('`'))
        if part.startswith('**'):
            run.bold = True
        if part.startswith('`'):
            run.font.name = 'Consolas'
            run.font.size = Pt(10)


def build():
    text = expand_source()
    doc = Document(REFERENCE)
    # Source-derived page system and styles retained; all PW03 body slots are replaced.
    body = doc._element.body
    for element in list(body):
        if element.tag != qn('w:sectPr'):
            body.remove(element)
    for relid, rel in list(doc.part.rels.items()):
        if rel.reltype.endswith('/image'):
            doc.part.drop_rel(relid)
    for section in doc.sections:
        for p in section.header.paragraphs:
            for run in p.runs:
                run.text = run.text.replace('робота 3', 'робота 4')
    for name in ['Normal', 'Title', 'Subtitle', 'Heading 1', 'Heading 2', 'Heading 3', 'Caption', 'Contents Heading']:
        style = doc.styles[name]
        style.font.name = 'Times New Roman'
        style.font.color.rgb = RGBColor(0, 0, 0)
        color = style.element.get_or_add_rPr().find(qn('w:color'))
        if color is not None:
            for attr in list(color.attrib):
                if 'theme' in attr.lower():
                    del color.attrib[attr]
    normal = doc.styles['Normal'].paragraph_format
    normal.widow_control = True
    doc.styles['Caption'].font.bold = False

    def para(value, style=None):
        p = doc.add_paragraph(style=style)
        inline(p, value)
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT if style or value.startswith('[') else WD_ALIGN_PARAGRAPH.JUSTIFY
        return p

    cover, content = text.split(':::body', 1)
    cover_rows = [p.strip() for p in cover.split('\n\n') if p.strip()]
    title = cover_rows.pop(0).removeprefix('# ')
    for i, row in enumerate(cover_rows):
        if i == 3:
            p = doc.add_paragraph(title, 'Title')
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_before = Pt(35)
        if row.startswith('https://github.com/'):
            from docx.opc.constants import RELATIONSHIP_TYPE
            p = doc.add_paragraph()
            link = OxmlElement('w:hyperlink')
            link.set(qn('r:id'), doc.part.relate_to(row, RELATIONSHIP_TYPE.HYPERLINK, is_external=True))
            run = OxmlElement('w:r'); properties = OxmlElement('w:rPr')
            colour = OxmlElement('w:color'); colour.set(qn('w:val'), '000000'); properties.append(colour)
            underline = OxmlElement('w:u'); underline.set(qn('w:val'), 'single'); properties.append(underline)
            run.append(properties); value = OxmlElement('w:t'); value.text = row; run.append(value)
            link.append(run); p._p.append(link)
        else:
            p = para(row)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if row.startswith(('Виконала', 'Вахрушева', 'Перевірив', 'Олексій', 'https://github.com/')):
            p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        if row.startswith('Виконала'):
            p.paragraph_format.space_before = Pt(40)
        if row == 'Просунутий рівень':
            for run in p.runs:
                run.bold = True
        if row == 'Київ 2026':
            p.paragraph_format.space_before = Pt(26)
    doc.add_page_break()
    doc.add_paragraph('Зміст', 'Contents Heading')
    toc_p = doc.add_paragraph()
    field = OxmlElement('w:fldSimple')
    field.set(qn('w:instr'), 'TOC \\o "1-2" \\h \\z \\u')
    toc_p._p.append(field)
    doc.add_page_break()

    def table(rows):
        cols = len(rows[0])
        widths = {2: [2.15, 4.45], 3: [1.55, 3.1, 1.95], 4: [2.85, 1.75, 1.0, 1.0]}[cols]
        if rows[0][0] == 'Набір':
            widths = [3.0, 1.4, 2.2]
        t = doc.add_table(rows=0, cols=cols)
        t.autofit = False
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        for col, width in zip(t.columns, widths):
            col.width = Inches(width)
        borders = OxmlElement('w:tblBorders')
        for edge in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
            node = OxmlElement('w:' + edge)
            for k, v in [('val', 'single'), ('sz', '4'), ('color', 'D9D9D9')]:
                node.set(qn('w:' + k), v)
            borders.append(node)
        t._tbl.tblPr.append(borders)
        margins = OxmlElement('w:tblCellMar')
        padding = '45' if len(rows) == 7 else '80'
        for edge, value in [('top', padding), ('bottom', padding), ('left', '90'), ('right', '90')]:
            node = OxmlElement('w:' + edge); node.set(qn('w:w'), value); node.set(qn('w:type'), 'dxa'); margins.append(node)
        t._tbl.tblPr.append(margins)
        for idx, row in enumerate(rows):
            r = t.add_row()
            r._tr.get_or_add_trPr().append(OxmlElement('w:cantSplit'))
            if idx == 0:
                r._tr.get_or_add_trPr().append(OxmlElement('w:tblHeader'))
            for j, value in enumerate(row):
                cell = r.cells[j]; cell.width = Inches(widths[j]); cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
                p = cell.paragraphs[0]; p.paragraph_format.line_spacing = 1.05; p.paragraph_format.space_after = Pt(0)
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER if idx == 0 or (cols == 4 and j >= 2) or (rows[0][0] == 'Набір' and j == 1) else WD_ALIGN_PARAGRAPH.LEFT
                inline(p, value)
                for run in p.runs:
                    run.font.size = Pt(10.5); run.bold = idx == 0
                if idx == 0:
                    shade = OxmlElement('w:shd'); shade.set(qn('w:fill'), 'E8EDF3'); cell._tc.get_or_add_tcPr().append(shade)
        doc.add_paragraph().paragraph_format.space_after = Pt(0)

    snippets = {
        'engine': ('Лістинг 1 — Перетворення потоку у TelemetryEngine.kt', 'data/TelemetryEngine.kt', '        val meter = resilient {', '        val forecast = resilient {'),
        'vm': ('Лістинг 2 — Гарячий потік у ForecastViewModel.kt', 'ui/ForecastViewModel.kt', '    val telemetry = restart.flatMapLatest', '    val uiState = combine'),
        'focus': ('Лістинг 3 — Контракт фокусу у MapPresentation.kt', 'ui/MapPresentation.kt', '    val position = readings.firstOrNull', '\n}'),
    }

    def code(lines, title=None):
        if title:
            p = para(title, 'Caption'); p.paragraph_format.keep_with_next = True
        for idx, line in enumerate(lines):
            # Literal code: PowerShell continuation backticks must survive Markdown parsing.
            p = doc.add_paragraph(line, 'Code')
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.keep_with_next = idx < len(lines)-1

    def figure(n, first=False):
        title, paths, caption = FIGURES[n-1]
        if not first:
            doc.add_page_break()
        p = para(title, 'Heading 3')
        # Figure titles stay out of TOC; only chapter/section headings are listed.
        if n == 7:
            for index, path in enumerate(paths):
                p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                im = Image.open(path); w, h = im.size; height = 2.55
                run = p.add_run(); run.add_picture(str(path), height=Inches(height))
                p.paragraph_format.space_after = Pt(5); p.paragraph_format.keep_with_next = True
                label = para('а' if index == 0 else 'б', 'Caption')
                label.alignment = WD_ALIGN_PARAGRAPH.CENTER
                label.paragraph_format.space_after = Pt(4)
                label.paragraph_format.keep_with_next = True
        elif n == 15:
            captions = [
                'Рисунок 15, а — фактичний landscape Configuration зі збереженим запитом і прихованою мапою. Список навмисно прокручено.',
                'Рисунок 15, б — повернення portrait зі збереженим запитом і прихованою мапою. Збільшений кадр для читання полів. Це поворот Activity, не process death.',
            ]
            for index, (path, height) in enumerate(zip(paths, [3.0, 6.5])):
                if index == 1:
                    doc.add_page_break()
                    para('Зміна орієнтації продовження', 'Heading 3')
                p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                shape = p.add_run().add_picture(str(path), height=Inches(height))
                shape._inline.docPr.set('descr', captions[index])
                p.paragraph_format.space_after = Pt(3); p.paragraph_format.keep_with_next = True
                label = para('а' if index == 0 else 'б', 'Caption')
                label.alignment = WD_ALIGN_PARAGRAPH.CENTER
                label.paragraph_format.space_after = Pt(3)
                label.paragraph_format.keep_with_next = True
                p = para(captions[index], 'Caption')
                p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            return
        else:
            p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for idx, path in enumerate(paths):
                if idx: p.add_run('    ')
                shape = p.add_run().add_picture(str(path), height=Inches(5.65))
                shape._inline.docPr.set('descr', f'Рисунок {n}, {"а" if idx == 0 else "б"}: {caption}')
            p.paragraph_format.keep_with_next = True
            from docx.enum.text import WD_TAB_ALIGNMENT
            labels = doc.add_paragraph()
            labels.paragraph_format.tab_stops.add_tab_stop(Inches(1.9), WD_TAB_ALIGNMENT.CENTER)
            labels.paragraph_format.tab_stops.add_tab_stop(Inches(4.7), WD_TAB_ALIGNMENT.CENTER)
            labels.add_run('\tа\tб')
            labels.paragraph_format.space_after = Pt(2)
            labels.paragraph_format.keep_with_next = True
        p = para(f'Рисунок {n} — {caption}', 'Caption'); p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    lines = content.strip().splitlines(); idx = 0; first_figure = True; in_questions = False
    while idx < len(lines):
        line = lines[idx].strip()
        if not line:
            idx += 1; continue
        if line.startswith('### Рисунок '):
            n = int(re.match(r'### Рисунок (\d+)', line)[1]); figure(n, first_figure); first_figure = False
            idx += 1
            while idx < len(lines) and not lines[idx].startswith(('### Рисунок ', '## ')):
                idx += 1
            continue
        if line.startswith('## '):
            value = line[3:]
            in_questions = value.startswith('12 ')
            if value.startswith(('11 ', '12 ', '13 ', 'Додаток')):
                doc.add_page_break()
            doc.add_paragraph(value, 'Heading 1'); idx += 1; continue
        if line.startswith('### '):
            p = doc.add_paragraph(line[4:], 'Heading 2')
            if line.startswith('### 12.') and not line.startswith('### 12.1'):
                p.paragraph_format.page_break_before = True
            idx += 1; continue
        if line.startswith(':::listing '):
            title, source, start, end = snippets[line.split()[-1]]
            raw = (ROOT / 'app/src/main/java/ua/kpi/energyforecast/pw04' / source).read_text()
            raw = start + raw.split(start, 1)[1].split(end, 1)[0]
            code(textwrap.dedent(raw).rstrip().splitlines(), title); idx += 1; continue
        if line.startswith(':::equation '):
            p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            math = OxmlElement('m:oMathPara'); om = OxmlElement('m:oMath'); run = OxmlElement('m:r'); t = OxmlElement('m:t')
            t.text = 'E_day = P_measured_kW × hoursPerDay × demandFactor'; run.append(t); om.append(run); math.append(om); p._p.append(math)
            idx += 1; continue
        if line.startswith('```'):
            idx += 1; block = []
            while idx < len(lines) and not lines[idx].startswith('```'):
                block.append(lines[idx]); idx += 1
            code(block, 'Лістинг 4 — Команди перевірки у PowerShell'); idx += 1; continue
        if line.startswith('|'):
            rows = []
            while idx < len(lines) and lines[idx].strip().startswith('|'):
                row = [cell.strip() for cell in lines[idx].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell) for cell in row): rows.append(row)
                idx += 1
            table(rows); continue
        p = para(line)
        if in_questions:
            p.paragraph_format.line_spacing = 1.05
            p.paragraph_format.space_after = Pt(3)
            for r in p.runs: r.font.size = Pt(11.5)
        if line.startswith('Таблиця '):
            p.paragraph_format.keep_with_next = True
            for r in p.runs: r.bold = True; r.font.size = Pt(10.5)
        if line.startswith('**'):
            p.paragraph_format.keep_with_next = True
        idx += 1

    props = doc.core_properties
    props.title = 'Звіт з практичної роботи 4 EnergyForecast'
    props.subject = 'Flow Coroutines LazyColumn фільтрація сортування мапи та геолокація'
    props.author = 'Вахрушева Христина Костянтинівна'
    props.keywords = 'PW04, варіант 1, просунутий рівень, Kotlin, Compose, Maps'
    props.comments = 'Результати перевірок і знімки 16.11.2026; оформлення виправлено 17.11.2026 за незалежним аудитом.'
    update = OxmlElement('w:updateFields'); update.set(qn('w:val'), 'true'); doc.settings.element.append(update)
    doc.save(HERE / 'PW04_Report.docx')
    enforce_black_text(HERE / 'PW04_Report.docx')
    manifest = []
    for n, (title, paths, caption) in enumerate(FIGURES, 1):
        manifest.append({'figure': n, 'title': title, 'caption': caption, 'files': [
            {'path': p.relative_to(HERE).as_posix(), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in paths]})
    (HERE / 'report-figures.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print('DOCX built:', len(doc.paragraphs), 'paragraphs;', len(doc.inline_shapes), 'original images; 48 control answers')


def evidence_package():
    """Package publication-safe evidence, not APKs or unfiltered machine logs."""
    private = ROOT / 'build/verification/final'
    staging = WORK / 'evidence-package'; staging.mkdir(exist_ok=True)
    counts = json.loads((HERE / 'verification-results.json').read_text())
    local = []
    for path in (ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        for case in ET.parse(path).getroot().findall('testcase'):
            local.append({k: case.get(k) for k in ['classname', 'name', 'time']} | {'passed': not any(case.find(t) is not None for t in ['failure', 'error', 'skipped'])})
    assert len(local) == 54 and all(c['passed'] for c in local)
    (staging / 'unit-cases.json').write_text(json.dumps(local, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    source_files = sorted(p for p in (ROOT / 'app/src').rglob('*') if p.is_file())
    source_files += [ROOT / 'app/build.gradle.kts', ROOT / 'build.gradle.kts', ROOT / 'gradle/libs.versions.toml']
    source = [{'file': p.relative_to(ROOT).as_posix(), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in source_files]
    snapshot = {'date': '2026-11-17', 'scope': 'Program sources at report revision; not a retroactive source-to-APK attestation',
                'files': source, 'aggregate_sha256': hashlib.sha256(json.dumps(source, sort_keys=True).encode()).hexdigest()}
    (staging / 'source-snapshot.json').write_text(json.dumps(snapshot, indent=2)+'\n')
    output = HERE / 'PW04_Report_Evidence.zip'
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((HERE / 'evidence').rglob('*')):
            if path.is_file(): archive.write(path, 'docs/' + path.relative_to(HERE).as_posix())
        for path in sorted(HERE.glob('*.md')) + sorted(HERE.glob('*.json')):
            archive.write(path, 'docs/' + path.name)
        comment = HERE / 'submission-comment.txt'
        if comment.exists():
            archive.write(comment, 'docs/' + comment.name)
        for path in staging.glob('*.json'): archive.write(path, 'verification/' + path.name)
        names = [r['scenario'] + '.txt' for r in counts['runs']]
        names += ['network-detached-runner.txt', 'services-unavailable-runner.txt']
        for name in sorted(set(names)):
            path = private / name
            if not path.exists(): continue
            raw = path.read_text(errors='replace')
            # Allowlist only test identity/status/count fields; no stack trace, shell, network address or SDK logs.
            clean = [line for line in raw.splitlines() if re.match(r'INSTRUMENTATION_STATUS: (class|test|numtests|current|pw04_real_location_provider|pw04_real_location_is_mock|pw04_real_location_accuracy_present)=|INSTRUMENTATION_STATUS_CODE:|^OK \(|^Time:', line)]
            archive.writestr('verification/android/' + name, '\n'.join(clean) + '\n')
        archive.writestr('README.txt', 'PW04 — редакція звіту 17.11.2026, докази виконань 16.11.2026\n\ndocs/evidence: 89 оригінальних PNG, manifests і пояснення.\nverification: 54 локальні випадки, очищені AndroidJUnitRunner записи, source-snapshot.json.\nНові тести під час створення звіту не виконувалися. APK і API-ключів немає.\nSource snapshot описує поточні джерела; старі запуски не мають повної ретроспективної прив’язки до робочого дерева.\n')
    print('Evidence package:', output.name, round(output.stat().st_size/1024/1024, 2), 'MiB')


if __name__ == '__main__':
    build()
    evidence_package()
