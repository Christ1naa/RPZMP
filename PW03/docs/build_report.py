"""Build the Ukrainian PW03 report from its reviewed Markdown source."""
from pathlib import Path
import re, json
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

HERE=Path(__file__).resolve().parent
SOURCE=HERE/'report.md'
DEST=HERE/'PW03_Report.docx'
SHOTS=HERE/'screenshots/2026-11-14'
manifest=json.loads((SHOTS/'manifest.json').read_text(encoding='utf-8'))
images={e['id']:e for e in manifest['screenshots']}
fig_ids=[1,2,3,7,8,9,12,15,18,48,30,33,42,24,44,46,47,36,37,38,40,43,35]

doc=Document()
sec=doc.sections[0]
sec.page_width=Inches(8.5)
sec.page_height=Inches(11)
sec.top_margin=sec.bottom_margin=Inches(.8)
sec.left_margin=Inches(1.05)
sec.right_margin=Inches(.85)
sec.header_distance=sec.footer_distance=Inches(.35)
sec.different_first_page_header_footer=True

def font(style,name,size):
    style.font.name=name
    style.font.size=Pt(size)
    style.font.color.rgb=RGBColor(0,0,0)
    rpr=style.element.get_or_add_rPr()
    rf=rpr.find(qn('w:rFonts'))
    if rf is None: rf=OxmlElement('w:rFonts');rpr.insert(0,rf)
    for key in ('asciiTheme','hAnsiTheme','eastAsiaTheme','cstheme','csTheme'):
        rf.attrib.pop(qn('w:'+key),None)
    for key in ('ascii','hAnsi','eastAsia','cs'):rf.set(qn('w:'+key),name)
    lang=OxmlElement('w:lang');lang.set(qn('w:val'),'uk-UA');rpr.append(lang)

for name in ['Normal','Title','Subtitle','Heading 1','Heading 2','Heading 3','Caption']:
    font(doc.styles[name],'Times New Roman',12)
normal=doc.styles['Normal']
normal.paragraph_format.line_spacing=1.18
normal.paragraph_format.space_after=Pt(6)
normal.paragraph_format.widow_control=True
for name,size in [('Heading 1',15),('Heading 2',13),('Heading 3',12)]:
    s=doc.styles[name];s.font.size=Pt(size);s.font.bold=True
    s.paragraph_format.space_before=Pt(14)
    s.paragraph_format.space_after=Pt(7)
    s.paragraph_format.keep_with_next=True
    s.paragraph_format.keep_together=True
doc.styles['Title'].font.size=Pt(19)
doc.styles['Title'].font.bold=True
doc.styles['Title'].paragraph_format.space_after=Pt(14)
doc.styles['Caption'].font.size=Pt(10.5)
doc.styles['Caption'].font.italic=False
doc.styles['Caption'].paragraph_format.space_after=Pt(8)
contents_style=doc.styles.add_style('Contents Heading',1)
font(contents_style,'Times New Roman',15)
contents_style.font.bold=True
contents_style.paragraph_format.space_after=Pt(10)
contents_style.paragraph_format.keep_with_next=True
for name,size in [('toc 1',11),('toc 2',10.5)]:
    style=doc.styles[name] if name in doc.styles else doc.styles.add_style(name,1)
    style.element.set(qn('w:styleId'),'TOC'+name[-1])
    style.element.set(qn('w:customStyle'),'0')
    font(style,'Times New Roman',size)
    style.paragraph_format.line_spacing=1.0
    style.paragraph_format.space_before=Pt(0)
    style.paragraph_format.space_after=Pt(1)
if 'Code' not in doc.styles:
    doc.styles.add_style('Code',1)
font(doc.styles['Code'],'Consolas',9)
doc.styles['Code'].paragraph_format.line_spacing=1.05
doc.styles['Code'].paragraph_format.space_after=Pt(0)
doc.styles['Code'].paragraph_format.keep_together=True
doc.styles['Code'].paragraph_format.left_indent=Inches(.1)
doc.styles['Code'].paragraph_format.right_indent=Inches(.1)

# Remove any inherited decorative paragraph rules from title styles.
for name in ('Title','Subtitle'):
    ppr=doc.styles[name].element.find(qn('w:pPr'))
    if ppr is not None:
        for node in list(ppr):
            if node.tag==qn('w:pBdr'):ppr.remove(node)

header=sec.header.paragraphs[0]
header.text='EnergyForecast   Практична робота 3'
header.alignment=WD_ALIGN_PARAGRAPH.RIGHT
for r in header.runs:r.font.name='Times New Roman';r.font.size=Pt(9);r.font.color.rgb=RGBColor(0,0,0)
footer=sec.footer.paragraphs[0]
footer.alignment=WD_ALIGN_PARAGRAPH.CENTER
field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGE');footer._p.append(field)

props=doc.core_properties
props.title='Звіт з практичної роботи 3 EnergyForecast'
props.subject='Repository REST API автентифікація та мережеві помилки'
props.author='Вахрушева Христина Костянтинівна'
props.keywords='ПР3, EnergyForecast, Kotlin, Retrofit, Room, JWT'
props.comments='Робоча реалізація і результати перевірок станом на 14.11.2026.'

def inline(p,text):
    for i,chunk in enumerate(re.split(r'(\*\*.*?\*\*)',text)):
        if not chunk:continue
        r=p.add_run(chunk[2:-2] if chunk.startswith('**') and chunk.endswith('**') else chunk)
        if chunk.startswith('**') and chunk.endswith('**'):r.bold=True
    return p

def para(text,style=None):
    p=doc.add_paragraph(style=style)
    inline(p,text)
    if style is None:p.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY
    # URLs and bibliographic entries should not produce stretched word spacing.
    if re.match(r'^\[\d+\] ',text):
        p.alignment=WD_ALIGN_PARAGRAPH.LEFT
    return p

def table(rows):
    columns=len(rows[0])
    tbl=doc.add_table(rows=1,cols=columns)
    tbl.alignment=WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit=False
    widths={2:[2.5,4.1],3:[1.65,3.05,1.9],4:[2.6,1.8,1.1,1.1],5:[1.5,1.15,1.25,1.0,1.7]}[columns]
    for col,w in zip(tbl.columns,widths):col.width=Inches(w)
    borders=OxmlElement('w:tblBorders')
    for edge in ['top','left','bottom','right','insideH','insideV']:
        n=OxmlElement('w:'+edge);n.set(qn('w:val'),'single');n.set(qn('w:sz'),'4');n.set(qn('w:color'),'D9D9D9');borders.append(n)
    tbl._tbl.tblPr.append(borders)
    margins=OxmlElement('w:tblCellMar')
    for side,value in [('top',80),('bottom',80),('left',95),('right',95)]:
        n=OxmlElement('w:'+side);n.set(qn('w:w'),str(value));n.set(qn('w:type'),'dxa');margins.append(n)
    tbl._tbl.tblPr.append(margins)
    for i,row in enumerate(rows):
        cells=tbl.rows[0].cells if i==0 else tbl.add_row().cells
        for j,(cell,text) in enumerate(zip(cells,row)):
            cell.width=Inches(widths[j]);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            p=cell.paragraphs[0];p.paragraph_format.space_after=Pt(0);p.paragraph_format.line_spacing=1.05
            p.alignment=WD_ALIGN_PARAGRAPH.CENTER if (columns>=4 and j>0) or (i==0) else WD_ALIGN_PARAGRAPH.LEFT
            inline(p,text)
            for r in p.runs:r.font.size=Pt(10.5);r.bold=(i==0)
            if i==0:
                shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'E8EDF3');cell._tc.get_or_add_tcPr().append(shade)
        trpr=tbl.rows[i]._tr.get_or_add_trPr()
        no_split=OxmlElement('w:cantSplit');trpr.append(no_split)
        if i==0:
            repeat=OxmlElement('w:tblHeader');trpr.append(repeat)
        if len(rows)<=6:
            for cell in cells:
                for paragraph in cell.paragraphs:
                    paragraph.paragraph_format.keep_with_next=(i<len(rows)-1)
    doc.add_paragraph().paragraph_format.space_after=Pt(2)

def mathrun(value):
    r=OxmlElement('m:r');t=OxmlElement('m:t');t.text=value;r.append(t);return r
def sub(base,index):
    s=OxmlElement('m:sSub');e=OxmlElement('m:e');e.append(mathrun(base));s.append(e)
    n=OxmlElement('m:sub');n.append(mathrun(index));s.append(n);return s
def fraction(a,b):
    f=OxmlElement('m:f');num=OxmlElement('m:num');num.append(a);den=OxmlElement('m:den');den.append(mathrun(b));f.append(num);f.append(den);return f
def equation(kind):
    p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before=Pt(5);p.paragraph_format.space_after=Pt(8)
    math=OxmlElement('m:oMath')
    if kind=='device':
        parts=[sub('E','day,i'),mathrun(' = '),fraction(sub('P','i'),'1000'),mathrun(' · '),sub('t','i'),mathrun(' · '),sub('q','i')]
        number=1
    elif kind=='total':
        summ=OxmlElement('m:nary');prop=OxmlElement('m:naryPr');char=OxmlElement('m:chr');char.set(qn('m:val'),'∑');prop.append(char);summ.append(prop)
        for name,val in [('sub','i = 1'),('sup','n')]:
            el=OxmlElement('m:'+name);el.append(mathrun(val));summ.append(el)
        expr=OxmlElement('m:e');expr.append(sub('E','day,i'));summ.append(expr)
        parts=[sub('E','day'),mathrun(' = '),summ];number=2
    else:parts=[sub('E','period'),mathrun(' = '),sub('E','day'),mathrun(' · D')];number=3
    for el in parts:math.append(el)
    p._p.append(math);p.add_run('     ('+str(number)+')')

def screenshot(p,number,width):
    image=images[fig_ids[number-1]]
    r=p.add_run();shape=r.add_picture(str(SHOTS/image['file']),width=Inches(width))
    for n in shape._inline.iter(qn('wp:docPr')):
        n.set('descr',f"Рисунок {number}. {image['caption']}")
        n.set('title',f'Рисунок {number}')
def figure_caption(number):
    image=images[fig_ids[number-1]]
    p=para(f"Рисунок {number} — {image['caption']}",'Caption')
    p.alignment=WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.keep_together=True
    return p

source=SOURCE.read_text(encoding='utf-8')
cover,body=source.split(':::body',1)
coverlines=[s.strip() for s in cover.splitlines() if s.strip()]
title=coverlines[0][2:]
for text in coverlines[1:4]:
    p=para(text);p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after=Pt(3)
    for r in p.runs:r.font.size=Pt(12)
p=doc.add_paragraph();p.paragraph_format.space_after=Pt(22)
p=para(title,'Title');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
for text in coverlines[4:9]:
    p=para(text);p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after=Pt(10)
    if text=='Просунутий рівень':
        for r in p.runs:r.bold=True
p=doc.add_paragraph();p.paragraph_format.space_after=Pt(18)
for text in coverlines[9:-1]:
    p=para(text);p.alignment=WD_ALIGN_PARAGRAPH.RIGHT
    p.paragraph_format.space_after=Pt(5)
p=doc.add_paragraph();p.paragraph_format.space_after=Pt(20)
p=para(coverlines[-1]);p.alignment=WD_ALIGN_PARAGRAPH.CENTER
doc.add_page_break()
h=para('Зміст','Contents Heading')
h._p.get_or_add_pPr().append(OxmlElement('w:suppressLineNumbers'))
p=doc.add_paragraph()
toc=OxmlElement('w:fldSimple');toc.set(qn('w:instr'),'TOC \\o "1-2" \\h \\z \\u')
cached=OxmlElement('w:r');t=OxmlElement('w:t');t.text='Зміст оновлюється під час підготовки документа.';cached.append(t);toc.append(cached);p._p.append(toc)
doc.add_page_break()

lines=body.splitlines();i=0;code=False;appendix=False;first_fig=True
while i<len(lines):
    line=lines[i].strip()
    if not line:i+=1;continue
    if line.startswith('```'):
        i+=1;code_lines=[]
        while i<len(lines) and not lines[i].strip().startswith('```'):
            code_lines.append(lines[i]);i+=1
        # Each code line remains editable; only short listings are kept together.
        for j,text in enumerate(code_lines):
            p=para(text,'Code');p.paragraph_format.keep_with_next=(j<len(code_lines)-1)
        doc.add_paragraph().paragraph_format.space_after=Pt(2)
    elif line.startswith(':::equation '):equation(line.split()[1])
    elif line==':::appendix':
        doc.add_page_break();appendix=True
    elif line.startswith(':::figpair'):
        a,b=map(int,line.split()[1:])
        p=doc.add_paragraph()
        if not first_fig:p.paragraph_format.page_break_before=True
        p.alignment=WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.keep_with_next=True
        screenshot(p,a,2.65);p.add_run('     ');screenshot(p,b,2.65)
        cap=figure_caption(a);cap.paragraph_format.keep_with_next=True
        figure_caption(b)
        first_fig=False
    elif line.startswith(':::figwide'):
        number=int(line.split()[1]);p=doc.add_paragraph();p.paragraph_format.page_break_before=True
        p.paragraph_format.keep_with_next=True;p.alignment=WD_ALIGN_PARAGRAPH.CENTER
        screenshot(p,number,6.55);figure_caption(number)
        para('У горизонтальній орієнтації наявний каталог залишається доступним, а дві картки розміщуються поруч. Для цього рисунка мережу емулятора вимкнено; дані читаються з постійного кешу Room.')
        first_fig=False
    elif line.startswith('### '):para(line[4:],'Heading 2')
    elif line.startswith('## '):
        p=para(line[3:],'Heading 1')
        if line.startswith('## Додаток Б'):p.paragraph_format.page_break_before=True
    elif line.startswith('|'):
        rows=[]
        while i<len(lines) and lines[i].strip().startswith('|'):
            vals=[v.strip() for v in lines[i].strip().strip('|').split('|')]
            if not all(re.fullmatch(r':?-+:?',v) for v in vals):rows.append(vals)
            i+=1
        table(rows);continue
    else:
        p=para(line,'Caption' if line.startswith(('Таблиця ','Лістинг ')) else None)
        if line.startswith(('Таблиця ','Лістинг ')):p.paragraph_format.keep_with_next=True
    i+=1

settings=doc.settings.element
update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
doc.save(DEST)
print(DEST)
print(f'Figures: {len(fig_ids)}; paragraphs: {len(doc.paragraphs)}; tables: {len(doc.tables)}')
