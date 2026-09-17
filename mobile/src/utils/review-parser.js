// 复习卡片文本解析 —— 抽取网页版 Review.vue 中题干/选项解析逻辑，
// 供列表预览与答题页共用，避免两处重复实现。

const OPTION_PREFIXES = ['A.', 'B.', 'C.', 'D.', 'A、', 'B、', 'C、', 'D、']

// 题干中需要剔除的行（选项 / 答案 / 解析），与网页版保持一致
const NON_QUESTION_PREFIXES = [
  ...OPTION_PREFIXES,
  '正确答案：',
  '解析：',
  '【正确答案】',
  '【解析】',
]

/**
 * 从原始 question 文本中抽取题干部分。
 * question 格式为：「题干\nA. xxx\nB. xxx\n正确答案：A\n解析：xxx」
 *
 * @param {string} question 原始题目文本
 * @returns {string} 题干
 */
export function parseQuestionText(question) {
  if (!question) return ''
  const raw = typeof question === 'string' ? question : String(question ?? '')
  const parts = []
  for (const line of raw.split('\n')) {
    const trimmed = line.trim()
    if (trimmed && !NON_QUESTION_PREFIXES.some((p) => trimmed.startsWith(p))) {
      parts.push(trimmed)
    }
  }
  const cleaned = parts.join(' ').trim()
  if (cleaned) return cleaned
  return raw.trim().slice(0, 80) || ''
}

/**
 * 从原始 question 文本中解析选择题选项。
 *
 * @param {string} question 原始题目文本
 * @returns {Array<{key: string, text: string}>} 选项列表
 */
export function parseChoiceOptions(question) {
  if (!question) return []
  const options = []
  for (const line of question.split('\n')) {
    const trimmed = line.trim()
    if (trimmed.startsWith('选项解析：') || trimmed.startsWith('解析：')) break
    const match = trimmed.match(/^([A-D])\.\s*(.+)$/)
    if (match) {
      options.push({ key: match[1], text: match[2].trim() })
    }
  }
  return options
}

/**
 * 题型文案映射，与网页版 getCardTypeText 一致。
 */
const CARD_TYPE_TEXT = {
  choice: '选择题',
  essay: '简答题',
  fill: '填空题',
  judge: '判断题',
}

export function getCardTypeText(cardType) {
  return CARD_TYPE_TEXT[cardType] || '未知题型'
}

/* ===== 列表预览用到的两个兼容导出（去 AI 味列表页之前使用的名字）===== */

/**
 * 把 question 文本里的 Markdown、选项、答案行去掉，用于卡片预览。
 * 之所以单独提供而不是直接用 parseQuestionText：
 * 列表页需要更强的"纯文本"清洗（去 markdown 语法、去 # 列表号），
 * 而 parseQuestionText 被答题页依赖，需要更原始的题干格式，两者目标不同。
 */
export function stripQuestionMarkdown(question) {
  const base = parseQuestionText(question)
  if (!base) return ''
  return base
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/[#>*_\-~]/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

/**
 * 卡片池名的展示美化：
 * 前端展示时加上题型/数量的简短信息；接口层并没有这个字段，这里只做轻量展示。
 * 如果 name 本身是纯中文名则直接返回；否则尝试格式化。
 */
export function formatPoolName(pool) {
  if (!pool) return '全部卡片'
  if (typeof pool === 'string') return pool
  const name = pool.name || pool.poolName || '全部卡片'
  const count = pool.cardCount ?? pool.total ?? null
  if (count == null || count === '') return String(name)
  return `${name} · ${count} 张`
}

export default { parseQuestionText, parseChoiceOptions, getCardTypeText, stripQuestionMarkdown, formatPoolName }