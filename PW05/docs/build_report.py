"""Генератор звіту PW05 зі стилями фінального PW04.
Запускати bundled Python. DOCX експортується й перевіряється окремо у Word.
"""
from pathlib import Path
from copy import deepcopy
import re,json,hashlib
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from pypdf import PdfReader

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
REFERENCE=Path('C:/Projects/RPZMP/PW04/docs/PW04_Report.docx')
PDF=Path('C:/Users/tinav/OneDrive/Робочий стіл/Розробка програмного забезпечення мобільних пристроїв/PW_05 RPZMP (1).pdf')
out=HERE/'PW05_Report.docx'
answers=json.loads((HERE/'control-answers.json').read_text(encoding='utf-8-sig'))
reader=PdfReader(PDF)
qa=[]
for lesson,page,group in zip([17,18,19,20],[17,28,40,51],answers):
    text=reader.pages[page-1].extract_text().split('Контрольні питання',1)[1].split('Правильні відповіді',1)[0]
    text=re.sub(r'\s+',' ',text).strip()
    questions=re.findall(r'(?:^|\s)(\d+)\.\s+(.*?)(?=\s\d+\.\s|$)',text)
    assert len(questions)==12,(lesson,questions)
    qa.append((lesson,[(int(n),q,a) for (n,q),a in zip(questions,group)]))
control='# Контрольні питання PW05\n\n'
for lesson,group in qa:
    control+=f'## Заняття {lesson}\n\n'
    for n,q,a in group:control+=f'### {n}. {q}\n\n{a}\n\n'
(HERE/'control-questions.md').write_text(control,encoding='utf-8')
body=(HERE/'report-body.md').read_text(encoding='utf-8-sig')
md=body+'\n'+control.replace('# Контрольні питання PW05\n\n','')
# Реальні оригінальні кадри, без обрізання або домальовування.
manifest=json.loads((HERE/'evidence/galaxy-a54-api36/manifest.json').read_text(encoding='utf-8'))
figures=[(item['file'],item['caption']) for item in manifest]
md+='\n## 12 Оригінальні скриншоти фізичного пристрою\n\nSamsung Galaxy A54 SM-A546B, Android 16/API 36, 19.11.2026. Portrait 1080×2340, 450 dpi; landscape 2340×1080, шрифт 150%. Решта кадрів: шрифт 100%. Енергетичні графіки демонстраційні, сенсорні показники фізичні. Повторний показ після відкладання підтверджено через 527,953 с. Зображення не обрізані та не домальовані.\n\n'
for i,(name,caption) in enumerate(figures,1):
    md+=f'Рисунок {i} — {caption}\n\n![Рисунок {i}](evidence/galaxy-a54-api36/{name})\n\n'
md += "\n## 13 Джерела\n\nМетодичні вказівки PW_05 RPZMP (1).pdf, КПІ ім. Ігоря Сікорського, 2026, 145 сторінок.\n\nAndroid Developers: https://developer.android.com/develop/background-work/services/alarms ; https://developer.android.com/reference/android/app/AlarmManager ; https://developer.android.com/develop/ui/compose/graphics/draw/overview .\n"
(HERE/'PW05_Report.md').write_text(md,encoding='utf-8')
d=Document(REFERENCE)
title=[deepcopy(p._p) for p in d.paragraphs[:15]]
body_el=d._element.body
for child in list(body_el):
    if child.tag!=qn('w:sectPr'):body_el.remove(child)
# Старі зображення й посилання PW04 не потрапляють у новий пакет.
for rid,rel in list(d.part.rels.items()):
    if rel.reltype.endswith('/image') or rel.reltype.endswith('/hyperlink'):
        d.part.drop_rel(rid)
for p in title:body_el.insert(len(body_el)-1,p)
d.paragraphs[3].text='Звіт з практичної роботи 5'
d.paragraphs[3].style=d.styles['Title']
d.paragraphs[5].text='Практичні заняття 17–20'
d.paragraphs[6].text='Тема роботи «Сенсори, локальні сповіщення, графіки та власні компоненти Jetpack Compose»'
p=d.paragraphs[11]
for child in list(p._p):
    if child.tag!=qn('w:pPr'):p._p.remove(child)
hyper=OxmlElement('w:hyperlink')
rid=d.part.relate_to('https://github.com/Christ1naa/RPZMP/tree/feature/pw05/PW05','http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink',is_external=True)
hyper.set(qn('r:id'),rid)
run=OxmlElement('w:r');props=OxmlElement('w:rPr');color=OxmlElement('w:color');color.set(qn('w:val'),'000000');props.append(color);run.append(props)
t=OxmlElement('w:t');t.text='https://github.com/Christ1naa/RPZMP/tree/feature/pw05/PW05';run.append(t);hyper.append(run);p._p.append(hyper)
for p in d.paragraphs[:15]:p.alignment=WD_ALIGN_PARAGRAPH.CENTER
d.add_page_break()
p=d.add_paragraph('Зміст');p.style=d.styles['Title']
p=d.add_paragraph()
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'TOC \\o "1-2" \\h \\z \\u');p._p.append(fld)
d.add_page_break()
# Титульні відомості вже наведені на окремій першій сторінці.
started=False
lines=body.splitlines();index=0
while index<len(lines):
    line=lines[index]
    if line.startswith('## 1 '):started=True
    if not started:index+=1;continue
    if line.startswith('|'):
        rows=[]
        while index<len(lines) and lines[index].startswith('|'):
            cells=[v.strip() for v in lines[index].strip().strip('|').split('|')]
            if not all(re.fullmatch(r'[: -]+',v) for v in cells):rows.append(cells)
            index+=1
        table=d.add_table(rows=0,cols=len(rows[0]));table.style='Table Grid'
        for n,cells in enumerate(rows):
            row=table.add_row()
            for cell,value in zip(row.cells,cells):
                cell.text=value
                for r in cell.paragraphs[0].runs:r.font.size=Pt(11);r.bold=n==0
            if n==0:
                repeat=OxmlElement('w:tblHeader');row._tr.get_or_add_trPr().append(repeat)
            cant=OxmlElement('w:cantSplit');row._tr.get_or_add_trPr().append(cant)
        d.add_paragraph()
        continue
    if line.startswith('## '):d.add_heading(line[3:],level=1)
    elif line.strip():
        paragraph=d.add_paragraph(line)
        paragraph.paragraph_format.widow_control=True
    index+=1
for lesson,group in qa:
    p=d.add_heading(f'Заняття {lesson}',level=2)
    for n,q,a in group:
        p=d.add_paragraph()
        p.paragraph_format.keep_with_next=True
        p.add_run(f'{n}. {q}').bold=True
        d.add_paragraph(a)
d.add_page_break()
d.add_heading('12 Оригінальні скриншоти фізичного пристрою',level=1)
d.add_paragraph('Samsung Galaxy A54 SM-A546B, Android 16/API 36, 19.11.2026. Портретні кадри 1080×2340 при 450 dpi; landscape 2340×1080 зі шрифтом 150%. Решта кадрів зі шрифтом 100%. Сенсори фізичні, енергетична історія демонстраційна. Усі PNG оригінальні, без обрізання або домальовування. Підписи пояснюють стан, а не гарантують точність фізичного виміру.')
from PIL import Image
items=[(i,name,caption) for i,(name,caption) in enumerate(figures,1)]
portraits=[item for item in items if Image.open(HERE/'evidence/galaxy-a54-api36'/item[1]).height>Image.open(HERE/'evidence/galaxy-a54-api36'/item[1]).width]
landscapes=[item for item in items if item not in portraits]
for start in range(0,len(portraits),2):
    if start>0:d.add_page_break()
    table=d.add_table(rows=1,cols=2)
    table.autofit=False
    for cell in table.rows[0].cells:cell.width=Inches(3.1)
    for cell,(i,name,caption) in zip(table.rows[0].cells,portraits[start:start+2]):
        paragraph=cell.paragraphs[0];paragraph.alignment=WD_ALIGN_PARAGRAPH.CENTER
        paragraph.add_run().add_picture(str(HERE/'evidence/galaxy-a54-api36'/name),width=Inches(2.94))
        caption_p=cell.add_paragraph(f'Рисунок {i} — {caption}')
        caption_p.alignment=WD_ALIGN_PARAGRAPH.CENTER
        for r in caption_p.runs:r.font.size=Pt(10)
    cant=OxmlElement('w:cantSplit');table.rows[0]._tr.get_or_add_trPr().append(cant)
if landscapes:
    d.add_page_break()
    for i,name,caption in landscapes:
        paragraph=d.add_paragraph();paragraph.alignment=WD_ALIGN_PARAGRAPH.CENTER
        paragraph.add_run().add_picture(str(HERE/'evidence/galaxy-a54-api36'/name),width=Inches(6.15))
        paragraph=d.add_paragraph(f'Рисунок {i} — {caption}');paragraph.alignment=WD_ALIGN_PARAGRAPH.CENTER
        for r in paragraph.runs:r.font.size=Pt(10)
d.add_page_break()

d.add_heading('13 Джерела', level=1)
for text in [
    'Методичні вказівки PW_05 RPZMP (1).pdf, 145 сторінок, КПІ ім. Ігоря Сікорського, 2026. Вимоги: с. 8–9, 15–17, 22, 27–28, 33–34, 39–40, 45–46, 50–51; варіант: с. 145.',
    'Android Developers. Schedule alarms. https://developer.android.com/develop/background-work/services/alarms',
    'Android Developers. AlarmManager API. https://developer.android.com/reference/android/app/AlarmManager',
    'Android Developers. Graphics in Compose. https://developer.android.com/develop/ui/compose/graphics/draw/overview'
]:d.add_paragraph(text)
# Колонтитули джерела мають називати поточну роботу.
for section in d.sections:
    for container in [section.header,section.first_page_header,section.even_page_header,section.footer,section.first_page_footer,section.even_page_footer]:
        for text in container._element.iter(qn('w:t')):
            if text.text:text.text=text.text.replace('Практична робота 4','Практична робота 5').replace('PW04','PW05')
# Чорний редагований текст включно зі стилями, титулом, TOC і гіперпосиланням.
for style in d.styles:
    if hasattr(style,'font'):style.font.color.rgb=RGBColor(0,0,0)
for part in [d.part]+[r.target_part for r in d.part.rels.values() if not r.is_external and r.reltype.endswith(('/header','/footer'))]:
    for r in part.element.iter(qn('w:r')):
        props=r.find(qn('w:rPr'))
        if props is None:props=OxmlElement('w:rPr');r.insert(0,props)
        color=props.find(qn('w:color'))
        if color is None:color=OxmlElement('w:color');props.append(color)
        color.attrib.clear();color.set(qn('w:val'),'000000')
d.core_properties.title='Практична робота 5 Система прогнозування споживання електроенергії'
d.core_properties.author='Вахрушева Христина Костянтинівна'
d.core_properties.comments=''
d.save(out)
spec=f"""# Стиль звіту PW05

Джерело: {REFERENCE.as_posix()}
SHA-256: {hashlib.sha256(REFERENCE.read_bytes()).hexdigest()}
Незмінне джерело: жодної операції запису до PW04.
Одна секція Letter 21,59×27,94 см. Поля верх/низ 2,032 см, ліве 2,667, праве 2,159.
Normal Times New Roman 12 pt; Heading 1 15 pt; Heading 2 13 pt; Title 19 pt.
Збережено стилі й параметри секції. У колонтитулах замінено номер роботи 4 на 5. Текст чорний.
Титульний блок клонується зі звіту PW04; замінено номер, заняття, тему і гіперпосилання.
Увесь старий зміст та всі source image/hyperlink relationships вилучені.
Дозволено нову кількість сторінок, новий TOC, нові абзаци й лише PNG доказів PW05.
Джерело PDF PW04 використане лише для стилю, не як доказ виконання PW05.
Канонічний render_docx.py недоступний через відсутність LibreOffice у Windows runtime.
Альтернатива: Word 16 COM → PDF → bundled Poppler PNG, перевірка всіх фінальних сторінок.
"""
(HERE/'report-style.md').write_text(spec,encoding='utf-8')
print('Written',out,'questions',sum(len(g) for _,g in qa),'figures',len(figures))

