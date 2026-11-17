"""Explicit black text and a clickable repository link for the PW03 report."""
from pathlib import Path
from copy import deepcopy
import zipfile
from lxml import etree
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.text.paragraph import Paragraph
from docx.opc.constants import RELATIONSHIP_TYPE

def enforce_black_text(path):
    """Use explicit black for all Word text and styles, independent of theme colors.

    Image bytes and table fills/borders are preserved. Character styles, TOC,
    field text, equations, headers and footers are covered as well as body text.
    """
    with zipfile.ZipFile(path) as archive:
        members = [(info, archive.read(info.filename)) for info in archive.infolist()]
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main', 'm': 'http://schemas.openxmlformats.org/officeDocument/2006/math'}

    def black(properties):
        node = properties.find(qn('w:color'))
        if node is None:
            node = OxmlElement('w:color')
            properties.append(node)
        node.attrib.clear()
        node.set(qn('w:val'), '000000')
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as archive:
        for info, data in members:
            if info.filename.startswith('word/') and info.filename.endswith('.xml'):
                tree = etree.fromstring(data)
                for node in tree.xpath('//w:color', namespaces=ns):
                    node.attrib.clear()
                    node.set(qn('w:val'), '000000')
                for parent in tree.xpath('//w:style | //w:r | //m:r | //w:rPrDefault', namespaces=ns):
                    properties = parent.find(qn('w:rPr'))
                    if properties is None:
                        properties = OxmlElement('w:rPr')
                    else:
                        parent.remove(properties)
                    if parent.tag == qn('w:style'):
                        index = next((i for i, child in enumerate(parent) if child.tag in [qn('w:tblPr'), qn('w:trPr'), qn('w:tcPr'), qn('w:tblStylePr')]), len(parent))
                    else:
                        index = 1 if parent.tag == qn('m:r') and parent.find(qn('m:rPr')) is not None else 0
                    parent.insert(index, properties)
                    black(properties)
                data = etree.tostring(tree, xml_declaration=True, encoding='UTF-8', standalone=True)
            archive.writestr(info, data)

def add_black_hyperlink(paragraph, url):
    link = OxmlElement('w:hyperlink')
    link.set(qn('r:id'), paragraph.part.relate_to(url, RELATIONSHIP_TYPE.HYPERLINK, is_external=True))
    run = OxmlElement('w:r'); properties = OxmlElement('w:rPr')
    colour = OxmlElement('w:color'); colour.set(qn('w:val'), '000000'); properties.append(colour)
    underline = OxmlElement('w:u'); underline.set(qn('w:val'), 'single'); properties.append(underline)
    run.append(properties); text = OxmlElement('w:t'); text.text = url; run.append(text)
    link.append(run); paragraph._p.append(link)


def update_existing_report(path):
    doc = Document(path)
    owner = next(p for p in doc.paragraphs if p.text.strip() == 'Вахрушева Христина Костянтинівна')
    url = 'https://github.com/Christ1naa/RPZMP/tree/feature/pw03/PW03'
    following = owner._p.getnext()
    if following is None or url not in ''.join(following.itertext()):
        element = OxmlElement('w:p'); owner._p.addnext(element)
        p = Paragraph(element, owner._parent)
        if owner._p.pPr is not None: element.append(deepcopy(owner._p.pPr))
        add_black_hyperlink(p, url)
    doc.save(path)
    enforce_black_text(path)


if __name__ == '__main__':
    import sys
    update_existing_report(Path(sys.argv[1]))
