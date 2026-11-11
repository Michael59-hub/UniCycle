package ng.unicycle.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * App-wide state, persisted on the device with multiplatform-settings (JSON blobs).
 * PROTOTYPE: there is no server yet, so accounts, posts and XP live only on this device.
 */
class AppStore(
    private val store: Settings = Settings(),
    val content: ContentRepository = PrototypeContent,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private inline fun <reified T> load(key: String, default: T): T {
        val raw = store.getString(key, "")
        if (raw.isEmpty()) return default
        return runCatching { json.decodeFromString<T>(raw) }.getOrDefault(default)
    }

    private inline fun <reified T> save(key: String, value: T) {
        store.putString(key, json.encodeToString(value))
    }

    // ---- time helpers -------------------------------------------------------
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /** Days since epoch in the device's local time zone. */
    fun today(): Long {
        val now = Clock.System.now()
        val offset = TimeZone.currentSystemDefault().offsetAt(now).totalSeconds
        return (now.epochSeconds + offset) / 86_400
    }

    private var idCounter = 0
    fun newId(): String = "${nowMillis()}-${idCounter++}"

    // ---- account / profile ----------------------------------------------------
    var account by mutableStateOf(load<Account?>("account", null))
        private set
    var signedIn by mutableStateOf(store.getBoolean("signed_in", false))
        private set
    var onboardingDone by mutableStateOf(store.getBoolean("onboarded", false))
        private set
    var profile by mutableStateOf(load("profile", Profile()))
        private set
    var prefs by mutableStateOf(load("prefs", Prefs()))
        private set
    var pendingInstitution by mutableStateOf(store.getString("pending_institution", ""))
        private set

    val firstName: String
        get() = account?.name?.trim()?.substringBefore(' ')?.ifBlank { null } ?: "Student"

    fun signUp(name: String, identifier: String) {
        account = Account(name.trim(), identifier.trim()); save("account", account)
        signedIn = true; store.putBoolean("signed_in", true)
    }

    /** Prototype sign-in: succeeds only if a local account with this identifier exists. */
    fun signIn(identifier: String): Boolean {
        val a = account ?: return false
        if (!a.identifier.equals(identifier.trim(), ignoreCase = true)) return false
        signedIn = true; store.putBoolean("signed_in", true)
        return true
    }

    fun renameAccount(name: String) {
        val a = account ?: return
        account = a.copy(name = name.trim()); save("account", account)
    }

    fun signOut() { signedIn = false; store.putBoolean("signed_in", false) }

    fun completeOnboarding(p: Profile, institutionRequest: String) {
        profile = p; save("profile", p)
        pendingInstitution = institutionRequest; store.putString("pending_institution", institutionRequest)
        onboardingDone = true; store.putBoolean("onboarded", true)
    }

    fun updateProfile(transform: (Profile) -> Profile) { profile = transform(profile); save("profile", profile) }
    fun updatePrefs(transform: (Prefs) -> Prefs) { prefs = transform(prefs); save("prefs", prefs) }

    fun addCourse(code: String) = updateProfile { if (code in it.courseCodes) it else it.copy(courseCodes = it.courseCodes + code) }
    fun removeCourse(code: String) = updateProfile { it.copy(courseCodes = it.courseCodes - code) }

    val myCourses: List<Course> get() = content.courses().filter { it.code in profile.courseCodes }

    fun deleteAllData() {
        store.clear()
        account = null; signedIn = false; onboardingDone = false; profile = Profile(); prefs = Prefs()
        pendingInstitution = ""
        attempts = emptyList(); ledger = emptyList(); notes = emptyList(); cards = emptyList(); tasks = emptyList()
        posts = emptyList(); reports = emptyList(); savedQuestionIds = emptyList(); recentSearches = emptyList()
        activityDays = emptyList(); inProgress = null; lastCourse = ""
    }

    // ---- learning data --------------------------------------------------------
    var attempts by mutableStateOf(load<List<QuizAttempt>>("attempts", emptyList()))
        private set
    var ledger by mutableStateOf(load<List<XpEntry>>("ledger", emptyList()))
        private set
    var savedQuestionIds by mutableStateOf(load<List<String>>("saved_q", emptyList()))
        private set
    var activityDays by mutableStateOf(load<List<Long>>("activity", emptyList()))
        private set
    var inProgress by mutableStateOf(load<QuizSnapshot?>("in_progress", null))
        private set
    var lastCourse by mutableStateOf(store.getString("last_course", ""))
        private set
    var recentSearches by mutableStateOf(load<List<String>>("recent_searches", emptyList()))
        private set

    val xp: Int get() = ledger.sumOf { it.amount }
    val xpLevel: Int get() = xp / 100 + 1

    private fun markActive() {
        val d = today()
        if (d !in activityDays) { activityDays = activityDays + d; save("activity", activityDays) }
    }

    val streak: Int
        get() {
            val days = activityDays.toSet()
            var d = today()
            if (d !in days) d -= 1
            var n = 0
            while (d in days) { n++; d-- }
            return n
        }

    fun addXp(reason: String, amount: Int, key: String = "") {
        if (key.isNotEmpty() && ledger.any { it.key == key }) return // each reward is granted once
        ledger = ledger + XpEntry(newId(), reason, amount, today(), key); save("ledger", ledger)
    }

    fun saveSnapshot(s: QuizSnapshot?) {
        inProgress = s
        if (s == null) store.remove("in_progress") else save("in_progress", s)
    }

    fun recordAttempt(a: QuizAttempt) {
        attempts = attempts + a; save("attempts", attempts)
        lastCourse = a.courseCode; store.putString("last_course", a.courseCode)
        markActive()
        a.questionIds.forEachIndexed { i, id ->
            val q = content.question(id) ?: return@forEachIndexed
            if (a.answers[i] == q.correctIndex) addXp("Correct answer: ${q.courseCode}", 5, key = "q:$id")
        }
        addXp("Completed: ${a.title}", 10, key = "attempt:${a.id}")
        saveSnapshot(null)
    }

    /** Ids whose most recent outcome was incorrect. */
    fun missedQuestionIds(courseCode: String? = null): List<String> {
        val last = LinkedHashMap<String, Boolean>()
        attempts.forEach { a ->
            a.questionIds.forEachIndexed { i, id ->
                val q = content.question(id) ?: return@forEachIndexed
                if (a.answers.containsKey(i)) last[id] = a.answers[i] == q.correctIndex
            }
        }
        return last.filter { !it.value }.keys.filter { courseCode == null || content.question(it)?.courseCode == courseCode }
    }

    fun masteredIds(): Set<String> {
        val ok = HashSet<String>()
        attempts.forEach { a ->
            a.questionIds.forEachIndexed { i, id ->
                val q = content.question(id)
                if (q != null && a.answers[i] == q.correctIndex) ok += id
            }
        }
        return ok
    }

    fun courseProgress(code: String): Float {
        val qs = content.questions(code)
        if (qs.isEmpty()) return 0f
        val mastered = masteredIds()
        return qs.count { it.id in mastered }.toFloat() / qs.size
    }

    val questionsAttempted: Int get() = attempts.sumOf { it.answers.size }
    val quizAverage: Int
        get() {
            val total = attempts.sumOf { it.total }
            return if (total == 0) 0 else attempts.sumOf { it.correct } * 100 / total
        }

    fun toggleSavedQuestion(id: String) {
        savedQuestionIds = if (id in savedQuestionIds) savedQuestionIds - id else savedQuestionIds + id
        save("saved_q", savedQuestionIds)
    }

    // ---- notes ------------------------------------------------------------------
    var notes by mutableStateOf(load<List<Note>>("notes", emptyList()))
        private set
    fun upsertNote(n: Note) { notes = notes.filter { it.id != n.id } + n; save("notes", notes) }
    fun deleteNote(id: String) { notes = notes.filter { it.id != id }; save("notes", notes) }

    // ---- flashcards (simple spaced repetition) -------------------------------------
    var cards by mutableStateOf(load<List<Flashcard>>("cards", emptyList()))
        private set
    fun addCard(front: String, back: String, courseCode: String) {
        cards = cards + Flashcard(newId(), front.trim(), back.trim(), courseCode, 0, today()); save("cards", cards)
    }
    fun deleteCard(id: String) { cards = cards.filter { it.id != id }; save("cards", cards) }
    fun dueCards(): List<Flashcard> = cards.filter { it.dueDay <= today() }

    fun reviewCard(id: String, known: Boolean) {
        val intervals = listOf(1, 3, 7, 14)
        cards = cards.map {
            if (it.id != id) it else {
                val box = if (known) minOf(it.box + 1, intervals.size) else 0
                it.copy(box = box, dueDay = today() + if (known) intervals[box - 1] else 0)
            }
        }
        save("cards", cards); markActive()
    }

    // ---- planner ----------------------------------------------------------------------
    var tasks by mutableStateOf(load<List<StudyTask>>("tasks", emptyList()))
        private set
    fun addTask(t: StudyTask) { tasks = tasks + t; save("tasks", tasks) }
    fun toggleTask(id: String) { tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it }; save("tasks", tasks); markActive() }
    fun deleteTask(id: String) { tasks = tasks.filter { it.id != id }; save("tasks", tasks) }

    // ---- community (local only) ---------------------------------------------------------
    var posts by mutableStateOf(load<List<Post>>("posts", emptyList()))
        private set
    var reports by mutableStateOf(load<List<Report>>("reports", emptyList()))
        private set

    private fun savePosts() = save("posts", posts)
    fun publishPost(type: PostType, courseCode: String, text: String) {
        posts = listOf(Post(newId(), account?.name ?: "You", type, courseCode, text.trim(), nowMillis())) + posts; savePosts()
    }
    fun updatePost(id: String, transform: (Post) -> Post) { posts = posts.map { if (it.id == id) transform(it) else it }; savePosts() }
    fun deletePost(id: String) { posts = posts.filter { it.id != id }; savePosts() }
    fun addComment(postId: String, text: String) =
        updatePost(postId) { it.copy(comments = it.comments + Comment(newId(), account?.name ?: "You", text.trim(), nowMillis())) }

    fun report(targetType: String, targetId: String, category: String) {
        reports = reports + Report(newId(), targetType, targetId, category, nowMillis()); save("reports", reports)
        if (targetType == "post") updatePost(targetId) { it.copy(hidden = true) }
    }

    // ---- search -----------------------------------------------------------------------------
    fun rememberSearch(q: String) {
        val t = q.trim(); if (t.length < 2) return
        recentSearches = (listOf(t) + recentSearches.filter { !it.equals(t, true) }).take(6); save("recent_searches", recentSearches)
    }
    fun clearSearches() { recentSearches = emptyList(); save("recent_searches", recentSearches) }
}
