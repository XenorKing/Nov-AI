package com.novaproject.novai.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val thumbsUp: Int = 0,
    val thumbsDown: Int = 0
)

data class Conversation(
    val id: String = "",
    val title: String = "Новый диалог",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val preview: String = "",
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList()
)

data class AISettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val topP: Float = 1.0f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val systemPromptOverride: String = "",
    val customModel: String = "",
    val customModels: List<String> = emptyList(),
    val openRouterToken: String = "",
    val aiName: String = "NovAI",
    val aiAvatarEmoji: String = "🤖",
    val accentColor: String = "cyan",
    /** Per-model system prompts: modelId → prompt text (empty string = use default). */
    val modelPrompts: Map<String, String> = emptyMap()
)

data class ChatRequest(
    val model: String = "nex-agi/nex-n2-pro:free",
    val messages: List<Map<String, String>>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
    val top_p: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0,
    val stream: Boolean = false
)

data class ChatChoice(val message: com.google.gson.JsonObject?)
data class ChatResponse(val choices: List<ChatChoice>?)

data class DayActivity(val label: String, val count: Int)

data class ModelTokenStats(
    val model: String = "",
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val totalTokens: Long = 0L,
    val messages: Long = 0L
)

data class UserStats(
    val totalConversations: Int = 0,
    val activeToday: Int = 0,
    val activeThisWeek: Int = 0,
    val dailyActivity: List<DayActivity> = emptyList(),
    val totalMessages: Long = 0L,
    val tokenStatsByModel: List<ModelTokenStats> = emptyList()
)

data class MessageSearchResult(
    val convId: String,
    val convTitle: String,
    val message: Message
)

/** Shared list of free models used across ChatScreen and SettingsScreen. */
data class FreeModel(val label: String, val modelId: String, val description: String)

val FREE_MODEL_LIST = listOf(
    FreeModel("NovAI",          "",                                               "Быстрый · рус, en, код · по умолчанию"),
    FreeModel("Llama 3.1 8B",   "meta-llama/llama-3.1-8b-instruct:free",         "Универсальный · Meta AI · рус, en, код"),
    FreeModel("Mistral 7B",     "mistralai/mistral-7b-instruct:free",             "Лёгкий и быстрый · Mistral AI · en, код"),
    FreeModel("Gemma 3 12B",    "google/gemma-3-12b-it:free",                     "Умный · Google · en, рус, аналитика"),
    FreeModel("Qwen 2.5 7B",    "qwen/qwen-2.5-7b-instruct:free",                "Многоязычный · Alibaba · рус, en, код")
)

/** Friendly display name for a model ID. */
fun friendlyModelName(modelId: String): String {
    FREE_MODEL_LIST.firstOrNull { it.modelId == modelId }?.let { return it.label }
    return when {
        modelId.isBlank() || modelId.startsWith("nex-agi/nex-n2") -> "NovAI"
        modelId.contains("llama-3.1-8b") || modelId.contains("llama-3-8b") -> "Llama 3.1 8B"
        modelId.contains("llama-3.1-70b") -> "Llama 3.1 70B"
        modelId.contains("mistral-7b") -> "Mistral 7B"
        modelId.contains("mistral") -> "Mistral"
        modelId.contains("gemma-3-12b") -> "Gemma 3 12B"
        modelId.contains("gemma") -> "Gemma"
        modelId.contains("qwen-2.5-7b") || modelId.contains("qwen2.5-7b") -> "Qwen 2.5 7B"
        modelId.contains("qwen") -> "Qwen"
        modelId.contains("gpt-4o-mini") -> "GPT-4o mini"
        modelId.contains("gpt-4o") -> "GPT-4o"
        modelId.contains("claude-3.5-sonnet") -> "Claude 3.5 Sonnet"
        modelId.contains("claude-3-haiku") -> "Claude 3 Haiku"
        modelId.contains("gemini-flash") -> "Gemini Flash"
        modelId.contains("gemini-pro") -> "Gemini Pro"
        else -> modelId.substringAfterLast("/").removeSuffix(":free").take(28)
    }
}
