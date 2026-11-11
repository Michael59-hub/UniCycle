package ng.unicycle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ng.unicycle.*
import ng.unicycle.data.*

data class Achievement(val title: String, val description: String, val earned: Boolean)

fun achievementsFor(app: AppStore): List<Achievement> = listOf(
    Achievement("First quiz", "Finish any quiz", app.attempts.isNotEmpty()),
    Achievement("Perfect score", "Get every question right in a quiz of 3 or more", app.attempts.any { it.total >= 3 && it.correct == it.total }),
    Achievement("10 correct", "Answer 10 questions correctly", app.attempts.sumOf { it.correct } >= 10),
    Achievement("3-day streak", "Study three days in a row", app.streak >= 3),
    Achievement("Note taker", "Write your first note", app.notes.isNotEmpty()),
    Achievement("Course builder", "Add three courses", app.profile.courseCodes.size >= 3),
)

@Composable
fun CompeteScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val achievements = achievementsFor(app)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.Page)) {
        PageTitle("Compete", "Earn XP by practising. Rankings arrive with the server.")
        PrototypeBanner("No rankings or tournaments are shown because none are real yet. Fair competition needs server-controlled timers and scoring.")
        Spacer(Modifier.height(Space.Md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
            StatTile("XP", "${app.xp}", Icons.Outlined.Bolt, Modifier.weight(1f), tint = UC.Amber)
            StatTile("Level", "${app.xpLevel}", Icons.Outlined.Star, Modifier.weight(1f), tint = UC.Amber)
            StatTile("Streak", "${app.streak}", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f), tint = UC.Amber)
        }
        TertiaryButton("See XP history") { nav.push(Route.XpLedger) }

        SectionHeader("Reasoning practice")
        UcCard(onClick = {
            val qs = app.content.questions(PrototypeContent.APT_NUM)
            nav.push(Route.Quiz(QuizSession("Numerical reasoning", PrototypeContent.APT_NUM, QuizMode.Practice, qs, 0)))
        }) {
            Text("Numerical reasoning", style = UcType.H2)
            Text("Short practice set. You get a reasoning score, not an IQ score.", style = UcType.BodySmall, color = UC.Muted)
        }

        SectionHeader("Achievements")
        achievements.forEach { a ->
            ListRow(a.title, a.description, if (a.earned) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock) {
                Text(if (a.earned) "Earned" else "Locked", style = UcType.Caption, color = if (a.earned) UC.Success else UC.Muted)
            }
        }

        SectionHeader("Leaderboards")
        EmptyState(
            Icons.Outlined.Leaderboard, "Rankings aren't live yet",
            "National, university, faculty, department and course leaderboards will appear here once scores can be verified on the server.",
            "How rankings will work",
        ) { nav.push(Route.RankingRules) }

        SectionHeader("Tournaments")
        EmptyState(Icons.Outlined.EmojiEvents, "No tournaments yet", "Registration, brackets and results will be shown here when tournaments are published.")
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun XpLedgerScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    ScreenScaffold("XP history", { nav.pop() }) {
        Text("Every XP change is recorded. Each correct question earns XP once, so repeating easy quizzes doesn't inflate your total.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        if (app.ledger.isEmpty()) EmptyState(Icons.Outlined.Bolt, "No XP yet", "Finish a quiz to earn your first XP.")
        app.ledger.reversed().forEach { e ->
            ListRow(e.reason, null, Icons.Outlined.Bolt) { Text("+${e.amount}", style = UcType.BodyStrong, color = UC.Amber) }
        }
    }
}

@Composable
fun RankingRulesScreen() {
    val nav = LocalNav.current
    ScreenScaffold("How rankings will work", { nav.pop() }) {
        Text("Competition should reward effort and improvement, not size or repeated low-effort attempts.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        listOf(
            "No raw totals" to "Universities are not ranked by total XP, which would favour larger institutions.",
            "Participation-adjusted" to "Institution scores account for the number of eligible participants.",
            "Accuracy and difficulty" to "Harder questions count for more, and accuracy matters alongside speed.",
            "Improvement" to "Progress over time is rewarded, so every student can climb.",
            "Server-scored" to "Timers and scores are calculated on the server and can't be changed from a device.",
            "Anti-cheating" to "Randomised questions and options, rate limits, duplicate-account checks and audit logs, with human review for disputes.",
            "Your choice" to "You can opt out of leaderboards in Privacy and safety.",
        ).forEach { (t, b) -> UcCard(Modifier.padding(bottom = Space.Md)) { Text(t, style = UcType.H2); Text(b, style = UcType.Body, color = UC.Muted) } }
        PrototypeBanner("This describes the planned scoring rules. Nothing is being ranked in this build.")
    }
}
