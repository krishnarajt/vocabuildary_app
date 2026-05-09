@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kptgames.vocabuildary.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kptgames.vocabuildary.data.BookItem
import com.kptgames.vocabuildary.data.BookUploadDraft
import com.kptgames.vocabuildary.data.FrequencyBand
import com.kptgames.vocabuildary.data.LanguageSkill
import com.kptgames.vocabuildary.data.ReminderSlot
import com.kptgames.vocabuildary.data.WordItem
import com.kptgames.vocabuildary.ui.theme.VocabuildaryTheme

private enum class AppTab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.Home),
    Learnt("Learnt", Icons.Filled.School),
    Past("Past", Icons.Filled.History),
    Plan("Plan", Icons.Filled.Flag),
    Catalog("Catalog", Icons.Filled.Search),
    Settings("Settings", Icons.Filled.Settings)
}

@Composable
fun VocabuildaryRoot(
    viewModel: VocabuildaryViewModel,
    onLogin: () -> Unit
) {
    VocabuildaryTheme {
        val state by viewModel.state.collectAsState()
        if (!state.authChecked) {
            LoadingScreen("Checking session")
        } else if (!state.isLoggedIn) {
            LoginScreen(status = state.status, error = state.error, onLogin = onLogin)
        } else {
            DashboardScreen(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun LoadingScreen(label: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LoginScreen(status: String, error: String?, onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vocabuildary", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Daily vocabulary, now with native Android reminders.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in through API gateway")
        }
        Spacer(Modifier.height(12.dp))
        Text(status, color = MaterialTheme.colorScheme.primary)
        if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    var tab by remember { mutableStateOf(AppTab.Today) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vocabuildary", fontWeight = FontWeight.Bold)
                        Text(
                            state.profile?.name ?: state.profile?.email ?: state.status,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatusStrip(state)
            }
            when (tab) {
                AppTab.Today -> {
                    item { TodayPanel(state, viewModel) }
                    item { ReminderSlotsPanel(state, viewModel) }
                }
                AppTab.Learnt -> {
                    item { LearntPanel(state, viewModel) }
                }
                AppTab.Past -> {
                    item { NotificationHistoryPanel(state) }
                }
                AppTab.Plan -> {
                    item { LearningPlanPanel(state, viewModel) }
                    item { ProgressPanel(state, viewModel) }
                }
                AppTab.Catalog -> {
                    item { CatalogPanel(state, viewModel) }
                    item { SkillsPanel(state, viewModel) }
                    item { BooksPanel(state, viewModel) }
                }
                AppTab.Settings -> {
                    item { SettingsPanel(state, viewModel) }
                    item { IdentityPanel(state, viewModel) }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }

        state.bookWordsSheet?.let { sheet ->
            ModalBottomSheet(onDismissRequest = { viewModel.closeBookWords() }) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sheet.book?.name ?: "Book words", style = MaterialTheme.typography.titleLarge)
                    sheet.words.take(80).forEach { word ->
                        WordRow(word = word)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusStrip(state: VocabuildaryUiState) {
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    val languageCodes = state.profile?.learning?.targetLanguageCodes.orEmpty()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text(state.status) })
        if (languageCodes.isNotEmpty()) {
            AssistChip(
                onClick = {},
                label = { Text(languageCodes.joinToString(" / ") { it.uppercase() }) },
                leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) }
            )
        }
        AssistChip(
            onClick = {},
            label = {
                Text(if (state.profile?.notifications?.configured == true) "Reminders ready" else "Reminders need setup")
            },
            leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) }
        )
        state.error?.let {
            AssistChip(onClick = {}, label = { Text(it, color = MaterialTheme.colorScheme.error) })
        }
    }
}

@Composable
private fun TodayPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Today") {
        val word = state.learningPlan?.newWord
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    word?.word ?: "Waiting for a learning plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (word != null) {
                    Text(word.meaning, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (word.example.isNotBlank()) {
                        Text(
                            "\"${word.example}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(word.languageCode.uppercase()) })
                        word.frequencyRank?.let { AssistChip(onClick = {}, label = { Text("rank $it") }) }
                        word.zipfFrequency?.let { AssistChip(onClick = {}, label = { Text("zipf ${"%.2f".format(it)}") }) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.sendTestReminder() }) {
                Text("Send Test")
            }
            TextButton(onClick = { viewModel.refreshAll() }) {
                Text("Refresh")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Last 5 reminded words", fontWeight = FontWeight.SemiBold)
        state.reminders.forEach { item ->
            CompactRow(title = item.word, subtitle = item.remindedAt)
        }
    }
}

@Composable
private fun ReminderSlotsPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Reminder Slots") {
        state.reminderSlots.forEachIndexed { index, slot ->
            ReminderSlotEditor(
                slot = slot,
                onChange = { viewModel.updateReminderSlot(index, it) },
                onRemove = { viewModel.removeReminderSlot(index) }
            )
            HorizontalDivider()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.addReminderSlot() }) { Text("Add Slot") }
            Button(onClick = { viewModel.saveReminderSlots() }) { Text("Save Slots") }
        }
    }
}

@Composable
private fun ReminderSlotEditor(
    slot: ReminderSlot,
    onChange: (ReminderSlot) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = slot.label,
                onValueChange = { onChange(slot.copy(label = it)) },
                label = { Text("Label") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Switch(checked = slot.enabled, onCheckedChange = { onChange(slot.copy(enabled = it)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = slot.timeOfDay,
                onValueChange = { onChange(slot.copy(timeOfDay = it)) },
                label = { Text("HH:MM") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = slot.timezone,
                onValueChange = { onChange(slot.copy(timezone = it)) },
                label = { Text("Timezone") },
                modifier = Modifier.weight(1.4f)
            )
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun LearntPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Learnt Words") {
        if (state.learntWords.isEmpty()) Text("No learnt words yet.")
        state.learntWords.forEach { item ->
            CompactRow(
                title = "${item.word} · ${item.progressPercent}%",
                subtitle = item.meaning,
                action = { TextButton(onClick = { viewModel.resetProgress(item.wordId) }) { Text("Reset") } }
            )
        }
    }
}

@Composable
private fun NotificationHistoryPanel(state: VocabuildaryUiState) {
    SectionCard(title = "Past Notifications") {
        if (state.notificationHistory.isEmpty()) Text("No notifications in this range.")
        state.notificationHistory.forEach { item ->
            CompactRow(title = item.word, subtitle = "${item.meaning}\n${item.remindedAt}")
        }
    }
}

@Composable
private fun LearningPlanPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Learning Plan") {
        val plan = state.learningPlan
        Text("New word", fontWeight = FontWeight.SemiBold)
        if (plan?.newWord != null) WordRow(plan.newWord) else Text("No plan available.")
        if (!plan?.previousCloze?.prompt.isNullOrBlank()) {
            Text("Previous blank: ${plan?.previousCloze?.prompt}")
            Text("Answer: ${plan?.previousCloze?.answer}", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text("Blank prompt word", fontWeight = FontWeight.SemiBold)
        plan?.reviewWords.orEmpty().forEach { word ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.planEditor.clozeWordId == word.id,
                    onClick = { viewModel.updatePlanEditor { it.copy(clozeWordId = word.id) } }
                )
                Text("${word.word} · ${word.meaning}", modifier = Modifier.weight(1f))
            }
        }
        Text("Context words", fontWeight = FontWeight.SemiBold)
        plan?.reviewWords.orEmpty().forEach { word ->
            val checked = state.planEditor.contextWordIds.contains(word.id)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        viewModel.updatePlanEditor {
                            val next = if (isChecked) it.contextWordIds + word.id else it.contextWordIds - word.id
                            it.copy(contextWordIds = next, clozeWordId = it.clozeWordId.takeIf { id -> id != word.id })
                        }
                    }
                )
                Text(word.word, modifier = Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.saveLearningPlan() }) { Text("Save Plan") }
            TextButton(onClick = { viewModel.rebuildLearningPlan() }) { Text("Rebuild") }
        }
    }
}

@Composable
private fun ProgressPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Progress") {
        state.progress.forEach { item ->
            CompactRow(
                title = "${item.word} · ${item.progressPercent}%",
                subtitle = "Encounters ${item.encounterCount} · next ${item.nextDueOn ?: "not scheduled"}",
                action = { TextButton(onClick = { viewModel.resetProgress(item.wordId) }) { Text("Reset") } }
            )
        }
    }
}

@Composable
private fun CatalogPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Catalog") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.catalogForm.query,
                onValueChange = { value -> viewModel.updateCatalogForm { it.copy(query = value) } },
                label = { Text("Search") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.catalogForm.languageCode,
                onValueChange = { value -> viewModel.updateCatalogForm { it.copy(languageCode = value) } },
                label = { Text("Lang") },
                modifier = Modifier.width(92.dp)
            )
        }
        Button(onClick = { viewModel.searchCatalog() }) { Text("Search Words") }
        Text("${state.catalog.total} words")
        state.catalog.items.take(20).forEach { WordRow(it) }
        HorizontalDivider()
        Text("Languages", fontWeight = FontWeight.SemiBold)
        state.languages.forEach {
            CompactRow(title = "${it.name} (${it.code})", subtitle = "${it.wordCount} words · ${it.bookCount} books")
        }
        LanguageFormPanel(state, viewModel)
        HorizontalDivider()
        Text("Imports", fontWeight = FontWeight.SemiBold)
        Text("Words: ${state.imports.stats?.wordCount ?: 0} · frequency rows: ${state.imports.stats?.frequencyCount ?: 0}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.startFrequencyImport() }) { Text("Wordfreq") }
            TextButton(onClick = { viewModel.startKaikkiImport(false) }) { Text("Kaikki") }
        }
        state.imports.items.take(8).forEach { run ->
            CompactRow(
                title = "${run.source} · ${run.status}",
                subtitle = "processed ${run.processedItems}, inserted ${run.insertedItems}, updated ${run.updatedItems}"
            )
        }
    }
}

@Composable
private fun LanguageFormPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.languageForm.code,
            onValueChange = { value -> viewModel.updateLanguageForm { it.copy(code = value) } },
            label = { Text("New language code") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.languageForm.name,
            onValueChange = { value -> viewModel.updateLanguageForm { it.copy(name = value) } },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { viewModel.createLanguage() }) { Text("Save Language") }
    }
}

@Composable
private fun SkillsPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Language Skills") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.skills.items.forEach { skill ->
                FilterChip(
                    selected = state.selectedSkillLanguage == skill.language.code,
                    onClick = { viewModel.selectSkillLanguage(skill.language.code) },
                    label = { Text(skill.language.code) }
                )
            }
        }
        val selected = state.skills.items.firstOrNull { it.language.code == state.selectedSkillLanguage }
        if (selected != null) SkillDetail(selected, state, viewModel)
    }
}

@Composable
private fun SkillDetail(skill: LanguageSkill, state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    Text(skill.language.name, style = MaterialTheme.typography.titleMedium)
    Text("Current level: ${skill.level?.levelCode ?: "not set"} · ${bandLabel(skill.frequencyBand)}")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.skills.levels.forEach { level ->
            FilterChip(
                selected = skill.level?.levelCode == level.code,
                onClick = { viewModel.saveSkillLevel(skill.language.code, level.code) },
                label = { Text(level.code) }
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.loadQuiz(false) }) { Text("Load Quiz") }
        TextButton(onClick = { viewModel.loadQuiz(true) }) { Text("Generate") }
    }
    state.quiz?.questions?.forEach { question ->
        Text(question.questionText, fontWeight = FontWeight.SemiBold)
        question.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.quizAnswers[question.id] == index,
                    onClick = { viewModel.answerQuiz(question.id, index) }
                )
                Text(option)
            }
        }
    }
    if (state.quiz != null) {
        Button(onClick = { viewModel.submitQuiz() }) { Text("Submit Quiz") }
    }
    state.quizResult?.let { result ->
        Text("Score ${result.score}/${result.total} · level ${result.levelCode}", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BooksPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val meta = context.fileMeta(uri)
            viewModel.uploadBook(
                context.contentResolver,
                uri,
                meta.name,
                meta.mimeType,
                meta.size
            )
        }
    }
    SectionCard(title = "Books") {
        BookDraftEditor(state.bookDraft, viewModel)
        Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose and Upload Book")
        }
        state.selectedBookFileName?.let { Text("Uploading $it") }
        state.books.forEach { book ->
            BookRow(book, viewModel)
        }
    }
}

@Composable
private fun BookDraftEditor(draft: BookUploadDraft, viewModel: VocabuildaryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = { value -> viewModel.updateBookDraft { it.copy(title = value) } },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft.author,
                onValueChange = { value -> viewModel.updateBookDraft { it.copy(author = value) } },
                label = { Text("Author") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = draft.languageCode,
                onValueChange = { value -> viewModel.updateBookDraft { it.copy(languageCode = value) } },
                label = { Text("Lang") },
                modifier = Modifier.width(92.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.learningEnabled,
                onCheckedChange = { value -> viewModel.updateBookDraft { it.copy(learningEnabled = value) } }
            )
            Text("Use book words for learning")
        }
    }
}

@Composable
private fun BookRow(book: BookItem, viewModel: VocabuildaryViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(book.name, fontWeight = FontWeight.SemiBold)
            Text("${book.status} · ${book.processed.uniqueWords} unique words")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.toggleBookLearning(book) }) {
                    Text(if (book.learningEnabled) "Pause Learning" else "Use For Learning")
                }
                TextButton(onClick = { viewModel.processBook(book.id) }) { Text("Process") }
                TextButton(onClick = { viewModel.viewBookWords(book) }) { Text("Words") }
            }
        }
    }
}

@Composable
private fun SettingsPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Settings") {
        val form = state.settingsForm
        val selectedCodes = selectedLanguageCodes(form.targetLanguageCodes)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.provider == "telegram",
                onClick = { viewModel.updateSettingsForm { it.copy(provider = "telegram") } },
                label = { Text("Telegram") }
            )
            FilterChip(
                selected = form.provider == "apprise",
                onClick = { viewModel.updateSettingsForm { it.copy(provider = "apprise") } },
                label = { Text("Apprise") }
            )
        }
        OutlinedTextField(
            value = form.telegramBotToken,
            onValueChange = { value -> viewModel.updateSettingsForm { it.copy(telegramBotToken = value) } },
            label = { Text("Telegram bot token") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.telegramChatId,
            onValueChange = { value -> viewModel.updateSettingsForm { it.copy(telegramChatId = value) } },
            label = { Text("Telegram chat id") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.appriseUrls,
            onValueChange = { value -> viewModel.updateSettingsForm { it.copy(appriseUrls = value) } },
            label = { Text("Apprise URLs") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = form.learningEnabled,
                onCheckedChange = { value -> viewModel.updateSettingsForm { it.copy(learningEnabled = value) } }
            )
            Text("Learning enabled")
        }
        Text("Reminder languages", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.languages.forEach { language ->
                FilterChip(
                    selected = selectedCodes.contains(language.code),
                    onClick = {
                        viewModel.updateSettingsForm {
                            it.copy(targetLanguageCodes = toggleLanguageCode(it.targetLanguageCodes, language.code))
                        }
                    },
                    label = { Text("${language.code.uppercase()} · ${language.name}") }
                )
            }
        }
        OutlinedTextField(
            value = form.targetLanguageCodes,
            onValueChange = { value -> viewModel.updateSettingsForm { it.copy(targetLanguageCodes = value) } },
            label = { Text("Language codes") },
            modifier = Modifier.fillMaxWidth()
        )
        LearningBandsEditor(selectedCodes, state, viewModel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallField("Review", form.dailyReviewWords, Modifier.weight(1f)) {
                viewModel.updateSettingsForm { settings -> settings.copy(dailyReviewWords = it) }
            }
            SmallField("Blank", form.dailyClozeWords, Modifier.weight(1f)) {
                viewModel.updateSettingsForm { settings -> settings.copy(dailyClozeWords = it) }
            }
            SmallField("Mastery", form.masteryEncounters, Modifier.weight(1f)) {
                viewModel.updateSettingsForm { settings -> settings.copy(masteryEncounters = it) }
            }
        }
        OutlinedTextField(
            value = form.reviewIntervals,
            onValueChange = { value -> viewModel.updateSettingsForm { it.copy(reviewIntervals = value) } },
            label = { Text("Review intervals") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.saveSettings() }) { Text("Save Settings") }
            if (state.authRequired) {
                TextButton(onClick = { viewModel.logout() }) { Text("Sign Out") }
            }
        }
    }
}

@Composable
private fun LearningBandsEditor(
    selectedCodes: Set<String>,
    state: VocabuildaryUiState,
    viewModel: VocabuildaryViewModel
) {
    if (selectedCodes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        selectedCodes.forEach { languageCode ->
            val skill = state.skills.items.firstOrNull { it.language.code == languageCode }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        skill?.language?.name ?: languageCode.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Current level ${skill?.level?.levelCode ?: "not set"} · ${bandLabel(skill?.frequencyBand)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.skills.levels.forEach { level ->
                            val band = skill?.frequencyBands?.firstOrNull { it.levelCode == level.code }
                            FilterChip(
                                selected = skill?.level?.levelCode == level.code,
                                onClick = { viewModel.saveSkillLevel(languageCode, level.code) },
                                label = { Text("${level.code} ${bandRangeLabel(band)}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityPanel(state: VocabuildaryUiState, viewModel: VocabuildaryViewModel) {
    SectionCard(title = "Identity and Mobile") {
        val profile = state.profile
        Text(
            if (state.authRequired) {
                profile?.email ?: profile?.gatewaySub ?: "Signed in"
            } else {
                "Local development profile"
            },
            fontWeight = FontWeight.SemiBold
        )
        Text("Native devices: ${profile?.mobile?.enabledDeviceCount ?: 0}")
        Text("Provider ready: ${profile?.notifications?.providerConfigured == true}")
        Text("Mobile ready: ${profile?.mobile?.configured == true}")
        Button(onClick = { viewModel.refreshAll() }) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Re-register Device")
        }
        profile?.rawIdentityHeaders?.forEach { (name, value) ->
            CompactRow(title = name, subtitle = value)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun CompactRow(
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        action?.invoke()
    }
}

@Composable
private fun WordRow(word: WordItem) {
    CompactRow(
        title = word.word,
        subtitle = listOfNotNull(
            word.meaning.takeIf { it.isNotBlank() },
            word.example.takeIf { it.isNotBlank() },
            word.frequencyRank?.let { "rank $it" }
        ).joinToString("\n")
    )
}

@Composable
private fun SmallField(
    label: String,
    value: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
    )
}

private fun selectedLanguageCodes(value: String): Set<String> {
    return value.split(",", ";")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()
}

private fun toggleLanguageCode(value: String, code: String): String {
    val codes = selectedLanguageCodes(value).toMutableSet()
    if (!codes.add(code)) codes.remove(code)
    return codes.ifEmpty { setOf(code) }.joinToString(",")
}

private fun bandLabel(band: FrequencyBand?): String {
    return bandRangeLabel(band).ifBlank { "all ranked words" }
}

private fun bandRangeLabel(band: FrequencyBand?): String {
    val min = band?.minFrequencyRank
    val max = band?.maxFrequencyRank
    return when {
        min != null && max != null -> "$min-$max"
        min != null -> "$min+"
        max != null -> "1-$max"
        else -> ""
    }
}

private data class PickedFileMeta(
    val name: String,
    val size: Long?,
    val mimeType: String?
)

private fun Context.fileMeta(uri: Uri): PickedFileMeta {
    var name = "book"
    var size: Long? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return PickedFileMeta(name = name, size = size, mimeType = contentResolver.getType(uri))
}
