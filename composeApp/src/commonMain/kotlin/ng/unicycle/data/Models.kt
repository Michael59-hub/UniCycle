package ng.unicycle.data

import kotlinx.serialization.Serializable

enum class InstitutionType(val label: String) {
    Federal("Federal university"),
    State("State university"),
    Private("Private university"),
    Polytechnic("Polytechnic"),
    CollegeOfEducation("College of education"),
    Other("Other"),
}

data class Institution(val id: String, val name: String, val type: InstitutionType)
data class Faculty(val id: String, val name: String)
data class Department(val id: String, val facultyId: String, val name: String)
data class Programme(val id: String, val departmentId: String, val name: String)

data class Course(
    val code: String,
    val title: String,
    val description: String,
    val level: Int,
    /** Empty = applies to every department. */
    val departmentIds: Set<String>,
    val topics: List<String>,
)

enum class Provenance(val label: String) {
    Verified("Verified"),
    Community("Community submitted"),
    Ai("AI generated"),
    UnderReview("Under review"),
}

enum class Difficulty(val label: String) { Easy("Easy"), Medium("Medium"), Hard("Hard") }

data class PastQuestion(
    val id: String,
    val courseCode: String,
    val topic: String,
    val year: Int?,
    val source: String,
    val provenance: Provenance,
    val difficulty: Difficulty,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val eli10: String,
    val simple: String,
    val university: String,
    val whyOthersWrong: String,
)

enum class ExplainLevel(val label: String) {
    Eli10("Explain like I'm 10"),
    Simple("Simple"),
    University("University level"),
    Detailed("Detailed (AI)"),
}

interface ContentRepository {
    /** True while content is the bundled prototype set rather than server data. */
    val isPrototype: Boolean
    fun institutions(): List<Institution>
    fun faculties(): List<Faculty>
    fun departments(facultyId: String): List<Department>
    fun programmes(departmentId: String): List<Programme>
    fun courses(): List<Course>
    fun questions(courseCode: String? = null): List<PastQuestion>
    fun question(id: String): PastQuestion?
}

@Serializable
enum class QuizMode(val label: String, val blurb: String) {
    Practice("Practice", "Check each answer and read the explanation straight away."),
    Exam("Mock exam", "Timed. Explanations are shown after you submit."),
    Revision("Revision", "Only questions you last answered incorrectly."),
}

@Serializable
enum class ThemeMode(val label: String) { Midnight("Midnight"), Light("Light"), System("System") }

@Serializable
data class Account(val name: String, val identifier: String)

@Serializable
data class Profile(
    val bio: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val departmentId: String = "",
    val departmentName: String = "",
    val programme: String = "",
    val level: Int = 100,
    val courseCodes: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
)

@Serializable
data class Prefs(
    val theme: ThemeMode = ThemeMode.System,
    val textScale: Float = 1f,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val speechRate: Float = 1f,
    val autoRead: Boolean = false,
    val dataSaver: Boolean = true,
    val wifiOnly: Boolean = true,
    val showInstitution: Boolean = true,
    val showActivity: Boolean = true,
    val joinLeaderboards: Boolean = true,
    val notifyReminders: Boolean = true,
    val notifyReplies: Boolean = true,
    val notifyCompetitions: Boolean = false,
)

@Serializable
data class TopicResult(val topic: String, val correct: Int, val total: Int)

@Serializable
data class QuizAttempt(
    val id: String,
    val title: String,
    val courseCode: String,
    val mode: QuizMode,
    val day: Long,
    val questionIds: List<String>,
    val answers: Map<Int, Int>,
    val correct: Int,
    val total: Int,
    val secondsUsed: Int,
    val topics: List<TopicResult>,
)

@Serializable
data class QuizSnapshot(
    val title: String,
    val courseCode: String,
    val mode: QuizMode,
    val questionIds: List<String>,
    val answers: Map<Int, Int>,
    val checked: List<Int>,
    val flagged: List<Int>,
    val current: Int,
    val secondsLeft: Int,
    val totalSeconds: Int,
)

@Serializable
data class XpEntry(val id: String, val reason: String, val amount: Int, val day: Long, val key: String = "")

@Serializable
data class Note(val id: String, val title: String, val body: String, val courseCode: String, val day: Long)

@Serializable
data class Flashcard(
    val id: String,
    val front: String,
    val back: String,
    val courseCode: String,
    val box: Int = 0,
    val dueDay: Long = 0,
)

@Serializable
data class StudyTask(
    val id: String,
    val title: String,
    val courseCode: String,
    val day: Long,
    val minutes: Int,
    val done: Boolean = false,
)

@Serializable
enum class PostType(val label: String) {
    Text("Discussion"), Question("Question"), Tip("Study tip"), Resource("Resource link"),
}

@Serializable
data class Comment(val id: String, val author: String, val text: String, val millis: Long)

@Serializable
data class Post(
    val id: String,
    val author: String,
    val type: PostType,
    val courseCode: String,
    val text: String,
    val millis: Long,
    val liked: Boolean = false,
    val saved: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val hidden: Boolean = false,
)

@Serializable
data class Report(val id: String, val targetType: String, val targetId: String, val category: String, val millis: Long)
