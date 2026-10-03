package com.antigravity.noichuan.domain.service

/**
 * Xây dựng chuỗi JSON Grammar cho Vosk Speech Recognizer.
 * Quy định bắt buộc: Luôn luôn bổ sung token "[unk]" vào cuối grammar
 * để mô hình gán các từ ngoại lai / nhiễu âm vào [unk], giảm thiểu false-accept.
 */
object VoskGrammarBuilder {

    const val UNKNOWN_TOKEN = "[unk]"

    fun buildGrammarJson(targetWords: List<String>): String {
        val cleanWords = targetWords
            .map { cleanWord(it) }
            .filter { it.isNotEmpty() }
            .distinct()

        val listWithUnk = if (cleanWords.contains(UNKNOWN_TOKEN)) {
            cleanWords
        } else {
            cleanWords + UNKNOWN_TOKEN
        }

        return listWithUnk.joinToString(
            separator = "\", \"",
            prefix = "[\"",
            postfix = "\"]"
        )
    }

    fun cleanWord(raw: String): String {
        return raw.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s'-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
