package ng.unicycle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ng.unicycle.*
import ng.unicycle.data.*

private data class Tool(val label: String, val icon: ImageVector, val route: Route)

fun sessionFromSnapshot(app: AppStore, s: QuizSnapshot): QuizSession =
    QuizSession(s.title, s.courseCode, s.mode, s.questionIds.mapNotNull { app.content.question(it) }, s.totalSeconds, s)

@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val today = app.today()
    val todaysTasks = app.tasks.filter { it.day == today }
    val resume = app.inProgress
    val lastCourse = app.content.courses().firstOrNull { it.code == app.lastCourse } ?: app.myCourses.firstOrNull()
    val latestPost = app.posts.firstOrNull { !it.hidden }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.Page)) {
        Spacer(Modifier.height(Space.Lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hello, ${app.firstName}", style = UcType.Display)
                Text("${app.profile.institutionName} · ${app.profile.level} level", style = UcType.BodySmall, color = UC.Muted)
            }
            Avatar(app.firstName, 44)
        }
        Spacer(Modifier.height(Space.Lg))
        UcCard(onClick = { nav.push(Route.Search) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, null, tint = UC.Muted)
                Spacer(Modifier.width(Space.Md))
                Text("Search courses, questions, notes", style = UcType.Body, color = UC.Muted)
            }
        }
        Spacer(Modifier.height(Space.Md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
            StatTile("Day streak", "${app.streak}", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f), tint = UC.Amber)
            StatTile("XP · level ${app.xpLevel}", "${app.xp}", Icons.Outlined.Bolt, Modifier.weight(1f), tint = UC.Amber)
        }

        SectionHeader("Continue learning")
        when {
            resume != null -> UcCard(onClick = { nav.push(Route.Quiz(sessionFromSnapshot(app, resume))) }) {
                Text("Resume: ${resume.title}", style = UcType.H2)
                Text("${resume.answers.size} of ${resume.questionIds.size} answered · ${resume.mode.label}", style = UcType.BodySmall, color = UC.Muted)
            }
            lastCourse != null -> UcCard(onClick = { nav.push(Route.Course(lastCourse.code)) }) {
                Text("${lastCourse.code} · ${lastCourse.title}", style = UcType.H2)
                Spacer(Modifier.height(Space.Sm))
                UcProgress(app.courseProgress(lastCourse.code))
            }
            else -> EmptyState(
                Icons.AutoMirrored.Outlined.MenuBook, "No courses yet", "Add your courses to see your progress here.", "Browse courses",
            ) { nav.goTab(Tab.Learn); nav.push(Route.Discover) }
        }

        SectionHeader("Today's study plan", "Planner") { nav.push(Route.Planner) }
        if (todaysTasks.isEmpty()) {
            Text("Nothing planned for today. Add a task to build your routine.", style = UcType.Body, color = UC.Muted)
        } else todaysTasks.forEach { t ->
            ListRow(
                t.title, "${t.minutes} min${if (t.courseCode.isNotBlank()) " · ${t.courseCode}" else ""}",
                if (t.done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, { app.toggleTask(t.id) },
            )
        }

        SectionHeader("Study toolkit")
        val tools = listOf(
            Tool("Past questions", Icons.Outlined.Quiz, Route.PastQuestions(null)),
            Tool("AI tools", Icons.Outlined.AutoAwesome, Route.AiHub),
            Tool("Flashcards", Icons.Outlined.Style, Route.Flashcards),
            Tool("Notes", Icons.Outlined.EditNote, Route.Notes),
            Tool("Planner", Icons.Outlined.CalendarMonth, Route.Planner),
            Tool("Saved", Icons.Outlined.BookmarkBorder, Route.Saved),
        )
        tools.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Md), modifier = Modifier.padding(bottom = Space.Md)) {
                row.forEach { t ->
                    UcCard(Modifier.weight(1f), onClick = { nav.push(t.route) }) {
                        Icon(t.icon, null, tint = UC.Primary)
                        Spacer(Modifier.height(Space.Sm))
                        Text(t.label, style = UcType.BodySmall)
                    }
                }
            }
        }

        SectionHeader("Community", "Open") { nav.goTab(Tab.Community) }
        if (latestPost == null) {
            Text("No posts yet. Start a discussion in one of your course halls.", style = UcType.Body, color = UC.Muted)
        } else PostCard(latestPost, compact = true)

        SectionHeader("Competitions")
        UcCard(onClick = { nav.goTab(Tab.Compete) }) {
            Text("Tournaments and leaderboards aren't live yet", style = UcType.H2)
            Text("They need server-controlled scoring to be fair. See how rankings will work.", style = UcType.BodySmall, color = UC.Muted)
        }
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun LearnScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val resume = app.inProgress
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.Page)) {
        PageTitle("Learn", "Your academic workspace")
        if (app.content.isPrototype) PrototypeBanner("Prototype content: sample questions are labelled \"Under review\" and are not official past papers.")
        if (resume != null) {
            SectionHeader("In progress")
            UcCard(onClick = { nav.push(Route.Quiz(sessionFromSnapshot(app, resume))) }) {
                Text(resume.title, style = UcType.H2)
                Text("${resume.answers.size}/${resume.questionIds.size} answered · tap to resume", style = UcType.BodySmall, color = UC.Muted)
            }
        }
        SectionHeader("My courses", "Add") { nav.push(Route.Discover) }
        if (app.myCourses.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Outlined.MenuBook, "No courses added",
                "Add the courses you're taking to see past questions and progress.", "Browse courses",
            ) { nav.push(Route.Discover) }
        } else app.myCourses.forEach { c ->
            UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.Course(c.code)) }) {
                Text(c.code, style = UcType.Caption, color = UC.Muted)
                Text(c.title, style = UcType.H2)
                Spacer(Modifier.height(Space.Sm))
                UcProgress(app.courseProgress(c.code))
            }
        }
        SectionHeader("Tools")
        ListRow("Past questions", "Browse by course and topic", Icons.Outlined.Quiz, { nav.push(Route.PastQuestions(null)) })
        ListRow("AI tools", "Tutor, rewriter, generator, summaries", Icons.Outlined.AutoAwesome, { nav.push(Route.AiHub) })
        ListRow("Flashcards", "${app.dueCards().size} due today", Icons.Outlined.Style, { nav.push(Route.Flashcards) })
        ListRow("Notes", "${app.notes.size} saved", Icons.Outlined.EditNote, { nav.push(Route.Notes) })
        ListRow("Study planner", null, Icons.Outlined.CalendarMonth, { nav.push(Route.Planner) })
        ListRow("Saved questions", "${app.savedQuestionIds.size} saved", Icons.Outlined.BookmarkBorder, { nav.push(Route.Saved) })
        ListRow("Offline library", "What works without data", Icons.Outlined.CloudDownload, { nav.push(Route.OfflineLibrary) })
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun CourseDetailScreen(code: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val course = app.content.courses().firstOrNull { it.code == code }
    if (course == null) {
        ScreenScaffold("Course", { nav.pop() }) { ErrorState("This course couldn't be found.") }
        return
    }
    val qs = app.content.questions(code)
    val added = code in app.profile.courseCodes
    var error by remember { mutableStateOf<String?>(null) }
    val mastered = app.masteredIds()

    ScreenScaffold(course.code, { nav.pop() }) {
        Text(course.title, style = UcType.H1)
        Text("${course.level} level", style = UcType.BodySmall, color = UC.Muted)
        Spacer(Modifier.height(Space.Sm))
        Text(course.description, style = UcType.Body)
        Spacer(Modifier.height(Space.Md))
        UcProgress(app.courseProgress(code))
        Text("${qs.count { it.id in mastered }} of ${qs.size} questions answered correctly", style = UcType.Caption, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        if (added) TertiaryButton("Remove from my courses") { app.removeCourse(code) }
        else SecondaryButton("Add to my courses", Modifier.fillMaxWidth(), icon = Icons.Outlined.Add) { app.addCourse(code) }

        SectionHeader("Topics")
        ChipFlow { course.topics.forEach { UcChip(it, false) {} } }

        SectionHeader("Study")
        if (qs.isEmpty()) {
            EmptyState(
                Icons.Outlined.Quiz, "No past questions yet",
                "This course has no questions in the prototype set. Community submissions will appear here once the server is connected.",
            )
        } else {
            ListRow("Past questions", "${qs.size} questions", Icons.Outlined.Quiz, { nav.push(Route.PastQuestions(code)) })
            Spacer(Modifier.height(Space.Sm))
            Text("Start a quiz", style = UcType.BodyStrong)
            Spacer(Modifier.height(Space.Sm))
            QuizMode.entries.forEach { m ->
                UcCard(Modifier.padding(bottom = Space.Sm), onClick = {
                    val ids = if (m == QuizMode.Revision) app.missedQuestionIds(code) else qs.map { it.id }
                    if (ids.isEmpty()) error = "You have no missed questions to revise in $code yet."
                    else {
                        error = null
                        val list = ids.mapNotNull { app.content.question(it) }
                        nav.push(Route.Quiz(QuizSession("${course.code} · ${m.label}", code, m, list, if (m == QuizMode.Exam) list.size * 90 else 0)))
                    }
                }) {
                    Text(m.label, style = UcType.BodyStrong)
                    Text(m.blurb, style = UcType.BodySmall, color = UC.Muted)
                }
            }
            error?.let { ErrorState(it) }
        }
        ListRow("Notes for this course", "${app.notes.count { it.courseCode == code }} notes", Icons.Outlined.EditNote, { nav.push(Route.Notes) })
        ListRow("Course community", "Lecture Hall", Icons.Outlined.Groups, { nav.push(Route.Hall(code)) })
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun DiscoverScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var q by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("All levels") }
    val levels = listOf("All levels") + (1..6).map { "${it}00" }
    val courses = app.content.courses().filter {
        (q.isBlank() || it.title.contains(q, true) || it.code.contains(q, true) || it.topics.any { t -> t.contains(q, true) }) &&
            (level == "All levels" || it.level.toString() == level)
    }
    ScreenScaffold("Find courses", { nav.pop() }) {
        UcSearchField(q, { q = it }, "Course code, title or topic")
        Spacer(Modifier.height(Space.Md))
        ChipRow(levels, level) { level = it }
        Spacer(Modifier.height(Space.Md))
        if (courses.isEmpty()) EmptyState(Icons.Outlined.SearchOff, "No courses match", "Try a different search or level.")
        courses.forEach { c ->
            val added = c.code in app.profile.courseCodes
            UcCard(Modifier.padding(bottom = Space.Md), onClick = { nav.push(Route.Course(c.code)) }) {
                Text("${c.code} · ${c.level} level", style = UcType.Caption, color = UC.Muted)
                Text(c.title, style = UcType.H2)
                Text(c.description, style = UcType.BodySmall, color = UC.Muted)
                Spacer(Modifier.height(Space.Sm))
                if (added) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = UC.Success, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Space.Sm)); Text("In my courses", style = UcType.BodySmall, color = UC.Success)
                } else SecondaryButton("Add course", icon = Icons.Outlined.Add) { app.addCourse(c.code) }
            }
        }
    }
}

@Composable
fun SearchScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var input by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(input) {
        delay(250) // debounce
        query = input.trim()
    }
    val repo = app.content
    val ready = query.length >= 2
    val courses = if (!ready) emptyList() else repo.courses().filter { it.title.contains(query, true) || it.code.contains(query, true) || it.topics.any { t -> t.contains(query, true) } }
    val questions = if (!ready) emptyList() else repo.questions().filter { it.text.contains(query, true) || it.topic.contains(query, true) }
    val notes = if (!ready) emptyList() else app.notes.filter { it.title.contains(query, true) || it.body.contains(query, true) }
    val posts = if (!ready) emptyList() else app.posts.filter { !it.hidden && it.text.contains(query, true) }
    val none = courses.isEmpty() && questions.isEmpty() && notes.isEmpty() && posts.isEmpty()

    ScreenScaffold("Search", { nav.pop() }) {
        UcSearchField(input, { input = it }, "Courses, topics, questions, notes, posts")
        Spacer(Modifier.height(Space.Md))
        when {
            input.isBlank() -> {
                if (app.recentSearches.isNotEmpty()) {
                    SectionHeader("Recent", "Clear") { app.clearSearches() }
                    ChipFlow { app.recentSearches.forEach { r -> UcChip(r, false, Icons.Outlined.History) { input = r } } }
                }
                SectionHeader("Try")
                ChipFlow { listOf("Calculus", "Stacks", "Osmosis", "Demand").forEach { UcChip(it, false) { input = it } } }
            }
            query != input.trim() -> { SkeletonBlock(20); Spacer(Modifier.height(Space.Sm)); SkeletonBlock(20); Spacer(Modifier.height(Space.Sm)); SkeletonBlock(20) }
            !ready -> Text("Type at least 2 characters.", style = UcType.Body, color = UC.Muted)
            none -> EmptyState(Icons.Outlined.SearchOff, "Nothing found", "No courses, questions, notes or posts match \"$query\".")
            else -> {
                LaunchedEffect(query) { app.rememberSearch(query) }
                if (courses.isNotEmpty()) {
                    SectionHeader("Courses")
                    courses.forEach { ListRow("${it.code} · ${it.title}", null, Icons.AutoMirrored.Outlined.MenuBook, { nav.push(Route.Course(it.code)) }) }
                }
                if (questions.isNotEmpty()) {
                    SectionHeader("Past questions")
                    questions.forEach { ListRow(it.text, "${it.courseCode} · ${it.topic}", Icons.Outlined.Quiz, { nav.push(Route.QuestionDetail(it.id)) }) }
                }
                if (notes.isNotEmpty()) {
                    SectionHeader("Notes")
                    notes.forEach { ListRow(it.title, it.courseCode.ifBlank { null }, Icons.Outlined.EditNote, { nav.push(Route.NoteEdit(it.id)) }) }
                }
                if (posts.isNotEmpty()) {
                    SectionHeader("Posts")
                    posts.forEach { ListRow(it.text.take(80), it.type.label, Icons.Outlined.Forum, { nav.goTab(Tab.Community) }) }
                }
            }
        }
        Spacer(Modifier.height(Space.Xl))
    }
}
