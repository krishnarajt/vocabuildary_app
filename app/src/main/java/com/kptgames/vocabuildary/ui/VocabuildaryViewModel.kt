package com.kptgames.vocabuildary.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kptgames.vocabuildary.data.*
import com.kptgames.vocabuildary.notifications.MobileNotificationWorker
import com.kptgames.vocabuildary.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

data class SettingsForm(
    val provider: String = "telegram",
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val appriseUrls: String = "",
    val learningEnabled: Boolean = true,
    val targetLanguageCode: String = "en",
    val dailyReviewWords: String = "3",
    val dailyClozeWords: String = "1",
    val masteryEncounters: String = "8",
    val reviewIntervals: String = "1,3,7,14,30,60,120"
)

data class LanguageForm(
    val code: String = "",
    val name: String = "",
    val nativeName: String = "",
    val notes: String = ""
)

data class CatalogForm(
    val query: String = "",
    val languageCode: String = "en"
)

data class PlanEditor(
    val clozeWordId: Int? = null,
    val contextWordIds: Set<Int> = emptySet()
)

data class BookWordsSheet(
    val book: BookItem? = null,
    val words: List<WordItem> = emptyList()
)

data class VocabuildaryUiState(
    val authChecked: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val status: String = "Checking session",
    val error: String? = null,
    val profile: UserProfile? = null,
    val reminders: List<ReminderItem> = emptyList(),
    val notificationHistory: List<ReminderItem> = emptyList(),
    val learntWords: List<WordProgress> = emptyList(),
    val progress: List<WordProgress> = emptyList(),
    val learningPlan: LearningPlan? = null,
    val languages: List<LanguageItem> = emptyList(),
    val catalog: WordCatalogResponse = WordCatalogResponse(),
    val imports: ImportsResponse = ImportsResponse(),
    val reminderSlots: List<ReminderSlot> = emptyList(),
    val books: List<BookItem> = emptyList(),
    val skills: LanguageSkillsResponse = LanguageSkillsResponse(),
    val selectedSkillLanguage: String = "en",
    val quiz: LanguageQuiz? = null,
    val quizAnswers: Map<Int, Int> = emptyMap(),
    val quizResult: QuizScoreResponse? = null,
    val settingsForm: SettingsForm = SettingsForm(),
    val languageForm: LanguageForm = LanguageForm(),
    val catalogForm: CatalogForm = CatalogForm(),
    val planEditor: PlanEditor = PlanEditor(),
    val bookDraft: BookUploadDraft = BookUploadDraft(),
    val selectedBookFileName: String? = null,
    val bookWordsSheet: BookWordsSheet? = null
)

class VocabuildaryViewModel(
    application: Application,
    private val authManager: OidcAuthManager,
    private val repository: VocabuildaryRepository
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(VocabuildaryUiState())
    val state: StateFlow<VocabuildaryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.load()
            if (authManager.isAuthorized()) {
                _state.value = _state.value.copy(isLoggedIn = true, authChecked = true)
                refreshAll()
            } else {
                _state.value = _state.value.copy(authChecked = true, status = "Sign in required")
            }
        }
    }

    fun handleAuthResult(result: Result<Unit>) {
        viewModelScope.launch {
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoggedIn = true, status = "Signed in")
                    refreshAll()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoggedIn = false,
                        status = "Sign in failed",
                        error = error.message
                    )
                }
            )
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, status = "Refreshing")
            try {
                repository.registerDevice()
                val snapshot = repository.loadDashboard()
                val selectedLanguage = snapshot.languageSkills.items.firstOrNull()?.language?.code
                    ?: snapshot.profile.learning.targetLanguageCode
                _state.value = _state.value.copy(
                    loading = false,
                    status = "Connected",
                    profile = snapshot.profile,
                    reminders = snapshot.reminders,
                    notificationHistory = snapshot.notificationHistory,
                    learntWords = snapshot.learntWords,
                    progress = snapshot.progress,
                    learningPlan = snapshot.learningPlan,
                    languages = snapshot.languages,
                    catalog = snapshot.catalog,
                    imports = snapshot.imports,
                    reminderSlots = snapshot.reminderSlots,
                    books = snapshot.books,
                    skills = snapshot.languageSkills,
                    selectedSkillLanguage = selectedLanguage,
                    settingsForm = settingsFormFromProfile(snapshot.profile),
                    catalogForm = CatalogForm(languageCode = snapshot.profile.learning.targetLanguageCode),
                    planEditor = planEditorFromPlan(snapshot.learningPlan)
                )
                ReminderScheduler.saveAndSchedule(getApplication(), snapshot.reminderSlots)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    status = "API unavailable",
                    error = error.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _state.value = VocabuildaryUiState(authChecked = true, status = "Signed out")
        }
    }

    fun updateSettingsForm(transform: (SettingsForm) -> SettingsForm) {
        _state.value = _state.value.copy(settingsForm = transform(_state.value.settingsForm))
    }

    fun saveSettings() {
        val form = _state.value.settingsForm
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, status = "Saving settings")
            try {
                val payload = mutableMapOf<String, Any?>(
                    "notification_provider" to form.provider,
                    "telegram_chat_id" to form.telegramChatId.trim(),
                    "learning" to mapOf(
                        "enabled" to form.learningEnabled,
                        "target_language_code" to form.targetLanguageCode.trim().lowercase(),
                        "daily_review_words" to form.dailyReviewWords.trim(),
                        "daily_cloze_words" to form.dailyClozeWords.trim(),
                        "mastery_encounters" to form.masteryEncounters.trim(),
                        "review_intervals" to form.reviewIntervals.trim()
                    )
                )
                if (form.telegramBotToken.isNotBlank()) {
                    payload["telegram_bot_token"] = form.telegramBotToken.trim()
                }
                if (form.appriseUrls.isNotBlank()) {
                    payload["apprise_urls"] = form.appriseUrls.trim()
                }
                val profile = repository.updateSettings(payload)
                _state.value = _state.value.copy(
                    saving = false,
                    profile = profile,
                    settingsForm = settingsFormFromProfile(profile),
                    status = "Settings saved"
                )
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, status = "Save failed", error = error.message)
            }
        }
    }

    fun sendTestReminder() {
        viewModelScope.launch {
            _state.value = _state.value.copy(status = "Sending test notification")
            try {
                val message = repository.sendTestReminder()
                MobileNotificationWorker.enqueue(getApplication())
                _state.value = _state.value.copy(status = message)
            } catch (error: Exception) {
                _state.value = _state.value.copy(status = "Test failed", error = error.message)
            }
        }
    }

    fun updateReminderSlot(index: Int, slot: ReminderSlot) {
        val slots = _state.value.reminderSlots.toMutableList()
        if (index in slots.indices) {
            slots[index] = slot
            _state.value = _state.value.copy(reminderSlots = slots)
        }
    }

    fun addReminderSlot() {
        val slots = _state.value.reminderSlots + ReminderSlot(
            label = "Reminder",
            timeOfDay = "09:00",
            timezone = TimeZone.getDefault().id,
            enabled = true
        )
        _state.value = _state.value.copy(reminderSlots = slots)
    }

    fun removeReminderSlot(index: Int) {
        val slots = _state.value.reminderSlots.toMutableList()
        if (index in slots.indices) {
            slots.removeAt(index)
            _state.value = _state.value.copy(reminderSlots = slots)
        }
    }

    fun saveReminderSlots() {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, status = "Saving reminder slots")
            try {
                val slots = repository.saveReminderSlots(_state.value.reminderSlots)
                ReminderScheduler.saveAndSchedule(getApplication(), slots)
                _state.value = _state.value.copy(
                    saving = false,
                    reminderSlots = slots,
                    status = "Reminder slots saved"
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message)
            }
        }
    }

    fun updateCatalogForm(transform: (CatalogForm) -> CatalogForm) {
        _state.value = _state.value.copy(catalogForm = transform(_state.value.catalogForm))
    }

    fun searchCatalog() {
        val form = _state.value.catalogForm
        viewModelScope.launch {
            _state.value = _state.value.copy(status = "Searching catalog")
            try {
                val catalog = repository.searchWords(form.query, form.languageCode)
                _state.value = _state.value.copy(catalog = catalog, status = "Catalog ready")
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun updateLanguageForm(transform: (LanguageForm) -> LanguageForm) {
        _state.value = _state.value.copy(languageForm = transform(_state.value.languageForm))
    }

    fun createLanguage() {
        val form = _state.value.languageForm
        viewModelScope.launch {
            try {
                repository.createLanguage(form.code, form.name, form.nativeName, form.notes)
                _state.value = _state.value.copy(languageForm = LanguageForm(), status = "Language saved")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun startFrequencyImport() {
        viewModelScope.launch {
            try {
                repository.startFrequencyImport()
                _state.value = _state.value.copy(status = "Frequency import queued")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun startKaikkiImport(insertMissing: Boolean) {
        viewModelScope.launch {
            try {
                repository.startKaikkiImport(insertMissing)
                _state.value = _state.value.copy(status = "Kaikki import queued")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun updatePlanEditor(transform: (PlanEditor) -> PlanEditor) {
        _state.value = _state.value.copy(planEditor = transform(_state.value.planEditor))
    }

    fun saveLearningPlan() {
        val editor = _state.value.planEditor
        viewModelScope.launch {
            try {
                val plan = repository.saveLearningPlan(editor.clozeWordId, editor.contextWordIds.toList())
                _state.value = _state.value.copy(
                    learningPlan = plan,
                    planEditor = planEditorFromPlan(plan),
                    status = "Plan saved"
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun rebuildLearningPlan() {
        viewModelScope.launch {
            try {
                val plan = repository.rebuildLearningPlan()
                _state.value = _state.value.copy(
                    learningPlan = plan,
                    planEditor = planEditorFromPlan(plan),
                    status = "Plan rebuilt"
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun resetProgress(wordId: Int) {
        viewModelScope.launch {
            try {
                repository.resetProgress(wordId)
                _state.value = _state.value.copy(status = "Progress reset")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun selectSkillLanguage(code: String) {
        _state.value = _state.value.copy(
            selectedSkillLanguage = code,
            quiz = null,
            quizAnswers = emptyMap(),
            quizResult = null
        )
    }

    fun saveSkillLevel(languageCode: String, levelCode: String) {
        viewModelScope.launch {
            try {
                repository.updateSkill(languageCode, levelCode)
                _state.value = _state.value.copy(status = "Skill level saved")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun loadQuiz(generate: Boolean = false) {
        val language = _state.value.selectedSkillLanguage
        viewModelScope.launch {
            try {
                val quiz = if (generate) repository.generateQuiz(language) else repository.getQuiz(language)
                _state.value = _state.value.copy(quiz = quiz, quizAnswers = emptyMap(), quizResult = null)
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun answerQuiz(questionId: Int, optionIndex: Int) {
        _state.value = _state.value.copy(
            quizAnswers = _state.value.quizAnswers + (questionId to optionIndex)
        )
    }

    fun submitQuiz() {
        val language = _state.value.selectedSkillLanguage
        val answers = _state.value.quizAnswers
        viewModelScope.launch {
            try {
                val result = repository.scoreQuiz(language, answers)
                _state.value = _state.value.copy(quizResult = result, status = "Quiz scored")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun updateBookDraft(transform: (BookUploadDraft) -> BookUploadDraft) {
        _state.value = _state.value.copy(bookDraft = transform(_state.value.bookDraft))
    }

    fun uploadBook(
        contentResolver: ContentResolver,
        uri: Uri,
        filename: String,
        contentType: String?,
        fileSize: Long?
    ) {
        val draft = _state.value.bookDraft
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, selectedBookFileName = filename)
            try {
                repository.uploadBook(contentResolver, uri, filename, contentType, fileSize, draft)
                _state.value = _state.value.copy(
                    saving = false,
                    status = "Book uploaded",
                    bookDraft = BookUploadDraft(),
                    selectedBookFileName = null
                )
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message)
            }
        }
    }

    fun processBook(bookId: Int) {
        viewModelScope.launch {
            try {
                repository.processBook(bookId)
                _state.value = _state.value.copy(status = "Book processed")
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun toggleBookLearning(book: BookItem) {
        viewModelScope.launch {
            try {
                repository.updateBookLearning(book.id, !book.learningEnabled)
                refreshAll()
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun viewBookWords(book: BookItem) {
        viewModelScope.launch {
            try {
                val words = repository.getBookWords(book.id)
                _state.value = _state.value.copy(bookWordsSheet = BookWordsSheet(book, words))
            } catch (error: Exception) {
                _state.value = _state.value.copy(error = error.message)
            }
        }
    }

    fun closeBookWords() {
        _state.value = _state.value.copy(bookWordsSheet = null)
    }

    private fun settingsFormFromProfile(profile: UserProfile): SettingsForm {
        val learning = profile.learning
        return SettingsForm(
            provider = profile.notifications.provider,
            telegramChatId = profile.telegram.chatId,
            learningEnabled = learning.enabled,
            targetLanguageCode = learning.targetLanguageCode,
            dailyReviewWords = learning.dailyReviewWords.toString(),
            dailyClozeWords = learning.dailyClozeWords.toString(),
            masteryEncounters = learning.masteryEncounters.toString(),
            reviewIntervals = learning.reviewIntervals.joinToString(",")
        )
    }

    private fun planEditorFromPlan(plan: LearningPlan?): PlanEditor {
        return PlanEditor(
            clozeWordId = plan?.clozeWord?.id,
            contextWordIds = plan?.contextWords?.map { it.id }?.toSet().orEmpty()
        )
    }
}

class VocabuildaryViewModelFactory(
    private val application: Application,
    private val authManager: OidcAuthManager,
    private val repository: VocabuildaryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VocabuildaryViewModel::class.java)) {
            return VocabuildaryViewModel(application, authManager, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
