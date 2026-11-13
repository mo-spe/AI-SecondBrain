from pathlib import Path
import re
from zipfile import ZipFile

from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'docs/stage-two-program-design.md'
OUTPUT = ROOT / 'docs/stage-two-program-design.docx'


def set_font(run, name='宋体', size=11.5, bold=False):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = RGBColor(0, 0, 0)
    fonts = run._element.get_or_add_rPr().get_or_add_rFonts()
    fonts.set(qn('w:eastAsia'), name)
    fonts.set(qn('w:ascii'), 'Arial' if name == '宋体' else name)
    fonts.set(qn('w:hAnsi'), 'Arial' if name == '宋体' else name)


def text_runs(paragraph, text, size=11.5):
    for fragment in re.split(r'(\*\*.*?\*\*|`[^`]+`)', text):
        if not fragment:
            continue
        bold = fragment.startswith('**') and fragment.endswith('**')
        code = fragment.startswith('`') and fragment.endswith('`')
        value = fragment[2:-2] if bold else fragment[1:-1] if code else fragment
        run = paragraph.add_run(value)
        set_font(run, 'Consolas' if code else '宋体', size, bold)


doc = Document()
section = doc.sections[0]
section.page_width = Inches(8.5)
section.page_height = Inches(11)
section.top_margin = section.bottom_margin = Inches(0.85)
section.left_margin = section.right_margin = Inches(0.9)
section.footer_distance = Inches(0.35)
section.different_first_page_header_footer = True

normal = doc.styles['Normal']
normal.font.name = '宋体'
normal.font.size = Pt(11.5)
normal.font.color.rgb = RGBColor(0, 0, 0)
normal.element.get_or_add_rPr().get_or_add_rFonts().set(qn('w:eastAsia'), '宋体')
normal.paragraph_format.line_spacing = 1.25
normal.paragraph_format.space_after = Pt(7)
normal.paragraph_format.widow_control = True
for style_name, size in [('Title', 26), ('Subtitle', 15), ('Heading 1', 17), ('Heading 2', 13)]:
    style = doc.styles[style_name]
    style.font.name = '微软雅黑'
    style.font.size = Pt(size)
    style.font.color.rgb = RGBColor(0, 0, 0)
    style.font.bold = style_name != 'Subtitle'
    style.font.italic = False
    style.font.underline = False
    fonts = style.element.get_or_add_rPr().get_or_add_rFonts()
    fonts.set(qn('w:eastAsia'), '微软雅黑')
    fonts.set(qn('w:ascii'), 'Arial')
    fonts.set(qn('w:hAnsi'), 'Arial')
    style.paragraph_format.space_before = Pt(15)
    style.paragraph_format.space_after = Pt(9)
    style.paragraph_format.keep_with_next = True
    if style_name in ['Title', 'Subtitle']:
        props = style.element.get_or_add_pPr()
        for existing in list(props.findall(qn('w:pBdr'))):
            props.remove(existing)
        borders = OxmlElement('w:pBdr')
        for edge in ['top', 'left', 'bottom', 'right', 'between', 'bar']:
            element = OxmlElement('w:' + edge)
            element.set(qn('w:val'), 'nil')
            borders.append(element)
        props.append(borders)

footer = section.footer.paragraphs[0]
footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
set_font(footer.add_run('第 '), size=9)
field = OxmlElement('w:fldSimple')
field.set(qn('w:instr'), 'PAGE')
footer._p.append(field)
set_font(footer.add_run(' 页'), size=9)

doc.core_properties.title = '第二阶段前端界面与核心交互程序设计文档'
doc.core_properties.subject = '费曼学习平台课程版第4至第7次课'
doc.core_properties.author = '项目开发组'
doc.core_properties.keywords = 'React Vite JWT Markdown KaTeX Mermaid MediaRecorder 百度语音识别'


def add_table(rows):
    count = len(rows[0])
    total = 9648
    if count == 3:
        widths = [900, 3400, total - 4300] if rows[0][0] in ['课次', '顺序', '编号'] else [3100, 2600, total - 5700]
    else:
        widths = [5100, total - 5100]
    table = doc.add_table(rows=0, cols=count)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    props = table._tbl.tblPr
    width = props.find(qn('w:tblW'))
    width.set(qn('w:w'), str(total))
    width.set(qn('w:type'), 'dxa')
    for col, value in zip(table.columns, widths):
        col.width = Inches(value / 1440)
    for index, values in enumerate(rows):
        row = table.add_row()
        no_split = OxmlElement('w:cantSplit')
        row._tr.get_or_add_trPr().append(no_split)
        if index == 0:
            row._tr.get_or_add_trPr().append(OxmlElement('w:tblHeader'))
        for col_index, (cell, value, cell_width) in enumerate(zip(row.cells, values, widths)):
            cell.width = Inches(cell_width / 1440)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            cp = cell._tc.get_or_add_tcPr()
            borders = OxmlElement('w:tcBorders')
            for edge in ['top', 'left', 'bottom', 'right']:
                border = OxmlElement('w:' + edge)
                for key, val in [('val', 'single'), ('sz', '4'), ('color', 'D9D9D9')]:
                    border.set(qn('w:' + key), val)
                borders.append(border)
            cp.append(borders)
            margins = OxmlElement('w:tcMar')
            for edge, val in [('top', 90), ('bottom', 90), ('left', 120), ('right', 120)]:
                element = OxmlElement('w:' + edge)
                element.set(qn('w:w'), str(val))
                element.set(qn('w:type'), 'dxa')
                margins.append(element)
            cp.append(margins)
            if index == 0:
                shading = OxmlElement('w:shd')
                shading.set(qn('w:fill'), 'E7EEF4')
                shading.set(qn('w:val'), 'clear')
                cp.append(shading)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.15
            if col_index == 0 and widths[0] < 1200:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            text_runs(p, value, size=10)
            if index == 0:
                for run in p.runs:
                    run.font.bold = True
    spacer = doc.add_paragraph()
    spacer.paragraph_format.space_after = Pt(2)
    spacer.paragraph_format.line_spacing = Pt(3)
    set_font(spacer.add_run(''), size=3)


lines = SOURCE.read_text(encoding='utf-8').splitlines()
index = 0
cover = True
code_fence = None
while index < len(lines):
    line = lines[index]
    if code_fence is not None:
        if line.strip() == code_fence:
            code_fence = None
        else:
            p = doc.add_paragraph()
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.05
            p.paragraph_format.left_indent = Inches(0.12)
            p.paragraph_format.keep_with_next = index + 1 < len(lines) and lines[index + 1].strip() != code_fence
            set_font(p.add_run(line or ' '), 'Consolas', 9)
        index += 1
        continue
    if line.startswith('```'):
        code_fence = re.match(r'`+', line).group(0)
    elif not line.strip():
        pass
    elif line.startswith('# '):
        p = doc.add_paragraph('第二阶段前端界面与核心交互', style='Title')
        p.paragraph_format.space_before = Pt(65)
        p.paragraph_format.space_after = Pt(3)
        for run in p.runs:
            set_font(run, '微软雅黑', 23, True)
        p = doc.add_paragraph('程序设计文档', style='Title')
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(24)
        for run in p.runs:
            set_font(run, '微软雅黑', 23, True)
    elif line.startswith('## '):
        cover = False
        p = doc.add_paragraph(line[3:], style='Heading 1')
        new_page = line[3:].startswith('1 ') or line[3:].startswith('附录')
        p.paragraph_format.page_break_before = new_page
        p.paragraph_format.space_before = Pt(0 if new_page else 15)
    elif line.startswith('### '):
        doc.add_paragraph(line[4:], style='Heading 2')
    elif line.startswith('|'):
        rows = []
        while index < len(lines) and lines[index].startswith('|'):
            values = [x.strip() for x in lines[index].strip().strip('|').split('|')]
            if not all(re.match(r'^:?-+:?$', x) for x in values):
                rows.append(values)
            index += 1
        add_table(rows)
        continue
    else:
        if cover and line == '费曼学习平台课程版':
            doc.add_paragraph(line, style='Subtitle')
        else:
            p = doc.add_paragraph()
            text_runs(p, line)
            if not cover:
                p.paragraph_format.first_line_indent = Inches(0.23)
    index += 1

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
doc.save(OUTPUT)
with ZipFile(OUTPUT) as archive:
    assert archive.testzip() is None
check = Document(OUTPUT)
assert len(check.tables) == 10, len(check.tables)
print(f'Created {OUTPUT}')
print(f'Paragraphs: {len(check.paragraphs)}; tables: {len(check.tables)}')
