package ng.unicycle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ng.unicycle.LocalApp
import ng.unicycle.PlatformBackHandler
import ng.unicycle.data.Profile

/** Placeholder UniCycle mark (two overlapping rounded shapes). Replace with the final logo asset. */
@Composable
fun UniCycleMark(markSize: Dp = 56.dp) {
    val primary = UC.Primary
    val deep = UC.Selected
    Canvas(Modifier.size(markSize)) {
        val w = size.width
        drawRoundRect(deep, Offset(0f, w * 0.1f), Size(w * 0.62f, w * 0.62f), CornerRadius(w * 0.31f))
        drawRoundRect(primary, Offset(w * 0.38f, w * 0.28f), Size(w * 0.62f, w * 0.62f), CornerRadius(w * 0.31f))
    }
}

@Composable
fun SplashScreen() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        UniCycleMark(72.dp)
        Spacer(Modifier.height(Space.Lg))
        Text("UniCycle", style = UcType.Display)
        Text(
            "Your campus. Your community. Your competition.", style = UcType.BodySmall, color = UC.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.Sm, start = Space.Xl, end = Space.Xl),
        )
    }
}

private enum class AuthStep { Welcome, SignIn, SignUp, Verify, Recover }

@Composable
fun AuthFlow() {
    val app = LocalApp.current
    var step by remember { mutableStateOf(AuthStep.Welcome) }
    var pendingName by remember { mutableStateOf("") }
    var pendingId by remember { mutableStateOf("") }

    PlatformBackHandler(enabled = step != AuthStep.Welcome) {
        step = if (step == AuthStep.Verify) AuthStep.SignUp else AuthStep.Welcome
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Page)) {
        when (step) {
            AuthStep.Welcome -> {
                Spacer(Modifier.height(Space.Xxl))
                UniCycleMark(56.dp)
                Spacer(Modifier.height(Space.Lg))
                Text("UniCycle", style = UcType.Display)
                Text("Your campus. Your community. Your competition.", style = UcType.Body, color = UC.Muted)
                Spacer(Modifier.height(Space.Xl))
                ValueRow(Icons.Outlined.School, "Learn", "Past questions, quizzes and explanations in plain language.")
                ValueRow(Icons.Outlined.Groups, "Connect", "Course communities and Lecture Halls with your coursemates.")
                ValueRow(Icons.Outlined.EmojiEvents, "Compete", "Fair, effort-based challenges across universities.")
                ValueRow(Icons.Outlined.CloudOff, "Study offline", "Built for patchy networks and small data budgets.")
                Spacer(Modifier.height(Space.Xl))
                PrimaryButton("Create account", Modifier.fillMaxWidth()) { step = AuthStep.SignUp }
                Spacer(Modifier.height(Space.Sm))
                SecondaryButton("Sign in", Modifier.fillMaxWidth()) { step = AuthStep.SignIn }
                Spacer(Modifier.height(Space.Lg))
                PrototypeBanner("Prototype build: accounts are stored on this device only until the server is connected.")
            }
            AuthStep.SignUp -> {
                var name by remember { mutableStateOf("") }
                var id by remember { mutableStateOf("") }
                var pw by remember { mutableStateOf("") }
                var tried by remember { mutableStateOf(false) }
                UcTopBar("Create your account", onBack = { step = AuthStep.Welcome })
                Text("Use your email or phone number. A student email is not required.", style = UcType.Body, color = UC.Muted)
                Spacer(Modifier.height(Space.Lg))
                UcTextField(name, { name = it }, "Full name", error = if (tried && name.isBlank()) "Enter your name" else null)
                Spacer(Modifier.height(Space.Md))
                UcTextField(
                    id, { id = it }, "Email or phone number", keyboard = KeyboardType.Email,
                    error = if (tried && id.isBlank()) "Enter your email or phone number" else null,
                )
                Spacer(Modifier.height(Space.Md))
                UcTextField(
                    pw, { pw = it }, "Password", password = true, supporting = "At least 8 characters",
                    error = if (tried && pw.length < 8) "Use at least 8 characters" else null,
                )
                Spacer(Modifier.height(Space.Xl))
                PrimaryButton("Continue", Modifier.fillMaxWidth()) {
                    tried = true
                    if (name.isNotBlank() && id.isNotBlank() && pw.length >= 8) { pendingName = name; pendingId = id; step = AuthStep.Verify }
                }
                Spacer(Modifier.height(Space.Md))
                PrototypeBanner("The password is checked for length only. It is not stored or sent anywhere in this build.")
            }
            AuthStep.Verify -> {
                UcTopBar("Verify your account", onBack = { step = AuthStep.SignUp })
                UcCard {
                    Icon(Icons.Outlined.Email, null, tint = UC.Primary)
                    Spacer(Modifier.height(Space.Sm))
                    Text("Verification isn't connected yet", style = UcType.H2)
                    Spacer(Modifier.height(Space.Xs))
                    Text(
                        "Real builds send a code to $pendingId. This prototype has no server to send it, so you can continue without verifying.",
                        style = UcType.Body, color = UC.Muted,
                    )
                }
                Spacer(Modifier.height(Space.Xl))
                PrimaryButton("Continue without verifying", Modifier.fillMaxWidth()) { app.signUp(pendingName, pendingId) }
            }
            AuthStep.SignIn -> {
                var id by remember { mutableStateOf("") }
                var pw by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                UcTopBar("Welcome back", onBack = { step = AuthStep.Welcome })
                UcTextField(id, { id = it; error = null }, "Email or phone number", keyboard = KeyboardType.Email)
                Spacer(Modifier.height(Space.Md))
                UcTextField(pw, { pw = it; error = null }, "Password", password = true, error = error)
                TertiaryButton("Forgot password?", Modifier.align(Alignment.End)) { step = AuthStep.Recover }
                Spacer(Modifier.height(Space.Md))
                PrimaryButton("Sign in", Modifier.fillMaxWidth()) {
                    error = when {
                        id.isBlank() -> "Enter your email or phone number"
                        pw.isEmpty() -> "Enter your password"
                        !app.signIn(id) -> "No account for that email or phone exists on this device. Create one first."
                        else -> null
                    }
                }
                Spacer(Modifier.height(Space.Md))
                PrototypeBanner("Prototype sign-in: passwords are not stored or verified until the server is connected.")
            }
            AuthStep.Recover -> {
                var id by remember { mutableStateOf("") }
                var sent by remember { mutableStateOf(false) }
                UcTopBar("Reset password", onBack = { step = AuthStep.SignIn })
                UcTextField(id, { id = it; sent = false }, "Email or phone number")
                Spacer(Modifier.height(Space.Lg))
                PrimaryButton("Request reset", Modifier.fillMaxWidth(), enabled = id.isNotBlank()) { sent = true }
                if (sent) {
                    Spacer(Modifier.height(Space.Lg))
                    ErrorState("Password reset needs the server, which isn't connected in this prototype. No message was sent.")
                }
            }
        }
    }
}

@Composable
private fun ValueRow(icon: ImageVector, title: String, body: String) {
    Row(Modifier.padding(vertical = Space.Sm), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = UC.Primary, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(Space.Md))
        Column { Text(title, style = UcType.BodyStrong); Text(body, style = UcType.BodySmall, color = UC.Muted) }
    }
}

private val StepTitles = listOf(
    "Your institution", "Faculty and department", "Programme and level", "Your courses",
    "Interests", "Accessibility", "Privacy and notifications", "You're all set",
)
private val InterestOptions = listOf(
    "Mathematics and logic", "Programming", "Sciences", "Business and economics", "Languages",
    "Health", "Law and politics", "Arts and culture", "Career and aptitude", "Study skills",
)

@Composable
fun OnboardingFlow() {
    val app = LocalApp.current
    val repo = app.content
    var step by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var instId by remember { mutableStateOf("") }
    var instName by remember { mutableStateOf("") }
    var customInst by remember { mutableStateOf("") }
    var facultyId by remember { mutableStateOf("") }
    var deptId by remember { mutableStateOf("") }
    var programme by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(100) }
    var chosenCourses by remember { mutableStateOf(setOf<String>()) }
    var interests by remember { mutableStateOf(setOf<String>()) }

    val last = StepTitles.lastIndex
    val faculty = repo.faculties().firstOrNull { it.id == facultyId }
    val dept = repo.departments(facultyId).firstOrNull { it.id == deptId }

    val canContinue = when (step) {
        0 -> instId.isNotBlank() || customInst.isNotBlank()
        1 -> facultyId.isNotBlank() && deptId.isNotBlank()
        2 -> programme.isNotBlank()
        else -> true
    }

    PlatformBackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.Page)) {
            Spacer(Modifier.height(Space.Lg))
            Text("Step ${step + 1} of ${StepTitles.size}", style = UcType.Caption, color = UC.Muted)
            Spacer(Modifier.height(Space.Xs))
            UcProgress((step + 1) / StepTitles.size.toFloat())
            Spacer(Modifier.height(Space.Lg))
            Text(StepTitles[step], style = UcType.Display)
            Spacer(Modifier.height(Space.Md))

            when (step) {
                0 -> {
                    Text("Choose where you study. This helps us show the right courses and communities.", style = UcType.Body, color = UC.Muted)
                    Spacer(Modifier.height(Space.Md))
                    UcSearchField(query, { query = it }, "Search institutions")
                    Spacer(Modifier.height(Space.Sm))
                    repo.institutions().filter { query.isBlank() || it.name.contains(query, true) }.forEach { i ->
                        val sel = instId == i.id
                        ListRow(i.name, i.type.label, onClick = { instId = i.id; instName = i.name; customInst = "" }) {
                            if (sel) Icon(Icons.Outlined.CheckCircle, "Selected", tint = UC.Primary)
                        }
                    }
                    Spacer(Modifier.height(Space.Lg))
                    Text("Not listed?", style = UcType.BodyStrong)
                    Spacer(Modifier.height(Space.Sm))
                    UcTextField(
                        customInst, { customInst = it; if (it.isNotBlank()) { instId = ""; instName = "" } },
                        "Your institution's name", supporting = "We'll queue it for review and add it to the directory.",
                    )
                }
                1 -> {
                    DropdownField(
                        "Faculty", faculty?.name.orEmpty(), repo.faculties().map { it.name },
                        { n -> facultyId = repo.faculties().first { it.name == n }.id; deptId = ""; programme = "" },
                    )
                    Spacer(Modifier.height(Space.Md))
                    if (facultyId.isNotBlank()) {
                        DropdownField(
                            "Department", dept?.name.orEmpty(), repo.departments(facultyId).map { it.name },
                            { n -> deptId = repo.departments(facultyId).first { it.name == n }.id; programme = "" },
                        )
                    }
                }
                2 -> {
                    DropdownField("Programme", programme, repo.programmes(deptId).map { it.name }, { programme = it })
                    Spacer(Modifier.height(Space.Lg))
                    Text("Level", style = UcType.BodyStrong)
                    Spacer(Modifier.height(Space.Sm))
                    ChipFlow { (1..6).forEach { l -> UcChip("${l}00 level", level == l * 100) { level = l * 100 } } }
                }
                3 -> {
                    val all = repo.courses()
                    val suggested = all.filter { (it.departmentIds.isEmpty() || deptId in it.departmentIds) && it.level <= level }
                    Text("Pick the courses you're taking. You can change this any time.", style = UcType.Body, color = UC.Muted)
                    Spacer(Modifier.height(Space.Sm))
                    if (suggested.isEmpty()) Text("No suggested courses for your selection yet.", style = UcType.Body, color = UC.Muted)
                    suggested.forEach { c ->
                        CourseCheckRow(c.code, c.title, c.code in chosenCourses) {
                            chosenCourses = if (c.code in chosenCourses) chosenCourses - c.code else chosenCourses + c.code
                        }
                    }
                    val others = all.filter { it !in suggested }
                    if (others.isNotEmpty()) {
                        SectionHeader("Other courses")
                        others.forEach { c ->
                            CourseCheckRow(c.code, c.title, c.code in chosenCourses) {
                                chosenCourses = if (c.code in chosenCourses) chosenCourses - c.code else chosenCourses + c.code
                            }
                        }
                    }
                }
                4 -> {
                    Text("Optional. We'll use these to suggest communities and content.", style = UcType.Body, color = UC.Muted)
                    Spacer(Modifier.height(Space.Md))
                    ChipFlow { InterestOptions.forEach { i -> UcChip(i, i in interests) { interests = if (i in interests) interests - i else interests + i } } }
                }
                5 -> {
                    Text("Adjust how UniCycle looks and reads. This is optional, and you never have to explain why.", style = UcType.Body, color = UC.Muted)
                    Spacer(Modifier.height(Space.Md))
                    AccessibilityControls()
                }
                6 -> {
                    Text("You choose what others can see. We only ask for device permissions when a feature needs them.", style = UcType.Body, color = UC.Muted)
                    PrivacyControls()
                    SectionHeader("Notifications")
                    NotificationControls()
                }
                else -> {
                    UcCard {
                        Icon(Icons.Outlined.CheckCircle, null, tint = UC.Success)
                        Spacer(Modifier.height(Space.Sm))
                        Text(instName.ifBlank { customInst }, style = UcType.H2)
                        Text("${faculty?.name.orEmpty()} · ${dept?.name.orEmpty()}", style = UcType.Body, color = UC.Muted)
                        Text("$programme · $level level", style = UcType.Body, color = UC.Muted)
                        Text("${chosenCourses.size} courses · ${interests.size} interests", style = UcType.Body, color = UC.Muted)
                    }
                    if (customInst.isNotBlank()) {
                        Spacer(Modifier.height(Space.Md))
                        PrototypeBanner("\"$customInst\" is saved as a pending request. It will be sent for review once the server is connected.")
                    }
                }
            }
            Spacer(Modifier.height(Space.Xl))
        }
        Row(Modifier.fillMaxWidth().padding(Space.Page), horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
            if (step > 0) SecondaryButton("Back", Modifier.weight(1f)) { step-- }
            PrimaryButton(if (step == last) "Go to Home" else "Continue", Modifier.weight(1f), enabled = canContinue) {
                if (step == last) {
                    app.completeOnboarding(
                        Profile(
                            institutionId = instId, institutionName = instName.ifBlank { customInst },
                            facultyId = facultyId, facultyName = faculty?.name.orEmpty(),
                            departmentId = deptId, departmentName = dept?.name.orEmpty(),
                            programme = programme, level = level,
                            courseCodes = chosenCourses.toList(), interests = interests.toList(),
                        ),
                        customInst,
                    )
                } else step++
            }
        }
    }
}

@Composable
private fun CourseCheckRow(code: String, title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Checkbox, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked, onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = UC.Primary, checkmarkColor = UC.OnPrimary, uncheckedColor = UC.Muted),
        )
        Spacer(Modifier.width(Space.Md))
        Column { Text(code, style = UcType.BodyStrong); Text(title, style = UcType.BodySmall, color = UC.Muted) }
    }
}
