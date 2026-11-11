package ng.unicycle.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue

/** Live state of one quiz attempt. Autosaved through [toSnapshot]. */
class QuizSession(
    val title: String,
    val courseCode: String,
    val mode: QuizMode,
    val questions: List<PastQuestion>,
    val totalSeconds: Int,
    snapshot: QuizSnapshot? = null,
) {
    var current by mutableIntStateOf(snapshot?.current ?: 0)
    var secondsLeft by mutableIntStateOf(snapshot?.secondsLeft ?: totalSeconds)
    val answers = mutableStateMapOf<Int, Int>().apply { snapshot?.answers?.let { putAll(it) } }
    val checked = mutableStateListOf<Int>().apply { snapshot?.checked?.let { addAll(it) } }
    val flagged = mutableStateListOf<Int>().apply { snapshot?.flagged?.let { addAll(it) } }

    val total: Int get() = questions.size
    val timed: Boolean get() = mode == QuizMode.Exam
    val answeredCount: Int get() = answers.size
    val correctCount: Int get() = questions.indices.count { answers[it] == questions[it].correctIndex }
    fun isLocked(i: Int): Boolean = mode != QuizMode.Exam && i in checked
    fun showsFeedback(i: Int): Boolean = mode != QuizMode.Exam && i in checked

    fun select(option: Int) { if (!isLocked(current)) answers[current] = option }
    fun check() { if (answers.containsKey(current) && current !in checked) checked.add(current) }
    fun toggleFlag() { if (current in flagged) flagged.remove(current) else flagged.add(current) }
    fun goTo(i: Int) { if (i in 0 until total) current = i }

    fun toSnapshot() = QuizSnapshot(
        title, courseCode, mode, questions.map { it.id }, answers.toMap(), checked.toList(),
        flagged.toList(), current, secondsLeft, totalSeconds,
    )

    fun toAttempt(id: String, day: Long): QuizAttempt {
        val topics = questions.indices.groupBy { questions[it].topic }.map { (topic, idx) ->
            TopicResult(topic, idx.count { answers[it] == questions[it].correctIndex }, idx.size)
        }
        return QuizAttempt(
            id, title, courseCode, mode, day, questions.map { it.id }, answers.toMap(),
            correctCount, total, totalSeconds - secondsLeft, topics,
        )
    }
}

enum class AiTool(val label: String, val blurb: String, val hint: String) {
    Tutor("AI Tutor", "Ask for a concept to be explained.", "Ask about a concept or paste a question"),
    Rewrite("Question rewriter", "Simplify or rephrase a question while keeping the concept.", "Paste the question to rewrite"),
    Generate("Question generator", "Draft practice questions for a topic.", "Course, topic and how many questions"),
    Summarize("Summaries", "Summarise your own notes.", "Paste the notes to summarise"),
    Flashcards("Flashcard generator", "Turn notes into flashcards.", "Paste the notes to convert"),
}

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String, val retryable: Boolean) : AiResult
}

/** Provider-agnostic AI interface. Implement against your server; never ship API keys in the app. */
interface AiService {
    val connected: Boolean
    suspend fun run(tool: AiTool, level: ExplainLevel, prompt: String): AiResult
}

class UnconfiguredAiService : AiService {
    override val connected = false
    override suspend fun run(tool: AiTool, level: ExplainLevel, prompt: String): AiResult {
        kotlinx.coroutines.delay(500)
        return AiResult.Failure(
            "The AI service isn't connected in this build, so no answer was generated. " +
                "Stored explanations on questions still work offline.",
            retryable = false,
        )
    }
}
