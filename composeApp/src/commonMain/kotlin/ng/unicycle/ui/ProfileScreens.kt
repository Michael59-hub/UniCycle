package ng.unicycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.unicycle.LocalApp
import ng.unicycle.LocalNav
import ng.unicycle.LocalSpeaker
import ng.unicycle.Route
import ng.unicycle.data.ThemeMode

@Composable
fun ScreenScaffold(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        UcTopBar(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.Page), content = content)
    }
}

@Composable
fun ProfileScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val p = app.profile
    val achievements = achievementsFor(app)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(96.dp).background(UC.Selected))
        Column(Modifier.padding(horizontal = Space.Page)) {
            Box(Modifier.offset(y = (-24).dp)) { Avatar(app.firstName, 72) }
            Column(Modifier.offset(y = (-16).dp)) {
                Text(app.account?.name ?: "Student", style = UcType.H1)
                if (app.prefs.showInstitution) {
                    Text(p.institutionName, style = UcType.Body, color = UC.Muted)
                    Text("${p.departmentName} · ${p.level} level", style = UcType.BodySmall, color = UC.Muted)
                }
                if (p.bio.isNotBlank()) { Spacer(Modifier.height(Space.Sm)); Text(p.bio, style = UcType.Body) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
                StatTile("XP", "${app.xp}", Icons.Outlined.Bolt, Modifier.weight(1f), tint = UC.Amber)
                StatTile("Day streak", "${app.streak}", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f), tint = UC.Amber)
            }
            Spacer(Modifier.height(Space.Md))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
                StatTile("Quiz average", "${app.quizAverage}%", Icons.Outlined.Insights, Modifier.weight(1f))
                StatTile("Questions attempted", "${app.questionsAttempted}", Icons.Outlined.Quiz, Modifier.weight(1f))
            }

            SectionHeader("Achievements")
            val earned = achievements.filter { it.earned }
            if (earned.isEmpty()) Text("Finish a quiz or add a note to earn your first badge.", style = UcType.Body, color = UC.Muted)
            else ChipFlow { earned.forEach { UcChip(it.title, true, Icons.Outlined.EmojiEvents) {} } }

            SectionHeader("Settings")
            ListRow("Edit profile", "Name, bio and level", Icons.Outlined.Edit, { nav.push(Route.EditProfile) }) { Chevron() }
            ListRow("Appearance", app.prefs.theme.label, Icons.Outlined.Palette, { nav.push(Route.AppearanceSettings) }) { Chevron() }
            ListRow("Accessibility", "Text size, contrast, voice", Icons.Outlined.Accessibility, { nav.push(Route.AccessibilitySettings) }) { Chevron() }
            ListRow("Privacy and safety", "Who can see your activity", Icons.Outlined.Lock, { nav.push(Route.PrivacySettings) }) { Chevron() }
            ListRow("Notifications", null, Icons.Outlined.Notifications, { nav.push(Route.NotificationSettings) }) { Chevron() }
            ListRow("Data and offline", "Data saver and downloads", Icons.Outlined.CloudDownload, { nav.push(Route.DataSettings) }) { Chevron() }
            ListRow("Account and security", app.account?.identifier, Icons.Outlined.ManageAccounts, { nav.push(Route.AccountSettings) }) { Chevron() }
            Spacer(Modifier.height(Space.Xl))
            Text("UniCycle 0.1.0 · prototype build", style = UcType.Caption, color = UC.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(Space.Xl))
        }
    }
}

@Composable
private fun Chevron() { Icon(Icons.Outlined.ChevronRight, null, tint = UC.Muted) }

@Composable
fun EditProfileScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var name by remember { mutableStateOf(app.account?.name.orEmpty()) }
    var bio by remember { mutableStateOf(app.profile.bio) }
    var level by remember { mutableIntStateOf(app.profile.level) }
    ScreenScaffold("Edit profile", { nav.pop() }) {
        UcTextField(name, { name = it }, "Display name", error = if (name.isBlank()) "Name can't be empty" else null)
        Spacer(Modifier.height(Space.Md))
        UcTextField(bio, { bio = it.take(160) }, "Bio (optional)", singleLine = false, minLines = 3, supporting = "${bio.length}/160")
        Spacer(Modifier.height(Space.Lg))
        Text("Level", style = UcType.BodyStrong)
        Spacer(Modifier.height(Space.Sm))
        ChipFlow { (1..6).forEach { l -> UcChip("${l}00", level == l * 100) { level = l * 100 } } }
        Spacer(Modifier.height(Space.Xl))
        PrimaryButton("Save", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            app.renameAccount(name); app.updateProfile { it.copy(bio = bio.trim(), level = level) }; nav.pop()
        }
        Spacer(Modifier.height(Space.Lg))
    }
}

@Composable
fun AppearanceScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    ScreenScaffold("Appearance", { nav.pop() }) {
        Text("Your choice is remembered between sessions.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        ChipFlow { ThemeMode.entries.forEach { m -> UcChip(m.label, app.prefs.theme == m) { app.updatePrefs { it.copy(theme = m) } } } }
        Spacer(Modifier.height(Space.Md))
        Text("Midnight is the dark purple theme. Light is a separate light design, not an inverted one. System follows your phone.", style = UcType.BodySmall, color = UC.Muted)
    }
}

@Composable
fun AccessibilityControls() {
    val app = LocalApp.current
    val speaker = LocalSpeaker.current
    val prefs = app.prefs
    Text("Text size", style = UcType.BodyStrong)
    Spacer(Modifier.height(Space.Sm))
    ChipFlow {
        listOf("Default" to 1f, "Large" to 1.15f, "Extra large" to 1.3f).forEach { (label, scale) ->
            UcChip(label, prefs.textScale == scale) { app.updatePrefs { it.copy(textScale = scale) } }
        }
    }
    Spacer(Modifier.height(Space.Md))
    ToggleRow("High contrast", "Stronger text and borders", prefs.highContrast) { v -> app.updatePrefs { it.copy(highContrast = v) } }
    ToggleRow("Reduce motion", "Turns off animated transitions", prefs.reduceMotion) { v -> app.updatePrefs { it.copy(reduceMotion = v) } }
    ToggleRow("Read questions aloud automatically", "Uses your phone's text-to-speech voice", prefs.autoRead) { v -> app.updatePrefs { it.copy(autoRead = v) } }
    Spacer(Modifier.height(Space.Sm))
    Text("Speech speed", style = UcType.BodyStrong)
    Spacer(Modifier.height(Space.Sm))
    ChipFlow {
        listOf("Slow" to 0.75f, "Normal" to 1f, "Fast" to 1.25f, "Very fast" to 1.5f).forEach { (label, r) ->
            UcChip(label, prefs.speechRate == r) { app.updatePrefs { it.copy(speechRate = r) } }
        }
    }
    Spacer(Modifier.height(Space.Md))
    SecondaryButton("Play a sample", icon = Icons.AutoMirrored.Outlined.VolumeUp) {
        speaker.speak("This is how UniCycle reads questions and explanations aloud.", prefs.speechRate)
    }
}

@Composable
fun AccessibilityScreen() {
    val nav = LocalNav.current
    val speaker = LocalSpeaker.current
    ScreenScaffold("Accessibility", { speaker.stop(); nav.pop() }) {
        Text("UniCycle works with TalkBack. These settings are free for everyone.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Lg))
        AccessibilityControls()
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun PrivacyControls() {
    val app = LocalApp.current
    val prefs = app.prefs
    ToggleRow("Show my institution and department", "On your profile", prefs.showInstitution) { v -> app.updatePrefs { it.copy(showInstitution = v) } }
    ToggleRow("Show my activity", "Streaks and quiz activity", prefs.showActivity) { v -> app.updatePrefs { it.copy(showActivity = v) } }
    ToggleRow("Take part in leaderboards", "You can opt out of rankings", prefs.joinLeaderboards) { v -> app.updatePrefs { it.copy(joinLeaderboards = v) } }
}

@Composable
fun NotificationControls() {
    val app = LocalApp.current
    val prefs = app.prefs
    ToggleRow("Study reminders", null, prefs.notifyReminders) { v -> app.updatePrefs { it.copy(notifyReminders = v) } }
    ToggleRow("Replies and mentions", null, prefs.notifyReplies) { v -> app.updatePrefs { it.copy(notifyReplies = v) } }
    ToggleRow("Competition updates", null, prefs.notifyCompetitions) { v -> app.updatePrefs { it.copy(notifyCompetitions = v) } }
    Spacer(Modifier.height(Space.Sm))
    PrototypeBanner("Preferences are saved, but no notifications are sent until push is connected.")
}

@Composable
fun PrivacyScreen() {
    val nav = LocalNav.current
    ScreenScaffold("Privacy and safety", { nav.pop() }) {
        Text("Private academic records are never shown by default.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        PrivacyControls()
        Spacer(Modifier.height(Space.Lg))
        PrototypeBanner("Blocking, muting and message controls arrive with messaging. Reporting is already available on posts and questions.")
    }
}

@Composable
fun NotificationScreen() {
    val nav = LocalNav.current
    ScreenScaffold("Notifications", { nav.pop() }) { NotificationControls() }
}

@Composable
fun DataScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    ScreenScaffold("Data and offline", { nav.pop() }) {
        ToggleRow("Data saver", "Keeps downloads and media small", app.prefs.dataSaver) { v -> app.updatePrefs { it.copy(dataSaver = v) } }
        ToggleRow("Download on Wi-Fi only", "Large files wait for Wi-Fi", app.prefs.wifiOnly) { v -> app.updatePrefs { it.copy(wifiOnly = v) } }
        Spacer(Modifier.height(Space.Sm))
        PrototypeBanner("These two settings are saved and will apply to downloads and media once content comes from the server.")
        SectionHeader("On this device")
        Text("${app.notes.size} notes · ${app.cards.size} flashcards · ${app.attempts.size} quiz attempts", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        Text("Prototype course content is bundled, so it always works without a network.", style = UcType.BodySmall, color = UC.Muted)
    }
}

@Composable
fun AccountScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("Account and security", { nav.pop() }) {
        ListRow(app.account?.name.orEmpty(), app.account?.identifier, Icons.Outlined.Person)
        Spacer(Modifier.height(Space.Lg))
        SecondaryButton("Sign out", Modifier.fillMaxWidth()) { app.signOut() }
        Spacer(Modifier.height(Space.Xl))
        Text("Delete my data", style = UcType.H2)
        Text("Removes your account, notes, flashcards, quiz history, posts and settings from this device. This can't be undone.", style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        DestructiveButton("Delete all data on this device", Modifier.fillMaxWidth()) { confirm = true }
        Spacer(Modifier.height(Space.Lg))
        PrototypeBanner("Server-side account deletion and data export will be added with the backend.")
    }
    if (confirm) ConfirmDialog(
        "Delete everything?", "All local data will be erased and you'll return to the welcome screen.",
        "Delete", { confirm = false; app.deleteAllData() }, { confirm = false }, destructive = true,
    )
}
