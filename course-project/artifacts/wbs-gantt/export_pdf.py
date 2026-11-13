from pathlib import Path
from html import escape
from io import BytesIO
import re

from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, A3, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader, PdfWriter

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / 'output/pdf/feynman-platform-wbs-gantt.pdf'
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('CN', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
pdfmetrics.registerFont(TTFont('CN-Bold', 'C:/Windows/Fonts/msyhbd.ttc', subfontIndex=0))
styles = {
    'title': ParagraphStyle('title', fontName='CN-Bold', fontSize=20, leading=30, spaceAfter=18),
    'h1': ParagraphStyle('h1', fontName='CN-Bold', fontSize=14, leading=22, spaceBefore=16, spaceAfter=10, keepWithNext=True),
    'body': ParagraphStyle('body', fontName='CN', fontSize=10.5, leading=17, spaceAfter=9, wordWrap='CJK'),
    'cell': ParagraphStyle('cell', fontName='CN', fontSize=9, leading=14, wordWrap='CJK'),
    'head': ParagraphStyle('head', fontName='CN-Bold', fontSize=9, leading=14, wordWrap='CJK'),
}


def plain(value):
    return escape(value.replace('`', '').replace('**', ''))


def footer(c, doc):
    c.saveState()
    c.setFont('CN', 8)
    c.setFillColor(colors.HexColor('#586870'))
    c.drawRightString(A4[0] - 42, 23, f'第 {doc.page} 页')
    c.restoreState()


story = []
lines = (ROOT / 'docs/feynman-platform-wbs-gantt.md').read_text(encoding='utf-8').splitlines()
index = 0
while index < len(lines):
    line = lines[index].strip()
    if line.startswith('```'):
        index += 1
        while index < len(lines) and not lines[index].startswith('```'):
            index += 1
        story.append(Paragraph('完整WBS层级结构见文末附图1，工作包编号与下表对应。', styles['body']))
    elif line.startswith('|'):
        rows = []
        while index < len(lines) and lines[index].strip().startswith('|'):
            row = [cell.strip() for cell in lines[index].strip().strip('|').split('|')]
            if not all(re.fullmatch(r':?-+:?', cell) for cell in row):
                rows.append(row)
            index += 1
        if '第1周' in rows[0]:
            story.append(Paragraph('完整甘特图见文末附图2，各工作包的起止周次已在进度计划中列明。', styles['body']))
            continue
        widths = {3: [115, 72, 324], 4: [39, 172, 36, 264], 5: [42, 52, 52, 45, 320]}[len(rows[0])]
        if rows[0][0] == '里程碑':
            widths = [140, 75, 296]
        data = [[Paragraph(plain(cell), styles['head' if r == 0 else 'cell']) for cell in row] for r, row in enumerate(rows)]
        table = Table(data, colWidths=widths, repeatRows=1, hAlign='LEFT')
        table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#E8EEF2')),
            ('GRID', (0, 0), (-1, -1), .5, colors.HexColor('#CED8DD')),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('LEFTPADDING', (0, 0), (-1, -1), 7),
            ('RIGHTPADDING', (0, 0), (-1, -1), 7),
            ('TOPPADDING', (0, 0), (-1, -1), 7),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 7),
        ]))
        story += [table, Spacer(1, 11)]
        continue
    elif line.startswith('# '):
        story.append(Paragraph(plain(line[2:]), styles['title']))
    elif line.startswith('## '):
        story.append(Paragraph(plain(line[3:]), styles['h1']))
    elif line and not line.startswith('配套图片：'):
        if line.startswith('图中“■”'):
            line = '甘特图以横向色条表示各工作包的计划工作区间，色条颜色对应主负责人，不表示实际完成百分比。'
        story.append(Paragraph(plain(line), styles['body']))
    index += 1

body = BytesIO()
SimpleDocTemplate(body, pagesize=A4, leftMargin=42, rightMargin=42, topMargin=38, bottomMargin=40,
                  title='费曼学习平台WBS工作分解与甘特图', author='课程项目组').build(story, onFirstPage=footer, onLaterPages=footer)
writer = PdfWriter()
writer.append(PdfReader(body))
body_pages = len(writer.pages)
charts = BytesIO()
c = canvas.Canvas(charts, pagesize=landscape(A3))
w, h = landscape(A3)
for number, (filename, label) in enumerate([
    ('feynman-platform-wbs.png', '附图1 WBS工作分解结构'),
    ('feynman-platform-gantt.png', '附图2 项目甘特图'),
], start=1):
    c.setFont('CN-Bold', 16)
    c.drawString(40, h - 32, label)
    image = ImageReader(str(ROOT / 'pictures' / filename))
    iw, ih = image.getSize()
    scale = min((w - 80) / iw, (h - 95) / ih)
    dw, dh = iw * scale, ih * scale
    c.drawImage(image, (w - dw) / 2, 45 + (h - 95 - dh) / 2, dw, dh)
    c.setFont('CN', 9)
    c.drawRightString(w - 40, 20, f'第 {body_pages + number} 页')
    c.showPage()
c.save()
writer.append(PdfReader(charts))
writer.add_metadata({'/Title': '费曼学习平台WBS工作分解与甘特图', '/Author': '课程项目组'})
with OUTPUT.open('wb') as stream:
    writer.write(stream)
check = PdfReader(OUTPUT)
assert len(check.pages) == body_pages + 2
assert '17' in ''.join(page.extract_text() for page in check.pages) or '7.3' in ''.join(page.extract_text() for page in check.pages)
print(f'Created {OUTPUT}')
print(f'Pages: {len(check.pages)}; text pages: {body_pages}; chart pages: 2')
