package com.example.data.model

enum class AiModel(
    val id: String,
    val displayName: String,
    val provider: String,
    val description: String
) {
    GEMINI_3_5_FLASH(
        id = "gemini-3.5-flash",
        displayName = "Leo Fast (Gemini 3.5 Flash)",
        provider = "Google Gemini",
        description = "فائق السرعة ومناسب لكافة المهام اليومية والمحادثات الذكية"
    ),
    GEMINI_3_1_PRO(
        id = "gemini-3.1-pro-preview",
        displayName = "Leo Pro (Gemini 3.1 Pro)",
        provider = "Google Gemini",
        description = "تفكير عميق واستنتاج متقدم للبرمجة والمشكلات المعقدة"
    ),
    DEEPSEEK_CHAT(
        id = "deepseek-chat",
        displayName = "Leo DeepSeek (مفتوح المصدر)",
        provider = "DeepSeek AI",
        description = "نموذج ذكاء اصطناعي مفتوح المصدر متميز في الأكواد والمنطق"
    );

    companion object {
        fun fromId(id: String): AiModel {
            return entries.firstOrNull { it.id == id } ?: GEMINI_3_5_FLASH
        }
    }
}
