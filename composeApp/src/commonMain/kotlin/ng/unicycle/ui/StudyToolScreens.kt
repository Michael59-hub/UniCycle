package ng.unicycle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ng.unicycle.*
import ng.unicycle.data.*

// ---------------------------------------------------------------------------------------
// AI tools (provider-agnostic; shows honest loading / failure states)
// ---------------------------------------------------------------------------------------

@Composable
fun AiHubScreen() {
    val ai = LocalAi.current
    val nav = LocalNav.current
    ScreenScaffold("AI tools", { nav.pop() }) {
        if (!ai.connected) PrototypeBanner("The AI service isn't connected in this build. The screens are ready, but requests will fail until a server provider is configured.")
        Spacer(Modifier.height(Space.Md))
        AiTool.entries.forEach { t ->
            ListRow(t.label, t.blurb, Icons.Outlined.AutoAwesome, { nav.push(Route.AiToolScreen(t)) }) { Icon(Icons.Outlined.ChevronRight, null, tint = UC.Muted) }
        }
        Spacer(Modifier.height(Space.Md))
        Text("AI answers are always labelled \"AI generated\" and are never treated as verified. Stored explanations on questions work without AI.", style = UcType.BodySmall, color = UC.Muted)
    }
}

@Composable
fun AiToolScreen(tool: AiTool) {
    val app = LocalApp.current
    val ai = LocalAi.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(ExplainLevel.Simple) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AiResult?>(null) }
    var reporting by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }

    fun run() {
        loading = true; result = null
        scope.launch { result = ai.run(tool, level, input); loading = false }
    }

    ScreenScaffold(tool.label, { nav.pop() }) {
        Text(tool.blurb, style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        if (tool == AiTool.Tutor || tool == AiTool.Summarize) {
            Text("Explanation style", style = UcType.BodyStrong)
            Spacer(Modifier.height(Space.Sm))
            ChipFlow { ExplainLevel.entries.filter { it != ExplainLevel.Detailed || tool == AiTool.Tutor }.forEach { l -> UcChip(l.label, level == l) { level = l } } }
            Spacer(Modifier.height(Space.Md))
        }
        UcTextField(input, { input = it }, tool.hint, singleLine = false, minLines = 4)
        Spacer(Modifier.height(Space.Md))
        PrimaryButton("Generate", Modifier.fillMaxWidth(), enabled = input.isNotBlank(), loading = loading, icon = Icons.Outlined.AutoAwesome) { run() }
        Spacer(Modifier.height(Space.Sm))
        Text("Requests are rate-limited by the server to keep the service fair.", style = UcType.Caption, color = UC.Muted)
        Spacer(Modifier.height(Space.Lg))
        if (loading) { SkeletonBlock(18); Spacer(Modifier.height(Space.Sm)); SkeletonBlock(18); Spacer(Modifier.height(Space.Sm)); SkeletonBlock(18) }
        val r = result
        if (r is AiResult.Failure) ErrorState(r.message, if (r.retryable) ({ run() }) else null)
        if (r is AiResult.Success) {
            UcCard {
                ProvenanceBadge(Provenance.Ai)
                Spacer(Modifier.height(Space.Sm))
                Text(r.text, style = UcType.Body)
                Spacer(Modifier.height(Space.Sm))
                if (reported) Text("Thanks, your report was saved.", style = UcType.Caption, color = UC.Muted)
                else TertiaryButton("Report an inaccurate or harmful answer") { reporting = true }
            }
        }
        Spacer(Modifier.height(Space.Xl))
    }
    if (reporting) ReportDialog(
        "Report this AI answer", listOf("Incorrect", "Harmful or unsafe", "Made-up sources", "Other"),
        { c -> app.report("ai", tool.name, c); reporting = false; reported = true }, { reporting = false },
    )
}

// ---------------------------------------------------------------------------------------
// Flashcards (simple spaced repetition)
// ---------------------------------------------------------------------------------------

@Composable
fun FlashcardsScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var adding by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    val due = app.dueCards()

    ScreenScaffold("Flashcards", { nav.pop() }) {
        Text("${app.cards.size} cards · ${due.size} due today", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        PrimaryButton(if (due.isEmpty()) "No cards due" else "Review ${due.size} due", Modifier.fillMaxWidth(), enabled = due.isNotEmpty(), icon = Icons.Outlined.Style) { nav.push(Route.FlashcardReview) }
        Spacer(Modifier.height(Space.Sm))
        SecondaryButton(if (adding) "Close" else "New card", Modifier.fillMaxWidth(), icon = if (adding) Icons.Outlined.Close else Icons.Outlined.Add) { adding = !adding }
        if (adding) {
            Spacer(Modifier.height(Space.Md))
            UcCard {
                UcTextField(front, { front = it }, "Front (question)", singleLine = false, minLines = 2)
                Spacer(Modifier.height(Space.Md))
                UcTextField(back, { back = it }, "Back (answer)", singleLine = false, minLines = 2)
                Spacer(Modifier.height(Space.Md))
                DropdownField("Course (optional)", course, app.myCourses.map { it.code }, { course = it }, placeholder = "No course")
                Spacer(Modifier.height(Space.Md))
                PrimaryButton("Save card", Modifier.fillMaxWidth(), enabled = front.isNotBlank() && back.isNotBlank()) {
                    app.addCard(front, back, course); front = ""; back = ""; adding = false
                }
            }
        }
        SectionHeader("All cards")
        if (app.cards.isEmpty()) EmptyState(Icons.Outlined.Style, "No flashcards yet", "Create a card for anything you want to remember. Cards come back on a spaced schedule.")
        app.cards.forEach { c ->
            UcCard(Modifier.padding(bottom = Space.Md)) {
                Text(c.front, style = UcType.BodyStrong)
                Text(c.back, style = UcType.Body, color = UC.Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (c.courseCode.isBlank()) "No course" else c.courseCode, style = UcType.Caption, color = UC.Muted, modifier = Modifier.weight(1f))
                    TertiaryButton("Delete") { app.deleteCard(c.id) }
                }
            }
        }
    }
}

@Composable
fun FlashcardReviewScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val queue = remember { app.dueCards().map { it.id } }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    val card = queue.getOrNull(index)?.let { id -> app.cards.firstOrNull { it.id == id } }

    ScreenScaffold("Review", { nav.pop() }) {
        if (card == null) {
            EmptyState(Icons.Outlined.CheckCircle, "All caught up", "You've reviewed every card that was due.", "Done") { nav.pop() }
        } else {
            Text("Card ${index + 1} of ${queue.size}", style = UcType.Caption, color = UC.Muted)
            UcProgress((index + 1) / queue.size.toFloat())
            Spacer(Modifier.height(Space.Lg))
            UcCard(onClick = { shown = !shown }) {
                Text(if (shown) "Answer" else "Question", style = UcType.Caption, color = UC.Primary)
                Spacer(Modifier.height(Space.Sm))
                Text(if (shown) card.back else card.front, style = UcType.H1)
                Spacer(Modifier.height(Space.Md))
                Text(if (shown) "Tap to see the question" else "Tap to reveal the answer", style = UcType.Caption, color = UC.Muted)
            }
            Spacer(Modifier.height(Space.Lg))
            if (!shown) {
                PrimaryButton("Show answer", Modifier.fillMaxWidth()) { shown = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
                    SecondaryButton("Needs revision", Modifier.weight(1f), icon = Icons.Outlined.Replay) { app.reviewCard(card.id, false); shown = false; index++ }
                    PrimaryButton("Known", Modifier.weight(1f), icon = Icons.Outlined.Check) { app.reviewCard(card.id, true); shown = false; index++ }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Notes
// ---------------------------------------------------------------------------------------

@Composable
fun NotesScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var q by remember { mutableStateOf("") }
    val shown = app.notes.filter { q.isBlank() || it.title.contains(q, true) || it.body.contains(q, true) }.sortedByDescending { it.day }
    ScreenScaffold("Notes", { nav.pop() }) {
        PrimaryButton("New note", Modifier.fillMaxWidth(), icon = Icons.Outlined.Add) { nav.push(Route.NoteEdit(null)) }
        Spacer(Modifier.height(Space.Md))
        if (app.notes.isNotEmpty()) { UcSearchField(q, { q = it }, "Search your notes"); Spacer(Modifier.height(Space.Md)) }
        if (app.notes.isEmpty()) EmptyState(Icons.Outlined.EditNote, "No notes yet", "Write notes for a course. They're saved on this device and work offline.")
        else if (shown.isEmpty()) EmptyState(Icons.Outlined.SearchOff, "No matching notes", "Try different words.")
        shown.forEach { n ->
            UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.NoteEdit(n.id)) }) {
                Text(n.title, style = UcType.H2)
                Text(n.body.take(120), style = UcType.BodySmall, color = UC.Muted, maxLines = 3)
                if (n.courseCode.isNotBlank()) Text(n.courseCode, style = UcType.Caption, color = UC.Primary)
            }
        }
    }
}

@Composable
fun NoteEditScreen(id: String?, presetCourse: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val existing = id?.let { i -> app.notes.firstOrNull { it.id == i } }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var course by remember { mutableStateOf(existing?.courseCode ?: presetCourse) }
    var confirmDelete by remember { mutableStateOf(false) }
    var shared by remember { mutableStateOf(false) }

    fun save(): Note {
        val n = Note(existing?.id ?: app.newId(), title.trim(), body.trim(), course, app.today())
        app.upsertNote(n); return n
    }

    ScreenScaffold(if (existing == null) "New note" else "Edit note", { nav.pop() }) {
        UcTextField(title, { title = it }, "Title", error = if (title.isBlank()) "Add a title" else null)
        Spacer(Modifier.height(Space.Md))
        UcTextField(body, { body = it }, "Write your note", singleLine = false, minLines = 8)
        Spacer(Modifier.height(Space.Md))
        DropdownField("Course (optional)", course, app.myCourses.map { it.code }, { course = it }, placeholder = "No course")
        Spacer(Modifier.height(Space.Lg))
        PrimaryButton("Save note", Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { save(); nav.pop() }
        if (course.isNotBlank()) {
            Spacer(Modifier.height(Space.Sm))
            SecondaryButton(
                if (shared) "Shared to $course hall" else "Share to $course Lecture Hall", Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !shared,
            ) {
                val n = save(); app.publishPost(PostType.Resource, course, "Notes: ${n.title}\n\n${n.body}"); shared = true
            }
        }
        if (existing != null) { Spacer(Modifier.height(Space.Sm)); DestructiveButton("Delete note", Modifier.fillMaxWidth()) { confirmDelete = true } }
        Spacer(Modifier.height(Space.Xl))
    }
    if (confirmDelete && existing != null) ConfirmDialog(
        "Delete this note?", "This can't be undone.", "Delete",
        { confirmDelete = false; app.deleteNote(existing.id); nav.pop() }, { confirmDelete = false }, destructive = true,
    )
}

// ---------------------------------------------------------------------------------------
// Planner
// ---------------------------------------------------------------------------------------

private fun dayLabel(day: Long, today: Long) = when {
    day < today -> "Overdue"
    day == today -> "Today"
    day == today + 1 -> "Tomorrow"
    else -> "In ${day - today} days"
}

@Composable
fun PlannerScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var adding by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Upcoming") }
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var offset by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    val today = app.today()

    val suggestions = app.missedQuestionIds().mapNotNull { app.content.question(it) }.map { it.courseCode to it.topic }.distinct().take(4)
    val tasks = app.tasks.filter {
        when (filter) { "Today" -> it.day == today && !it.done; "Done" -> it.done; else -> !it.done }
    }.sortedBy { it.day }

    ScreenScaffold("Study planner", { nav.pop() }) {
        ChipRow(listOf("Today", "Upcoming", "Done"), filter) { filter = it }
        Spacer(Modifier.height(Space.Md))
        SecondaryButton(if (adding) "Close" else "Add task", Modifier.fillMaxWidth(), icon = if (adding) Icons.Outlined.Close else Icons.Outlined.Add) { adding = !adding }
        if (suggestions.isNotEmpty() && !adding) {
            SectionHeader("Suggested revision")
            Text("Based on questions you missed.", style = UcType.BodySmall, color = UC.Muted)
            Spacer(Modifier.height(Space.Sm))
            ChipFlow { suggestions.forEach { (c, t) -> UcChip("$c: $t", false) { adding = true; title = "Revise $t"; course = c } } }
        }
        if (adding) {
            Spacer(Modifier.height(Space.Md))
            UcCard {
                UcTextField(title, { title = it }, "What will you study?")
                Spacer(Modifier.height(Space.Md))
                DropdownField("Course (optional)", course, app.myCourses.map { it.code }, { course = it }, placeholder = "No course")
                Spacer(Modifier.height(Space.Md))
                Text("When", style = UcType.BodyStrong); Spacer(Modifier.height(Space.Sm))
                ChipFlow { listOf("Today" to 0, "Tomorrow" to 1, "In 3 days" to 3, "Next week" to 7).forEach { (l, o) -> UcChip(l, offset == o) { offset = o } } }
                Spacer(Modifier.height(Space.Md))
                Text("Duration", style = UcType.BodyStrong); Spacer(Modifier.height(Space.Sm))
                ChipFlow { listOf(15, 30, 60, 90).forEach { m -> UcChip("$m min", minutes == m) { minutes = m } } }
                Spacer(Modifier.height(Space.Lg))
                PrimaryButton("Save task", Modifier.fillMaxWidth(), enabled = title.isNotBlank()) {
                    app.addTask(StudyTask(app.newId(), title.trim(), course, today + offset, minutes)); title = ""; course = ""; adding = false
                }
            }
        }
        Spacer(Modifier.height(Space.Md))
        if (tasks.isEmpty()) EmptyState(Icons.Outlined.CalendarMonth, "No tasks here", if (filter == "Done") "Completed tasks appear here." else "Plan a short session to get started.")
        tasks.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ListRow(
                        t.title, "${dayLabel(t.day, today)} · ${t.minutes} min${if (t.courseCode.isNotBlank()) " · ${t.courseCode}" else ""}",
                        if (t.done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, { app.toggleTask(t.id) },
                    )
                }
                IconAction(Icons.Outlined.Delete, "Delete task ${t.title}", { app.deleteTask(t.id) }, tint = UC.Muted)
            }
        }
        Spacer(Modifier.height(Space.Sm))
        Text("Reminders will be added once notifications are connected.", style = UcType.Caption, color = UC.Muted)
    }
}

// ---------------------------------------------------------------------------------------
// Offline library
// ---------------------------------------------------------------------------------------

@Composable
fun OfflineLibraryScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    ScreenScaffold("Offline library", { nav.pop() }) {
        Text("Everything here opens without a network connection.", style = UcType.Body, color = UC.Muted)
        SectionHeader("Course content")
        app.content.courses().forEach { c ->
            val n = app.content.questions(c.code).size
            ListRow(
                "${c.code} · ${c.title}", if (n == 0) "No questions in the prototype set" else "$n questions with explanations",
                Icons.AutoMirrored.Outlined.MenuBook, { nav.push(Route.Course(c.code)) },
            ) { if (n > 0) Icon(Icons.Outlined.OfflinePin, "Available offline", tint = UC.Success) }
        }
        SectionHeader("Your data")
        ListRow("Notes", "${app.notes.size} saved on this device", Icons.Outlined.EditNote, { nav.push(Route.Notes) })
        ListRow("Flashcards", "${app.cards.size} saved on this device", Icons.Outlined.Style, { nav.push(Route.Flashcards) })
        ListRow("Study tasks", "${app.tasks.size} saved on this device", Icons.Outlined.CalendarMonth, { nav.push(Route.Planner) })
        ListRow("Saved questions", "${app.savedQuestionIds.size} saved", Icons.Outlined.BookmarkBorder, { nav.push(Route.Saved) })
        Spacer(Modifier.height(Space.Lg))
        PrototypeBanner("Server downloads (with file sizes, quality choices and Wi-Fi-only rules) arrive with the backend. Prototype content is bundled, so it is always available offline.")
        Spacer(Modifier.height(Space.Xl))
    }
}
