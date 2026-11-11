package ng.unicycle

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import ng.unicycle.data.*
import ng.unicycle.ui.*

val LocalApp = staticCompositionLocalOf<AppStore> { error("AppStore not provided") }
val LocalSpeaker = staticCompositionLocalOf<Speaker> { error("Speaker not provided") }
val LocalAi = staticCompositionLocalOf<AiService> { error("AiService not provided") }
val LocalNav = staticCompositionLocalOf<Nav> { error("Nav not provided") }

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Learn("Learn", Icons.AutoMirrored.Outlined.MenuBook),
    Compete("Compete", Icons.Outlined.EmojiEvents),
    Community("Community", Icons.Outlined.Groups),
    Profile("Profile", Icons.Outlined.Person),
}

sealed interface Route {
    data class Course(val code: String) : Route
    data object Search : Route
    data object Discover : Route
    data class PastQuestions(val courseCode: String?) : Route
    data class QuestionDetail(val id: String) : Route
    data class Quiz(val session: QuizSession) : Route
    data class Results(val attemptId: String) : Route
    data object Flashcards : Route
    data object FlashcardReview : Route
    data object Notes : Route
    data class NoteEdit(val id: String?, val courseCode: String = "") : Route
    data object Planner : Route
    data object Saved : Route
    data object AiHub : Route
    data class AiToolScreen(val tool: AiTool) : Route
    data object OfflineLibrary : Route
    data class Hall(val code: String) : Route
    data object XpLedger : Route
    data object RankingRules : Route
    data object EditProfile : Route
    data object PrivacySettings : Route
    data object AccessibilitySettings : Route
    data object NotificationSettings : Route
    data object AppearanceSettings : Route
    data object DataSettings : Route
    data object AccountSettings : Route
}

/** Simple back stack: each tab has a root screen; secondary screens stack on top of it. */
class Nav {
    var tab by mutableStateOf(Tab.Home)
    val stack = mutableStateListOf<Route>()
    val top: Route? get() = stack.lastOrNull()

    fun push(route: Route) { stack.add(route) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replaceTop(route: Route) { if (stack.isNotEmpty()) stack[stack.lastIndex] = route else stack.add(route) }
    fun goTab(t: Tab) { stack.clear(); tab = t }
}

@Composable
fun App() {
    val app = remember { AppStore() }
    val speaker = remember { createSpeaker() }
    val ai = remember<AiService> { UnconfiguredAiService() }
    val dark = when (app.prefs.theme) {
        ThemeMode.Midnight -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    UniCycleTheme(dark, app.prefs.highContrast, app.prefs.textScale, app.prefs.reduceMotion) {
        SystemBarsEffect(dark)
        CompositionLocalProvider(LocalApp provides app, LocalSpeaker provides speaker, LocalAi provides ai) {
            Surface(Modifier.fillMaxSize(), color = UC.Bg, contentColor = UC.Text) {
                Box(Modifier.safeDrawingPadding()) { Root(app) }
            }
        }
    }
}

@Composable
private fun Root(app: AppStore) {
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(900); splash = false }
    when {
        splash -> SplashScreen()
        !app.signedIn -> AuthFlow()
        !app.onboardingDone -> OnboardingFlow()
        else -> MainShell()
    }
}

@Composable
private fun MainShell() {
    val nav = remember { Nav() }
    val speaker = LocalSpeaker.current
    val inQuiz = nav.top is Route.Quiz

    PlatformBackHandler(enabled = nav.stack.isNotEmpty() || nav.tab != Tab.Home) {
        speaker.stop()
        if (nav.stack.isNotEmpty()) nav.pop() else nav.tab = Tab.Home
    }

    CompositionLocalProvider(LocalNav provides nav) {
        Scaffold(
            containerColor = UC.Bg,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!inQuiz) {
                    NavigationBar(containerColor = UC.Surface) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = nav.tab == t,
                                onClick = { speaker.stop(); nav.goTab(t) },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label, style = UcType.Caption) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = UC.OnSelected, selectedTextColor = UC.Primary,
                                    indicatorColor = UC.Selected,
                                    unselectedIconColor = UC.Muted, unselectedTextColor = UC.Muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                val route = nav.top
                if (route == null) TabRoot(nav.tab) else RouteHost(route)
            }
        }
    }
}

@Composable
private fun TabRoot(tab: Tab) {
    when (tab) {
        Tab.Home -> HomeScreen()
        Tab.Learn -> LearnScreen()
        Tab.Compete -> CompeteScreen()
        Tab.Community -> CommunityScreen()
        Tab.Profile -> ProfileScreen()
    }
}

@Composable
private fun RouteHost(route: Route) {
    when (route) {
        is Route.Course -> CourseDetailScreen(route.code)
        Route.Search -> SearchScreen()
        Route.Discover -> DiscoverScreen()
        is Route.PastQuestions -> PastQuestionsScreen(route.courseCode)
        is Route.QuestionDetail -> QuestionDetailScreen(route.id)
        is Route.Quiz -> QuizScreen(route.session)
        is Route.Results -> ResultsScreen(route.attemptId)
        Route.Flashcards -> FlashcardsScreen()
        Route.FlashcardReview -> FlashcardReviewScreen()
        Route.Notes -> NotesScreen()
        is Route.NoteEdit -> NoteEditScreen(route.id, route.courseCode)
        Route.Planner -> PlannerScreen()
        Route.Saved -> SavedScreen()
        Route.AiHub -> AiHubScreen()
        is Route.AiToolScreen -> AiToolScreen(route.tool)
        Route.OfflineLibrary -> OfflineLibraryScreen()
        is Route.Hall -> HallScreen(route.code)
        Route.XpLedger -> XpLedgerScreen()
        Route.RankingRules -> RankingRulesScreen()
        Route.EditProfile -> EditProfileScreen()
        Route.PrivacySettings -> PrivacyScreen()
        Route.AccessibilitySettings -> AccessibilityScreen()
        Route.NotificationSettings -> NotificationScreen()
        Route.AppearanceSettings -> AppearanceScreen()
        Route.DataSettings -> DataScreen()
        Route.AccountSettings -> AccountScreen()
    }
}
