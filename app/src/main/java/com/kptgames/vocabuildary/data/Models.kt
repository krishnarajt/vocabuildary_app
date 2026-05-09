package com.kptgames.vocabuildary.data

import com.google.gson.annotations.SerializedName

data class ProfileResponse(val user: UserProfile = UserProfile())

data class UserProfile(
    val id: Int = 0,
    @SerializedName("identity_key") val identityKey: String = "",
    @SerializedName("gateway_sub") val gatewaySub: String? = null,
    val email: String? = null,
    val name: String? = null,
    val notifications: NotificationSettingsSummary = NotificationSettingsSummary(),
    val telegram: TelegramSettings = TelegramSettings(),
    val apprise: AppriseSettings = AppriseSettings(),
    val mobile: MobileSummary = MobileSummary(),
    val learning: LearningSettings = LearningSettings(),
    @SerializedName("raw_identity_headers") val rawIdentityHeaders: Map<String, String> = emptyMap()
)

data class NotificationSettingsSummary(
    val provider: String = "telegram",
    val configured: Boolean = false,
    @SerializedName("provider_configured") val providerConfigured: Boolean = false
)

data class TelegramSettings(
    @SerializedName("bot_token_set") val botTokenSet: Boolean = false,
    @SerializedName("bot_token_hint") val botTokenHint: String? = null,
    @SerializedName("chat_id") val chatId: String = "",
    val configured: Boolean = false
)

data class AppriseSettings(
    @SerializedName("urls_set") val urlsSet: Boolean = false,
    @SerializedName("urls_hint") val urlsHint: String? = null,
    val configured: Boolean = false
)

data class MobileSummary(
    val configured: Boolean = false,
    @SerializedName("device_count") val deviceCount: Int = 0,
    @SerializedName("enabled_device_count") val enabledDeviceCount: Int = 0
)

data class LearningSettings(
    val enabled: Boolean = true,
    @SerializedName("target_language_code") val targetLanguageCode: String = "en",
    @SerializedName("daily_review_words") val dailyReviewWords: Int = 3,
    @SerializedName("daily_cloze_words") val dailyClozeWords: Int = 1,
    @SerializedName("mastery_encounters") val masteryEncounters: Int = 8,
    @SerializedName("review_intervals") val reviewIntervals: List<Int> = listOf(1, 3, 7, 14, 30, 60, 120)
)

data class ReminderResponse(
    val items: List<ReminderItem> = emptyList(),
    val days: Int? = null
)

data class ReminderItem(
    @SerializedName("word_id") val wordId: Int = 0,
    val word: String = "",
    val meaning: String = "",
    val example: String = "",
    @SerializedName("language_code") val languageCode: String = "",
    @SerializedName("reminded_at") val remindedAt: String = ""
)

data class ProgressResponse(val items: List<WordProgress> = emptyList())

data class LearntResponse(
    val items: List<WordProgress> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0
)

data class WordProgress(
    @SerializedName("word_id") val wordId: Int = 0,
    val word: String = "",
    val meaning: String = "",
    @SerializedName("language_code") val languageCode: String = "",
    @SerializedName("frequency_rank") val frequencyRank: Int? = null,
    @SerializedName("zipf_frequency") val zipfFrequency: Double? = null,
    val status: String = "",
    @SerializedName("progress_percent") val progressPercent: Int = 0,
    @SerializedName("encounter_count") val encounterCount: Int = 0,
    @SerializedName("next_due_on") val nextDueOn: String? = null,
    @SerializedName("last_seen_on") val lastSeenOn: String? = null,
    @SerializedName("introduced_on") val introducedOn: String? = null,
    @SerializedName("mastered_at") val masteredAt: String? = null,
    @SerializedName("reset_count") val resetCount: Int = 0
)

data class LearningPlanResponse(val plan: LearningPlan? = null)

data class LearningPlan(
    val session: LearningSession = LearningSession(),
    @SerializedName("new_word") val newWord: WordItem? = null,
    @SerializedName("cloze_word") val clozeWord: WordItem? = null,
    @SerializedName("context_words") val contextWords: List<WordItem> = emptyList(),
    @SerializedName("review_words") val reviewWords: List<WordItem> = emptyList(),
    @SerializedName("previous_cloze") val previousCloze: PreviousCloze? = null,
    @SerializedName("message_text") val messageText: String? = null,
    @SerializedName("cloze_prompt") val clozePrompt: String? = null,
    @SerializedName("cloze_answer") val clozeAnswer: String? = null,
    val settings: LearningSettings = LearningSettings()
)

data class LearningSession(
    val id: Int = 0,
    val date: String? = null,
    @SerializedName("sent_at") val sentAt: String? = null
)

data class PreviousCloze(
    val word: String = "",
    val meaning: String = "",
    val prompt: String = "",
    val answer: String = "",
    @SerializedName("session_date") val sessionDate: String? = null
)

data class WordCatalogResponse(
    val items: List<WordItem> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0
)

data class WordItem(
    val id: Int = 0,
    @SerializedName("word_id") val wordId: Int? = null,
    val word: String = "",
    val meaning: String = "",
    val example: String = "",
    @SerializedName("language_code") val languageCode: String = "",
    @SerializedName("part_of_speech") val partOfSpeech: String? = null,
    val pronunciation: String? = null,
    val etymology: String? = null,
    val register: String? = null,
    @SerializedName("difficulty_level") val difficultyLevel: Int? = null,
    @SerializedName("frequency_rank") val frequencyRank: Int? = null,
    @SerializedName("zipf_frequency") val zipfFrequency: Double? = null,
    @SerializedName("occurrence_count") val occurrenceCount: Int? = null,
    @SerializedName("rank_in_book") val rankInBook: Int? = null
)

data class LanguagesResponse(val items: List<LanguageItem> = emptyList())

data class LanguageItem(
    val code: String = "",
    val name: String = "",
    @SerializedName("native_name") val nativeName: String = "",
    val notes: String = "",
    @SerializedName("word_count") val wordCount: Int = 0,
    @SerializedName("frequency_count") val frequencyCount: Int = 0,
    @SerializedName("book_count") val bookCount: Int = 0
)

data class ReminderSlotsResponse(val items: List<ReminderSlot> = emptyList())

data class ReminderSlot(
    val id: Int? = null,
    val label: String = "",
    @SerializedName("time_of_day") val timeOfDay: String = "09:00",
    val timezone: String = "Asia/Kolkata",
    val enabled: Boolean = true,
    @SerializedName("last_sent_on") val lastSentOn: String? = null
)

data class ImportsResponse(
    val stats: ImportStats? = null,
    val items: List<ImportRun> = emptyList()
)

data class ImportStats(
    @SerializedName("language_code") val languageCode: String = "en",
    @SerializedName("word_count") val wordCount: Int = 0,
    @SerializedName("frequency_count") val frequencyCount: Int = 0
)

data class ImportRun(
    val id: Int = 0,
    val source: String = "",
    @SerializedName("language_code") val languageCode: String = "",
    val status: String = "",
    @SerializedName("processed_items") val processedItems: Int = 0,
    @SerializedName("inserted_items") val insertedItems: Int = 0,
    @SerializedName("updated_items") val updatedItems: Int = 0,
    @SerializedName("skipped_items") val skippedItems: Int = 0,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class BooksResponse(val items: List<BookItem> = emptyList())

data class BookItem(
    val id: Int = 0,
    @SerializedName("book_uuid") val bookUuid: String = "",
    val name: String = "",
    val title: String = "",
    val isbn: String = "",
    val author: String = "",
    val language: String = "",
    @SerializedName("language_code") val languageCode: String = "en",
    val notes: String = "",
    val status: String = "",
    @SerializedName("learning_enabled") val learningEnabled: Boolean = false,
    @SerializedName("processing_error") val processingError: String? = null,
    val source: BookSource = BookSource(),
    val processed: BookProcessed = BookProcessed()
)

data class BookSource(
    val filename: String = "",
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("mime_type") val mimeType: String? = null
)

data class BookProcessed(
    @SerializedName("total_words") val totalWords: Int = 0,
    @SerializedName("unique_words") val uniqueWords: Int = 0,
    @SerializedName("processed_at") val processedAt: String? = null
)

data class BookResponse(val book: BookItem = BookItem())

data class CreateBookUploadResponse(
    val book: BookItem = BookItem(),
    val upload: UploadTarget = UploadTarget()
)

data class UploadTarget(
    val method: String = "PUT",
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

data class BookWordsResponse(
    val book: BookItem? = null,
    val items: List<WordItem> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0
)

data class LanguageSkillsResponse(
    val levels: List<SkillLevel> = emptyList(),
    val items: List<LanguageSkill> = emptyList()
)

data class SkillLevel(
    val code: String = "",
    val name: String = "",
    val description: String = ""
)

data class LanguageSkill(
    val language: LanguageItem = LanguageItem(),
    val level: UserLanguageLevel? = null,
    @SerializedName("quiz_available") val quizAvailable: Boolean = false,
    @SerializedName("quiz_id") val quizId: Int? = null
)

data class UserLanguageLevel(
    @SerializedName("level_code") val levelCode: String = "",
    val source: String = "",
    @SerializedName("quiz_score") val quizScore: Int? = null,
    @SerializedName("quiz_total") val quizTotal: Int? = null
)

data class SkillUpdateResponse(
    val skill: LanguageSkill? = null,
    val levels: List<SkillLevel> = emptyList()
)

data class QuizResponse(val quiz: LanguageQuiz? = null)

data class QuizGenerateResponse(
    val quiz: LanguageQuiz? = null,
    val generated: Boolean = false,
    val message: String = ""
)

data class LanguageQuiz(
    val id: Int = 0,
    @SerializedName("language_code") val languageCode: String = "",
    val title: String = "",
    val source: String = "",
    @SerializedName("question_count") val questionCount: Int = 0,
    val questions: List<QuizQuestion> = emptyList()
)

data class QuizQuestion(
    val id: Int = 0,
    val position: Int = 0,
    @SerializedName("prompt_type") val promptType: String = "",
    @SerializedName("question_text") val questionText: String = "",
    val options: List<String> = emptyList(),
    val explanation: String = ""
)

data class QuizScoreResponse(
    val score: Int = 0,
    val total: Int = 0,
    @SerializedName("level_code") val levelCode: String = "",
    val results: List<QuizQuestionResult> = emptyList(),
    val skill: LanguageSkill? = null
)

data class QuizQuestionResult(
    @SerializedName("question_id") val questionId: Int = 0,
    val correct: Boolean = false,
    @SerializedName("correct_option_index") val correctOptionIndex: Int? = null,
    val explanation: String = ""
)

data class MobileDeviceResponse(val device: MobileDeviceInfo = MobileDeviceInfo())

data class MobileDevicesResponse(val items: List<MobileDeviceInfo> = emptyList())

data class MobileDeviceInfo(
    val id: Int = 0,
    @SerializedName("device_id") val deviceId: String = "",
    val platform: String = "android",
    @SerializedName("display_name") val displayName: String = "",
    val timezone: String = "",
    @SerializedName("app_version") val appVersion: String = "",
    val enabled: Boolean = true,
    @SerializedName("last_seen_at") val lastSeenAt: String? = null
)

data class MobileNotificationsResponse(
    val results: List<Map<String, Any>> = emptyList(),
    val items: List<MobileNotificationItem> = emptyList()
)

data class MobileNotificationItem(
    val id: Int = 0,
    @SerializedName("device_id") val deviceId: Int = 0,
    @SerializedName("session_id") val sessionId: Int? = null,
    @SerializedName("word_id") val wordId: Int? = null,
    val kind: String = "",
    val title: String = "",
    val body: String = "",
    @SerializedName("html_body") val htmlBody: String = "",
    @SerializedName("queued_at") val queuedAt: String? = null
)

data class ApiMessage(
    val message: String? = null,
    val error: String? = null,
    val started: Boolean? = null
)
