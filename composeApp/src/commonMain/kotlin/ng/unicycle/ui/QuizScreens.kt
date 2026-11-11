package ng.unicycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ng.unicycle.*
import ng.unicycle.data.*

private const val Letters = "ABCDEFGH"

private fun readAloudText(q: PastQuestion) =
    q.text + ". " + q.options.mapIndexed { i, o -> "Option ${Letters[i]}: $o." }.joinToString(" ")

fun formatClock(totalSeconds: Int): String {
    val m = (totalSeconds / 60).toString().padStart(2, '0')
    val s = (totalSeconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}

@Composable
fun ReportDialog(title: String, categories: List<String>, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UC.Elevated, titleContentColor = UC.Text, textContentColor = UC.Text,
        title = { Text(title, style = UcType.H2) },
        text = {
            Column {
                categories.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.RadioButton) { picked = c },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = picked == c, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = UC.Primary, unselectedColor = UC.Muted))
                        Spacer(Modifier.width(Space.Sm)); Text(c, style = UcType.Body)
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = picked.isNotEmpty(), onClick = { onSubmit(picked) }) { Text("Send report", color = if (picked.isEmpty()) UC.Muted else UC.Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = UC.Muted) } },
    )
}

val QuestionReportCategories = listOf("Wrong answer", "Ambiguous question", "Duplicate question", "Inappropriate content")

/** Action row used on every question: read aloud, stop, save, report. */
@Composable
fun QuestionActions(q: PastQuestion, extra: @Composable RowScope.() -> Unit = {}) {
    val app = LocalApp.current
    val speaker = LocalSpeaker.current
    var reporting by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }
    val saved = q.id in app.savedQuestionIds
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAction(Icons.AutoMirrored.Outlined.VolumeUp, "Read question aloud", { speaker.speak(readAloudText(q), app.prefs.speechRate) })
        IconAction(Icons.Outlined.Stop, "Stop reading", { speaker.stop() })
        IconAction(
            if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            if (saved) "Remove from saved questions" else "Save question", { app.toggleSavedQuestion(q.id) },
            tint = if (saved) UC.Primary else UC.Text,
        )
        IconAction(Icons.Outlined.Flag, "Report this question", { reporting = true })
        extra()
    }
    if (reported) Text("Thanks. Your report was saved on this device and will be sent for review once the server is connected.", style = UcType.Caption, color = UC.Muted)
    if (reporting) ReportDialog("Report this question", QuestionReportCategories, { c -> app.report("question", q.id, c); reporting = false; reported = true }, { reporting = false })
}

@Composable
fun OptionRow(index: Int, text: String, selected: Boolean, reveal: Boolean, isCorrect: Boolean, locked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        reveal && isCorrect -> UC.SuccessContainer
        reveal && selected -> UC.DangerContainer
        selected -> UC.Selected
        else -> UC.Surface
    }
    val borderColor = when {
        reveal && isCorrect -> UC.Success
        reveal && selected -> UC.Danger
        selected -> UC.Primary
        else -> UC.Border
    }
    val status = when {
        reveal && isCorrect -> "Correct answer"
        reveal && selected -> "Your answer, incorrect"
        selected -> "Selected"
        else -> ""
    }
    val okColor = UC.Success; val badColor = UC.Danger; val selColor = UC.OnSelected
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(bg)
            .border(if (selected || (reveal && isCorrect)) 2.dp else 1.dp, borderColor, shape)
            .clickable(enabled = !locked, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Option ${Letters[index]}: $text. $status" }
            .padding(horizontal = Space.Md, vertical = Space.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${Letters[index]}.", style = UcType.BodyStrong, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = UcType.Body)
            if (status.isNotEmpty() && reveal) Text(status, style = UcType.Caption, color = if (isCorrect) okColor else badColor)
        }
        // Icons so correctness is never conveyed by colour alone.
        when {
            reveal && isCorrect -> Icon(Icons.Outlined.CheckCircle, null, tint = okColor)
            reveal && selected -> Icon(Icons.Outlined.Cancel, null, tint = badColor)
            selected -> Icon(Icons.Outlined.RadioButtonChecked, null, tint = selColor)
        }
    }
}

/** Explanation levels + "why the other options are wrong". The Detailed level calls the AI service. */
@Composable
fun ExplanationPanel(q: PastQuestion, userAnswer: Int?) {
    val ai = LocalAi.current
    val nav = LocalNav.current
    var level by remember(q.id) { mutableStateOf(ExplainLevel.Simple) }
    var aiState by remember(q.id) { mutableStateOf<AiResult?>(null) }
    var loading by remember(q.id) { mutableStateOf(false) }
    var attempt by remember(q.id) { mutableIntStateOf(0) }
    val correct = userAnswer == q.correctIndex

    LaunchedEffect(level, q.id, attempt) {
        if (level == ExplainLevel.Detailed) {
            loading = true; aiState = null
            aiState = ai.run(AiTool.Tutor, ExplainLevel.Detailed, "Explain in detail: ${q.text} Correct answer: ${q.options[q.correctIndex]}")
            loading = false
        }
    }

    UcCard(container = if (correct) UC.SuccessContainer else UC.Elevated) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel, null, tint = if (correct) UC.Success else UC.Danger)
            Spacer(Modifier.width(Space.Sm))
            Text(
                when { userAnswer == null -> "Not answered"; correct -> "Correct"; else -> "Incorrect" },
                style = UcType.H2,
            )
        }
        Spacer(Modifier.height(Space.Xs))
        Text("Correct answer: ${Letters[q.correctIndex]}. ${q.options[q.correctIndex]}", style = UcType.BodyStrong)
        Spacer(Modifier.height(Space.Md))
        ChipFlow { ExplainLevel.entries.forEach { l -> UcChip(l.label, level == l) { level = l } } }
        Spacer(Modifier.height(Space.Md))
        val result = aiState
        when (level) {
            ExplainLevel.Eli10 -> Text(q.eli10, style = UcType.Body)
            ExplainLevel.Simple -> Text(q.simple, style = UcType.Body)
            ExplainLevel.University -> Text(q.university, style = UcType.Body)
            ExplainLevel.Detailed -> {
                if (loading) { SkeletonBlock(16); Spacer(Modifier.height(Space.Sm)); SkeletonBlock(16) }
                else if (result is AiResult.Failure) ErrorState(result.message, if (result.retryable) ({ attempt++ }) else null)
                else if (result is AiResult.Success) { ProvenanceBadge(Provenance.Ai); Spacer(Modifier.height(Space.Sm)); Text(result.text, style = UcType.Body) }
            }
        }
        Spacer(Modifier.height(Space.Md))
        Text("Why the other options are wrong", style = UcType.BodyStrong)
        Text(q.whyOthersWrong, style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        SecondaryButton("Ask the tutor", Modifier.fillMaxWidth(), icon = Icons.Outlined.Psychology) { nav.push(Route.AiToolScreen(AiTool.Tutor)) }
    }
}

@Composable
private fun QuizGrid(session: QuizSession) {
    val primary = UC.Primary; val onPrimary = UC.OnPrimary; val selected = UC.Selected; val onSelected = UC.OnSelected
    val surface = UC.Surface; val border = UC.Border; val text = UC.Text; val amber = UC.Amber
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until session.total).chunked(7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { i ->
                    val isCurrent = i == session.current
                    val answered = session.answers.containsKey(i)
                    val flagged = i in session.flagged
                    val desc = "Question ${i + 1}, ${if (answered) "answered" else "not answered"}${if (flagged) ", marked for review" else ""}${if (isCurrent) ", current" else ""}"
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) primary else if (answered) selected else surface)
                            .border(if (flagged) 2.dp else 1.dp, if (flagged) amber else border, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { session.goTo(i) }
                            .semantics { contentDescription = desc },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = UcType.BodySmall, color = if (isCurrent) onPrimary else if (answered) onSelected else text)
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("Filled = answered · amber outline = marked for review", style = UcType.Caption, color = UC.Muted)
    }
}

@Composable
fun QuizScreen(session: QuizSession) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val speaker = LocalSpeaker.current
    var leaving by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun finish() {
        if (finished) return
        finished = true
        speaker.stop()
        val attempt = session.toAttempt(app.newId(), app.today())
        app.recordAttempt(attempt)
        nav.replaceTop(Route.Results(attempt.id))
    }

    PlatformBackHandler(enabled = true) { leaving = true }

    // Autosave progress after every change.
    LaunchedEffect(session.current, session.answers.size, session.checked.size, session.flagged.size) {
        if (!finished) app.saveSnapshot(session.toSnapshot())
    }
    // Timer for mock exams.
    LaunchedEffect(session) {
        while (session.timed && session.secondsLeft > 0 && !finished) { delay(1000); session.secondsLeft-- }
        if (session.timed && session.secondsLeft <= 0) finish()
    }
    if (session.questions.isEmpty()) {
        ScreenScaffold(session.title, { nav.pop() }) { EmptyState(Icons.Outlined.Quiz, "No questions", "There are no questions for this quiz.") }
        return
    }

    val q = session.questions[session.current]
    val feedback = session.showsFeedback(session.current)
    val locked = session.isLocked(session.current)
    val isLast = session.current == session.total - 1
    LaunchedEffect(q.id) { if (app.prefs.autoRead) speaker.speak(readAloudText(q), app.prefs.speechRate) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.Xs), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Outlined.Close, "Leave quiz", { leaving = true })
            Column(Modifier.weight(1f)) {
                Text(session.title, style = UcType.BodyStrong, maxLines = 1)
                Text("Question ${session.current + 1} of ${session.total}", style = UcType.Caption, color = UC.Muted)
            }
            if (session.timed) {
                val low = session.secondsLeft < 60
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = Space.Md).semantics { contentDescription = "Time left ${formatClock(session.secondsLeft)}" },
                ) {
                    Icon(Icons.Outlined.Timer, null, tint = if (low) UC.Danger else UC.Muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Space.Xs))
                    Text(formatClock(session.secondsLeft), style = UcType.H2.copy(fontFeatureSettings = "tnum"), color = if (low) UC.Danger else UC.Text)
                }
            }
        }
        UcProgress((session.current + 1) / session.total.toFloat(), Modifier.padding(horizontal = Space.Page))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Space.Page)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Sm), verticalAlignment = Alignment.CenterVertically) {
                ProvenanceBadge(q.provenance)
                Text("${q.topic} · ${q.difficulty.label}", style = UcType.Caption, color = UC.Muted)
            }
            Spacer(Modifier.height(Space.Md))
            Text(q.text, style = UcType.H1)
            Spacer(Modifier.height(Space.Lg))
            q.options.forEachIndexed { i, o ->
                OptionRow(i, o, session.answers[session.current] == i, feedback, i == q.correctIndex, locked) { session.select(i) }
                Spacer(Modifier.height(Space.Sm))
            }
            QuestionActions(q) {
                val flagged = session.current in session.flagged
                IconAction(
                    if (flagged) Icons.Outlined.Flag else Icons.Outlined.OutlinedFlag,
                    if (flagged) "Remove review mark" else "Mark for review", { session.toggleFlag() },
                    tint = if (flagged) UC.Amber else UC.Text,
                )
            }
            if (feedback) { Spacer(Modifier.height(Space.Md)); ExplanationPanel(q, session.answers[session.current]) }
            if (session.total > 1) { Spacer(Modifier.height(Space.Lg)); QuizGrid(session) }
            Spacer(Modifier.height(Space.Xl))
        }

        Row(Modifier.fillMaxWidth().background(UC.Surface).padding(Space.Page), horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
            SecondaryButton("Previous", Modifier.weight(1f), enabled = session.current > 0) { speaker.stop(); session.goTo(session.current - 1) }
            val needsCheck = session.mode != QuizMode.Exam && !feedback
            if (needsCheck) {
                PrimaryButton("Check answer", Modifier.weight(1f), enabled = session.answers.containsKey(session.current)) { session.check() }
            } else if (isLast) {
                PrimaryButton("Finish", Modifier.weight(1f)) { if (session.answeredCount < session.total) submitting = true else finish() }
            } else {
                PrimaryButton("Next", Modifier.weight(1f)) { speaker.stop(); session.goTo(session.current + 1) }
            }
        }
    }

    if (leaving) ConfirmDialog(
        "Leave this quiz?", "Your progress is saved. You can resume it from Home or Learn.", "Leave",
        { leaving = false; speaker.stop(); nav.pop() }, { leaving = false }, dismissLabel = "Keep going",
    )
    if (submitting) ConfirmDialog(
        "Submit quiz?", "${session.total - session.answeredCount} questions are unanswered and will count as incorrect.",
        "Submit", { submitting = false; finish() }, { submitting = false }, dismissLabel = "Keep working",
    )
}

@Composable
fun ResultsScreen(attemptId: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val attempt = app.attempts.firstOrNull { it.id == attemptId }
    if (attempt == null) {
        ScreenScaffold("Results", { nav.pop() }) { ErrorState("This result couldn't be found.") }
        return
    }
    val questions = attempt.questionIds.mapNotNull { app.content.question(it) }
    val missed = attempt.questionIds.filterIndexed { i, id -> attempt.answers[i] != app.content.question(id)?.correctIndex }
    val pct = if (attempt.total == 0) 0 else attempt.correct * 100 / attempt.total
    val isAptitude = attempt.courseCode == PrototypeContent.APT_NUM

    ScreenScaffold("Results", { nav.pop() }) {
        UcCard {
            Text(attempt.title, style = UcType.Caption, color = UC.Muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${attempt.correct}", style = UcType.Display.copy(fontFeatureSettings = "tnum"))
                Text(" / ${attempt.total}", style = UcType.H1, color = UC.Muted)
            }
            Text(if (isAptitude) "Reasoning score: $pct% (practice only, not an IQ score)" else "Accuracy: $pct%", style = UcType.Body)
            Text("Time used: ${formatClock(attempt.secondsUsed)}", style = UcType.BodySmall, color = UC.Muted)
        }

        SectionHeader("By topic")
        attempt.topics.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Space.Xs)) {
                Text(t.topic, Modifier.weight(1f), style = UcType.Body)
                Text("${t.correct}/${t.total}", style = UcType.BodyStrong)
            }
            UcProgress(if (t.total == 0) 0f else t.correct.toFloat() / t.total)
        }
        val weak = attempt.topics.filter { it.correct < it.total }.map { it.topic }
        Spacer(Modifier.height(Space.Md))
        Text(
            if (weak.isEmpty()) "Strong across every topic in this quiz." else "Areas to improve: ${weak.joinToString(", ")}.",
            style = UcType.Body, color = UC.Muted,
        )

        Spacer(Modifier.height(Space.Lg))
        if (missed.isNotEmpty()) {
            PrimaryButton("Retry missed questions (${missed.size})", Modifier.fillMaxWidth(), icon = Icons.Outlined.Replay) {
                val list = missed.mapNotNull { app.content.question(it) }
                nav.replaceTop(Route.Quiz(QuizSession("${attempt.courseCode} · Retry", attempt.courseCode, QuizMode.Revision, list, 0)))
            }
            Spacer(Modifier.height(Space.Sm))
        }
        SecondaryButton("Done", Modifier.fillMaxWidth()) { nav.pop() }

        SectionHeader("Review")
        questions.forEachIndexed { i, q ->
            val ans = attempt.answers[i]
            val ok = ans == q.correctIndex
            UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.QuestionDetail(q.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel, null, tint = if (ok) UC.Success else UC.Danger)
                    Spacer(Modifier.width(Space.Sm))
                    Text(if (ans == null) "Not answered" else if (ok) "Correct" else "Incorrect", style = UcType.BodyStrong)
                }
                Spacer(Modifier.height(Space.Xs))
                Text("${i + 1}. ${q.text}", style = UcType.Body)
                if (!ok) Text("Your answer: ${ans?.let { q.options[it] } ?: "none"}", style = UcType.BodySmall, color = UC.Muted)
                Text("Correct answer: ${q.options[q.correctIndex]}", style = UcType.BodySmall, color = UC.Success)
                Text("Tap for explanations", style = UcType.Caption, color = UC.Primary)
            }
        }
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun QuestionDetailScreen(id: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val speaker = LocalSpeaker.current
    val q = app.content.question(id)
    if (q == null) {
        ScreenScaffold("Question", { nav.pop() }) { ErrorState("This question couldn't be found.") }
        return
    }
    var picked by remember(id) { mutableStateOf<Int?>(null) }
    var revealed by remember(id) { mutableStateOf(false) }

    ScreenScaffold("${q.courseCode} question", { speaker.stop(); nav.pop() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Sm), verticalAlignment = Alignment.CenterVertically) {
            ProvenanceBadge(q.provenance)
            Text("${q.topic} · ${q.difficulty.label}", style = UcType.Caption, color = UC.Muted)
        }
        Text("Source: ${q.source}${q.year?.let { " · $it" } ?: ""}", style = UcType.Caption, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        Text(q.text, style = UcType.H1)
        Spacer(Modifier.height(Space.Lg))
        q.options.forEachIndexed { i, o ->
            OptionRow(i, o, picked == i, revealed, i == q.correctIndex, revealed) { picked = i }
            Spacer(Modifier.height(Space.Sm))
        }
        QuestionActions(q)
        Spacer(Modifier.height(Space.Sm))
        if (!revealed) {
            PrimaryButton("Check answer", Modifier.fillMaxWidth(), enabled = picked != null) { revealed = true }
        } else {
            ExplanationPanel(q, picked)
            Spacer(Modifier.height(Space.Md))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Sm)) {
                SecondaryButton("Rewrite", Modifier.weight(1f)) { nav.push(Route.AiToolScreen(AiTool.Rewrite)) }
                SecondaryButton("Related questions", Modifier.weight(1f)) { nav.push(Route.AiToolScreen(AiTool.Generate)) }
            }
        }
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun PastQuestionsScreen(initialCourse: String?) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val courses = app.content.courses().filter { app.content.questions(it.code).isNotEmpty() }
    var course by remember { mutableStateOf(initialCourse ?: "All") }
    var topic by remember { mutableStateOf("All topics") }
    val all = app.content.questions(if (course == "All") null else course)
    val topics = listOf("All topics") + all.map { it.topic }.distinct()
    val shown = all.filter { topic == "All topics" || it.topic == topic }

    ScreenScaffold("Past questions", { nav.pop() }) {
        PrototypeBanner("Sample questions are labelled \"Under review\". They are not official past papers.")
        Spacer(Modifier.height(Space.Md))
        ChipRow(listOf("All") + courses.map { it.code }, course) { course = it; topic = "All topics" }
        Spacer(Modifier.height(Space.Sm))
        ChipRow(topics, topic) { topic = it }
        Spacer(Modifier.height(Space.Md))
        if (shown.isEmpty()) EmptyState(Icons.Outlined.Quiz, "No questions match", "Try another course or topic.")
        shown.forEach { q ->
            UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.QuestionDetail(q.id)) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.Sm), verticalAlignment = Alignment.CenterVertically) {
                    ProvenanceBadge(q.provenance)
                    Text("${q.courseCode}${q.year?.let { " · $it" } ?: ""} · ${q.difficulty.label}", style = UcType.Caption, color = UC.Muted)
                }
                Spacer(Modifier.height(Space.Sm))
                Text(q.text, style = UcType.Body)
                Text(q.topic, style = UcType.Caption, color = UC.Muted)
            }
        }
    }
}

@Composable
fun SavedScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val saved = app.savedQuestionIds.mapNotNull { app.content.question(it) }
    ScreenScaffold("Saved questions", { nav.pop() }) {
        if (saved.isEmpty()) {
            EmptyState(Icons.Outlined.BookmarkBorder, "Nothing saved", "Tap the bookmark on any question to keep it here for revision.")
        } else {
            PrimaryButton("Practise saved questions", Modifier.fillMaxWidth()) {
                nav.push(Route.Quiz(QuizSession("Saved questions", saved.first().courseCode, QuizMode.Practice, saved, 0)))
            }
            Spacer(Modifier.height(Space.Md))
            saved.forEach { q ->
                UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.QuestionDetail(q.id)) }) {
                    Text("${q.courseCode} · ${q.topic}", style = UcType.Caption, color = UC.Muted)
                    Text(q.text, style = UcType.Body)
                    TertiaryButton("Remove") { app.toggleSavedQuestion(q.id) }
                }
            }
        }
    }
}
