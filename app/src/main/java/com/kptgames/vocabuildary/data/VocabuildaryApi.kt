package com.kptgames.vocabuildary.data

import android.content.ContentResolver
import android.net.Uri
import com.kptgames.vocabuildary.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.TimeZone
import java.util.concurrent.TimeUnit

interface VocabuildaryApi {
    @GET("me")
    suspend fun getProfile(): Response<ProfileResponse>

    @GET("recent-reminders")
    suspend fun getRecentReminders(
        @Query("limit") limit: Int = 5,
        @Query("days") days: Int? = null
    ): Response<ReminderResponse>

    @GET("learnt-words")
    suspend fun getLearntWords(
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0
    ): Response<LearntResponse>

    @GET("word-progress")
    suspend fun getWordProgress(@Query("limit") limit: Int = 100): Response<ProgressResponse>

    @POST("word-progress/{wordId}/reset")
    suspend fun resetWordProgress(@Path("wordId") wordId: Int): Response<Map<String, Any>>

    @GET("learning-plan")
    suspend fun getLearningPlan(): Response<LearningPlanResponse>

    @PUT("learning-plan")
    suspend fun updateLearningPlan(@Body payload: Map<String, Any?>): Response<LearningPlanResponse>

    @POST("learning-plan/rebuild")
    suspend fun rebuildLearningPlan(): Response<LearningPlanResponse>

    @GET("languages")
    suspend fun getLanguages(): Response<LanguagesResponse>

    @POST("languages")
    suspend fun createLanguage(@Body payload: Map<String, Any?>): Response<Map<String, LanguageItem>>

    @GET("words")
    suspend fun searchWords(
        @Query("query") query: String? = null,
        @Query("language_code") languageCode: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): Response<WordCatalogResponse>

    @GET("imports")
    suspend fun getImports(
        @Query("language_code") languageCode: String = "en",
        @Query("limit") limit: Int = 20
    ): Response<ImportsResponse>

    @POST("imports/frequency")
    suspend fun startFrequencyImport(@Body payload: Map<String, Any?>): Response<ApiMessage>

    @POST("imports/kaikki")
    suspend fun startKaikkiImport(@Body payload: Map<String, Any?>): Response<ApiMessage>

    @GET("reminder-slots")
    suspend fun getReminderSlots(): Response<ReminderSlotsResponse>

    @PUT("reminder-slots")
    suspend fun updateReminderSlots(@Body payload: Map<String, Any?>): Response<ReminderSlotsResponse>

    @GET("books")
    suspend fun getBooks(): Response<BooksResponse>

    @PUT("books/{bookId}")
    suspend fun updateBook(
        @Path("bookId") bookId: Int,
        @Body payload: Map<String, Any?>
    ): Response<BookResponse>

    @POST("books/uploads")
    suspend fun createBookUpload(@Body payload: Map<String, Any?>): Response<CreateBookUploadResponse>

    @POST("books/{bookId}/upload-complete")
    suspend fun markBookUploadComplete(
        @Path("bookId") bookId: Int,
        @Body payload: Map<String, Any?>
    ): Response<BookResponse>

    @POST("books/{bookId}/process")
    suspend fun processBook(@Path("bookId") bookId: Int): Response<BookResponse>

    @GET("books/{bookId}/words")
    suspend fun getBookWords(
        @Path("bookId") bookId: Int,
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0
    ): Response<BookWordsResponse>

    @GET("language-skills")
    suspend fun getLanguageSkills(): Response<LanguageSkillsResponse>

    @PUT("language-skills/{languageCode}")
    suspend fun updateLanguageSkill(
        @Path("languageCode") languageCode: String,
        @Body payload: Map<String, Any?>
    ): Response<SkillUpdateResponse>

    @GET("language-skills/{languageCode}/quiz")
    suspend fun getLanguageQuiz(@Path("languageCode") languageCode: String): Response<QuizResponse>

    @POST("language-skills/{languageCode}/quiz/generate")
    suspend fun generateLanguageQuiz(@Path("languageCode") languageCode: String): Response<QuizGenerateResponse>

    @POST("language-skills/{languageCode}/quiz/score")
    suspend fun scoreLanguageQuiz(
        @Path("languageCode") languageCode: String,
        @Body payload: Map<String, Any?>
    ): Response<QuizScoreResponse>

    @PUT("settings")
    suspend fun updateSettings(@Body payload: Map<String, Any?>): Response<ProfileResponse>

    @POST("test-trigger")
    suspend fun sendTestReminder(): Response<ApiMessage>

    @GET("mobile/devices")
    suspend fun getMobileDevices(): Response<MobileDevicesResponse>

    @POST("mobile/devices")
    suspend fun registerMobileDevice(@Body payload: Map<String, Any?>): Response<MobileDeviceResponse>

    @GET("mobile/notifications")
    suspend fun getMobileNotifications(
        @Query("device_id") deviceId: String,
        @Query("limit") limit: Int = 20
    ): Response<MobileNotificationsResponse>

    @POST("mobile/notifications/sync-due")
    suspend fun syncDueMobileNotifications(@Body payload: Map<String, Any?>): Response<MobileNotificationsResponse>

    @POST("mobile/notifications/{notificationId}/delivered")
    suspend fun markMobileNotificationDelivered(
        @Path("notificationId") notificationId: Int
    ): Response<Map<String, Any>>
}

object ApiFactory {
    fun create(authManager: OidcAuthManager): Pair<VocabuildaryApi, OkHttpClient> {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { authManager.freshAccessToken() }
                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.VOCABUILDARY_API_BASE_URL.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(VocabuildaryApi::class.java) to client
    }
}

class VocabuildaryRepository(
    private val api: VocabuildaryApi,
    private val httpClient: OkHttpClient,
    private val preferences: AppPreferences
) {
    suspend fun loadDashboard(): DashboardSnapshot = coroutineScope {
        val profile = async { api.getProfile().unwrap().user }
        val recent = async { api.getRecentReminders(limit = 5).unwrap().items }
        val history = async { api.getRecentReminders(limit = 500, days = 7).unwrap().items }
        val learnt = async { api.getLearntWords().unwrap().items }
        val progress = async { api.getWordProgress().unwrap().items }
        val plan = async { api.getLearningPlan().unwrap().plan }
        val languages = async { api.getLanguages().unwrap().items }
        val catalog = async { api.searchWords(languageCode = "en").unwrap() }
        val imports = async { api.getImports().unwrap() }
        val slots = async { api.getReminderSlots().unwrap().items }
        val books = async { api.getBooks().unwrap().items }
        val skills = async { api.getLanguageSkills().unwrap() }
        awaitAll(
            profile,
            recent,
            history,
            learnt,
            progress,
            plan,
            languages,
            catalog,
            imports,
            slots,
            books,
            skills
        )
        DashboardSnapshot(
            profile = profile.await(),
            reminders = recent.await(),
            notificationHistory = history.await(),
            learntWords = learnt.await(),
            progress = progress.await(),
            learningPlan = plan.await(),
            languages = languages.await(),
            catalog = catalog.await(),
            imports = imports.await(),
            reminderSlots = slots.await(),
            books = books.await(),
            languageSkills = skills.await()
        )
    }

    suspend fun registerDevice(): MobileDeviceInfo {
        val deviceId = preferences.getOrCreateDeviceId()
        val payload = mapOf(
            "device_id" to deviceId,
            "platform" to "android",
            "display_name" to android.os.Build.MODEL,
            "timezone" to TimeZone.getDefault().id,
            "app_version" to BuildConfig.VERSION_NAME,
            "enabled" to true
        )
        return api.registerMobileDevice(payload).unwrap().device
    }

    suspend fun syncDueMobileNotifications(): List<MobileNotificationItem> {
        val deviceId = preferences.getOrCreateDeviceId()
        val response = api.syncDueMobileNotifications(
            mapOf(
                "device_id" to deviceId,
                "platform" to "android",
                "display_name" to android.os.Build.MODEL,
                "timezone" to TimeZone.getDefault().id,
                "app_version" to BuildConfig.VERSION_NAME,
                "limit" to 20
            )
        ).unwrap()
        return response.items
    }

    suspend fun markMobileNotificationDelivered(notificationId: Int) {
        api.markMobileNotificationDelivered(notificationId).unwrap()
    }

    suspend fun updateSettings(payload: Map<String, Any?>): UserProfile {
        return api.updateSettings(payload).unwrap().user
    }

    suspend fun sendTestReminder(): String {
        return api.sendTestReminder().unwrap().message ?: "Test notification sent."
    }

    suspend fun saveReminderSlots(slots: List<ReminderSlot>): List<ReminderSlot> {
        val payload = mapOf("slots" to slots.map { it.toApiPayload() })
        return api.updateReminderSlots(payload).unwrap().items
    }

    suspend fun searchWords(query: String, languageCode: String): WordCatalogResponse {
        return api.searchWords(
            query = query.ifBlank { null },
            languageCode = languageCode.ifBlank { null }
        ).unwrap()
    }

    suspend fun createLanguage(code: String, name: String, nativeName: String, notes: String) {
        api.createLanguage(
            mapOf(
                "code" to code,
                "name" to name,
                "native_name" to nativeName,
                "notes" to notes
            )
        ).unwrap()
    }

    suspend fun startFrequencyImport() {
        api.startFrequencyImport(mapOf("language_code" to "en")).unwrap()
    }

    suspend fun startKaikkiImport(insertMissing: Boolean) {
        api.startKaikkiImport(
            mapOf(
                "language_code" to "en",
                "insert_missing" to insertMissing
            )
        ).unwrap()
    }

    suspend fun saveLearningPlan(clozeWordId: Int?, contextIds: List<Int>): LearningPlan? {
        return api.updateLearningPlan(
            mapOf(
                "cloze_word_id" to clozeWordId,
                "context_word_ids" to contextIds
            )
        ).unwrap().plan
    }

    suspend fun rebuildLearningPlan(): LearningPlan? {
        return api.rebuildLearningPlan().unwrap().plan
    }

    suspend fun resetProgress(wordId: Int) {
        api.resetWordProgress(wordId).unwrap()
    }

    suspend fun updateSkill(languageCode: String, levelCode: String): SkillUpdateResponse {
        return api.updateLanguageSkill(languageCode, mapOf("level_code" to levelCode)).unwrap()
    }

    suspend fun getQuiz(languageCode: String): LanguageQuiz? {
        return api.getLanguageQuiz(languageCode).unwrap().quiz
    }

    suspend fun generateQuiz(languageCode: String): LanguageQuiz? {
        return api.generateLanguageQuiz(languageCode).unwrap().quiz
    }

    suspend fun scoreQuiz(languageCode: String, answers: Map<Int, Int>): QuizScoreResponse {
        return api.scoreLanguageQuiz(
            languageCode,
            mapOf("answers" to answers.mapKeys { it.key.toString() })
        ).unwrap()
    }

    suspend fun updateBookLearning(bookId: Int, enabled: Boolean): BookItem {
        return api.updateBook(bookId, mapOf("learning_enabled" to enabled)).unwrap().book
    }

    suspend fun processBook(bookId: Int): BookItem {
        return api.processBook(bookId).unwrap().book
    }

    suspend fun getBookWords(bookId: Int): List<WordItem> {
        return api.getBookWords(bookId).unwrap().items
    }

    suspend fun uploadBook(
        contentResolver: ContentResolver,
        uri: Uri,
        filename: String,
        contentType: String?,
        fileSize: Long?,
        metadata: BookUploadDraft
    ): BookItem = withContext(Dispatchers.IO) {
        val uploadResponse = api.createBookUpload(
            mapOf(
                "filename" to filename,
                "file_size" to fileSize,
                "content_type" to contentType,
                "title" to metadata.title,
                "author" to metadata.author,
                "isbn" to metadata.isbn,
                "language" to metadata.language,
                "language_code" to metadata.languageCode,
                "notes" to metadata.notes,
                "learning_enabled" to metadata.learningEnabled
            )
        ).unwrap()
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to read selected file")
        val requestBuilder = Request.Builder().url(uploadResponse.upload.url)
        uploadResponse.upload.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        val mediaType = contentType?.toMediaTypeOrNull()
        val request = requestBuilder
            .method(uploadResponse.upload.method, bytes.toRequestBody(mediaType))
            .build()
        val uploadResult = httpClient.newCall(request).execute()
        if (!uploadResult.isSuccessful) {
            throw IllegalStateException("Book upload failed: HTTP ${uploadResult.code}")
        }
        api.markBookUploadComplete(
            uploadResponse.book.id,
            mapOf("file_size" to (fileSize ?: bytes.size.toLong()))
        ).unwrap().book
    }
}

data class DashboardSnapshot(
    val profile: UserProfile,
    val reminders: List<ReminderItem>,
    val notificationHistory: List<ReminderItem>,
    val learntWords: List<WordProgress>,
    val progress: List<WordProgress>,
    val learningPlan: LearningPlan?,
    val languages: List<LanguageItem>,
    val catalog: WordCatalogResponse,
    val imports: ImportsResponse,
    val reminderSlots: List<ReminderSlot>,
    val books: List<BookItem>,
    val languageSkills: LanguageSkillsResponse
)

data class BookUploadDraft(
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val language: String = "",
    val languageCode: String = "en",
    val notes: String = "",
    val learningEnabled: Boolean = false
)

suspend fun <T> Response<T>.unwrap(): T {
    if (isSuccessful) {
        body()?.let { return it }
        throw IllegalStateException("Empty response body")
    }
    val errorText = errorBody()?.string().orEmpty()
    val message = Regex(""""error"\s*:\s*"([^"]+)"""").find(errorText)?.groupValues?.getOrNull(1)
        ?: Regex(""""message"\s*:\s*"([^"]+)"""").find(errorText)?.groupValues?.getOrNull(1)
        ?: message()
        ?: "Request failed"
    throw IllegalStateException(message)
}

fun ReminderSlot.toApiPayload(): Map<String, Any?> = mapOf(
    "id" to id,
    "label" to label,
    "time_of_day" to timeOfDay,
    "timezone" to timezone,
    "enabled" to enabled
)

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
