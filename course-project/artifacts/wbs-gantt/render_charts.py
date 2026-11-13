from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'pictures'
OUT.mkdir(parents=True, exist_ok=True)
FONT = 'C:/Windows/Fonts/msyh.ttc'
BOLD = 'C:/Windows/Fonts/msyhbd.ttc'


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else FONT, size)


def text(draw, xy, value, size=24, fill='#162b32', bold=False):
    draw.text(xy, value, font=font(size, bold), fill=fill)


rows = [
    ('1.1', '需求与验收清单', 'A', 1, 1),
    ('1.2', '架构与界面原型', 'A', 1, 1),
    ('1.3', '协调与阶段评审', 'A', 1, 8),
    ('2.1', '服务与数据模型', 'B', 1, 1),
    ('2.2', '用户认证与权限', 'B', 2, 2),
    ('2.3', '知识与记录 API', 'B', 2, 2),
    ('3.1', 'React 与认证页面', 'A', 2, 2),
    ('3.2', '知识与富文本', 'A', 3, 3),
    ('4.1', '录音与语音识别', 'C', 4, 4),
    ('4.2', 'AI 润色评价状态', 'C', 5, 5),
    ('5.1', '智能测验', 'A', 5, 5),
    ('5.2', 'RAG 知识问答', 'C', 6, 6),
    ('6.1', '知识图谱', 'D', 6, 6),
    ('6.2', '三维知识场景', 'D', 7, 7),
    ('7.1', '桌面与云端发布', 'B', 7, 8),
    ('7.2', '联调与验收', 'D', 8, 8),
    ('7.3', '文档与成果展示', 'A', 8, 8),
]
colors = {'A': '#246a87', 'B': '#38775c', 'C': '#8a6435', 'D': '#65548a'}
im = Image.new('RGB', (1800, 1510), '#ffffff')
d = ImageDraw.Draw(im)
text(d, (65, 38), '费曼学习平台项目甘特图', 44, bold=True)
text(d, (65, 105), '课程练习假定计划   4人团队 · 8周 · 16次课程', 26, '#52646a')
text(d, (65, 153), 'A 张晨 项目与前端     B 李欣 后端与部署     C 王博 AI     D 赵宁 可视化与测试', 24)
x0, top, week_w, row_h = 620, 225, 136, 62
d.rectangle((65, top, 1735, top + row_h), fill='#e8eef2')
text(d, (84, top + 14), 'WBS 与工作包', 25, bold=True)
text(d, (532, top + 14), '主责', 25, bold=True)
for w in range(8):
    text(d, (x0 + w * week_w + 28, top + 14), f'第{w + 1}周', 25, bold=True)
for i, (code, label, owner, start, end) in enumerate(rows):
    y = top + (i + 1) * row_h
    if i % 2 == 0:
        d.rectangle((65, y, 1735, y + row_h), fill='#f6f8f9')
    text(d, (84, y + 15), f'{code}  {label}', 24)
    text(d, (548, y + 15), owner, 24, colors[owner], True)
    for w in range(9):
        d.line((x0 + w * week_w, y, x0 + w * week_w, y + row_h), fill='#dce2e5', width=1)
    d.rounded_rectangle((x0 + (start - 1) * week_w + 10, y + 14, x0 + end * week_w - 10, y + row_h - 14), radius=7, fill=colors[owner])
    d.line((65, y + row_h, 1735, y + row_h), fill='#dce2e5', width=1)
text(d, (65, 1377), '里程碑：第2周 基础接口   第5周 复述闭环   第7周 功能集成   第8周 最终交付', 25, bold=True)
text(d, (65, 1427), '色条表示计划工作区间，不代表实际完成进度；同一周内的任务可按依赖先后执行。', 23, '#52646a')
im.save(OUT / 'feynman-platform-gantt.png')

groups = [
    ('1 项目管理与总体设计', ['1.1 需求与验收清单', '1.2 架构与界面原型', '1.3 协调与阶段评审']),
    ('2 后端与数据服务', ['2.1 服务与数据模型', '2.2 注册登录与 JWT 权限', '2.3 知识与学习记录 API']),
    ('3 前端与知识学习界面', ['3.1 React 与认证页面', '3.2 知识管理与富文本']),
    ('4 费曼复述核心功能', ['4.1 录音与百度语音转文字', '4.2 AI 润色评分与学习状态']),
    ('5 AI 学习辅助', ['5.1 智能测验与答题反馈', '5.2 个人知识库 RAG 问答']),
    ('6 知识可视化', ['6.1 知识图谱', '6.2 Three.js 与 CesiumJS 场景']),
    ('7 发布与验收', ['7.1 Electron 与云部署', '7.2 系统联调与验收', '7.3 文档与成果展示']),
]
im = Image.new('RGB', (1800, 1120), '#ffffff')
d = ImageDraw.Draw(im)
text(d, (65, 35), '费曼学习平台 WBS 工作分解结构', 44, bold=True)
text(d, (65, 105), '按交付成果划分工作包；编号与甘特图、负责人及验收标准一一对应。', 25, '#52646a')
d.rounded_rectangle((615, 175, 1185, 260), radius=10, fill='#173e4b')
text(d, (708, 196), '0 费曼学习平台', 34, '#ffffff', True)
left, width, gap, y1, y2, card_h = 65, 395, 30, 345, 695, 270
d.line((900, 260, 900, 305), fill='#8eabb5', width=3)
d.line((40, 305, 1537, 305), fill='#8eabb5', width=3)
d.line((40, 305, 40, 655), fill='#8eabb5', width=3)
d.line((40, 655, 1112, 655), fill='#8eabb5', width=3)
for i, (title, items) in enumerate(groups):
    col, row = (i, 0) if i < 4 else (i - 4, 1)
    x, y = left + col * (width + gap), y1 if row == 0 else y2
    if row == 0:
        d.line((x + width // 2, 305, x + width // 2, y), fill='#8eabb5', width=3)
    else:
        d.line((x + width // 2, 655, x + width // 2, y), fill='#8eabb5', width=3)
    d.rounded_rectangle((x, y, x + width, y + card_h), radius=10, fill='#f0f5f6', outline='#c7d7dd', width=2)
    text(d, (x + 20, y + 22), title, 26, bold=True)
    d.line((x + 20, y + 70, x + width - 20, y + 70), fill='#c7d7dd', width=2)
    for j, item in enumerate(items):
        text(d, (x + 20, y + 98 + j * 49), item, 22)
text(d, (65, 1030), '说明：下排模块与上排模块均为平台的直接子模块，位置仅用于排版，不表示业务依赖。', 24, '#52646a')
im.save(OUT / 'feynman-platform-wbs.png')
print(OUT / 'feynman-platform-gantt.png')
print(OUT / 'feynman-platform-wbs.png')
