package com.secondbrain.android.review

data class ReviewOption(val key: String, val text: String)
data class ReviewPrompt(val question: String, val options: List<ReviewOption>, val explanation: String)

/** 兼容历史题卡混入答案的格式；答题前只保留题干和选项，绝不回退展示原始解析。 */
fun parseReviewPrompt(raw: String): ReviewPrompt {
    val normalized = raw.replace("\r\n", "\n")
    val marker = Regex("(?:【(?:正确答案|参考答案|选项解析|答案解析|解析)】|(?:正确答案|参考答案|答案|选项解析|答案解析|解析)(?:\\*\\*)?\\s*[：:])").find(normalized)
    val visible = normalized.take(marker?.range?.first ?: normalized.length).trim().trimEnd('*', '【').trim()
    val explanation = marker?.let { normalized.substring(it.range.first).trim() }.orEmpty()
    val matches = Regex("(?m)^\\s*(?:\\*\\*)?([A-H])[.．、:：)）]\\s*(?:\\*\\*)?(.+)").findAll(visible).toList()
    val options = matches.mapIndexed { index, match ->
        val end = matches.getOrNull(index + 1)?.range?.first ?: visible.length
        ReviewOption(match.groupValues[1], (match.groupValues[2] + visible.substring(match.range.last + 1, end)).trim())
    }
    val question = visible.take(matches.firstOrNull()?.range?.first ?: visible.length).trim()
    return ReviewPrompt(question, options, explanation)
}
