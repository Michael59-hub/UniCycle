package ng.unicycle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.unicycle.*
import ng.unicycle.data.*

private fun timeAgo(now: Long, then: Long): String {
    val s = (now - then) / 1000
    return when {
        s < 60 -> "Just now"
        s < 3600 -> "${s / 60} min ago"
        s < 86_400 -> "${s / 3600} h ago"
        else -> "${s / 86_400} d ago"
    }
}

private val PostReportCategories = listOf("Spam", "Harassment", "Misinformation", "Inappropriate content")

@Composable
fun PostCard(post: Post, compact: Boolean = false) {
    val app = LocalApp.current
    var showComments by remember(post.id) { mutableStateOf(false) }
    var comment by remember(post.id) { mutableStateOf("") }
    var reporting by remember(post.id) { mutableStateOf(false) }
    var confirmDelete by remember(post.id) { mutableStateOf(false) }

    UcCard(Modifier.padding(bottom = if (compact) 0.dp else Space.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.author, 36)
            Spacer(Modifier.width(Space.Md))
            Column(Modifier.weight(1f)) {
                Text(post.author, style = UcType.BodyStrong)
                Text(
                    "${timeAgo(app.nowMillis(), post.millis)}${if (post.courseCode.isNotBlank()) " · ${post.courseCode}" else ""} · ${post.type.label}",
                    style = UcType.Caption, color = UC.Muted,
                )
            }
        }
        Spacer(Modifier.height(Space.Sm))
        Text(post.text, style = UcType.Body, maxLines = if (compact) 3 else Int.MAX_VALUE)
        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction(
                    if (post.liked) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbUpOffAlt, if (post.liked) "Unlike" else "Like",
                    { app.updatePost(post.id) { it.copy(liked = !it.liked) } }, tint = if (post.liked) UC.Primary else UC.Text,
                )
                Text(if (post.liked) "1" else "0", style = UcType.Caption, color = UC.Muted)
                IconAction(Icons.Outlined.ChatBubbleOutline, "Comments", { showComments = !showComments })
                Text("${post.comments.size}", style = UcType.Caption, color = UC.Muted)
                Spacer(Modifier.weight(1f))
                IconAction(
                    if (post.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (post.saved) "Unsave post" else "Save post",
                    { app.updatePost(post.id) { it.copy(saved = !it.saved) } }, tint = if (post.saved) UC.Primary else UC.Text,
                )
                IconAction(Icons.Outlined.Flag, "Report post", { reporting = true })
                IconAction(Icons.Outlined.Delete, "Delete post", { confirmDelete = true }, tint = UC.Muted)
            }
            if (showComments) {
                post.comments.forEach { c ->
                    Column(Modifier.padding(vertical = Space.Xs)) {
                        Text("${c.author} · ${timeAgo(app.nowMillis(), c.millis)}", style = UcType.Caption, color = UC.Muted)
                        Text(c.text, style = UcType.Body)
                    }
                }
                Spacer(Modifier.height(Space.Sm))
                UcTextField(comment, { comment = it }, "Add a comment")
                Spacer(Modifier.height(Space.Sm))
                PrimaryButton("Comment", enabled = comment.isNotBlank()) { app.addComment(post.id, comment); comment = "" }
            }
        }
    }
    if (reporting) ReportDialog("Report this post", PostReportCategories, { c -> app.report("post", post.id, c); reporting = false }, { reporting = false })
    if (confirmDelete) ConfirmDialog(
        "Delete post?", "This removes the post from this device.", "Delete",
        { confirmDelete = false; app.deletePost(post.id) }, { confirmDelete = false }, destructive = true,
    )
}

@Composable
private fun Composer(fixedCourse: String?, onDone: () -> Unit) {
    val app = LocalApp.current
    var type by remember { mutableStateOf(PostType.Text) }
    var course by remember { mutableStateOf(fixedCourse ?: "") }
    var text by remember { mutableStateOf("") }
    UcCard {
        ChipFlow { PostType.entries.forEach { t -> UcChip(t.label, type == t) { type = t } } }
        Spacer(Modifier.height(Space.Md))
        UcTextField(text, { text = it.take(500) }, "Share something with your coursemates", singleLine = false, minLines = 3, supporting = "${text.length}/500")
        if (fixedCourse == null) {
            Spacer(Modifier.height(Space.Md))
            DropdownField("Course (optional)", course, app.myCourses.map { it.code }, { course = it }, placeholder = "General")
        }
        Spacer(Modifier.height(Space.Md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
            SecondaryButton("Cancel", Modifier.weight(1f)) { onDone() }
            PrimaryButton("Post", Modifier.weight(1f), enabled = text.isNotBlank()) { app.publishPost(type, course, text); onDone() }
        }
    }
}

@Composable
fun CommunityScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    var tab by remember { mutableStateOf("For You") }
    var composing by remember { mutableStateOf(false) }
    val visible = app.posts.filter { !it.hidden }
    val shown = when (tab) {
        "Trending" -> visible.sortedByDescending { (if (it.liked) 1 else 0) + it.comments.size }
        "Following" -> emptyList()
        else -> visible
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.Page)) {
        PageTitle("Community", "Talk to your coursemates")
        PrototypeBanner("Prototype: posts and comments are saved on this device only. Other students will appear once the server is connected.")
        SectionHeader("Lecture Halls")
        if (app.profile.institutionName.isNotBlank()) {
            ListRow(app.profile.institutionName, "Institution hall", Icons.Outlined.School, { nav.push(Route.Hall("")) }) { Icon(Icons.Outlined.ChevronRight, null, tint = UC.Muted) }
        }
        app.myCourses.forEach { c ->
            ListRow("${c.code} hall", c.title, Icons.Outlined.Groups, { nav.push(Route.Hall(c.code)) }) { Icon(Icons.Outlined.ChevronRight, null, tint = UC.Muted) }
        }

        SectionHeader("Feed")
        ChipRow(listOf("For You", "Following", "My University", "Trending"), tab) { tab = it }
        Spacer(Modifier.height(Space.Md))
        if (!composing) PrimaryButton("New post", Modifier.fillMaxWidth(), icon = Icons.Outlined.Add) { composing = true }
        else Composer(null) { composing = false }
        Spacer(Modifier.height(Space.Md))
        if (tab == "Following") {
            EmptyState(Icons.Outlined.PersonAdd, "Not following anyone yet", "Following other students needs the server. Their posts will show up here.")
        } else if (shown.isEmpty()) {
            EmptyState(Icons.Outlined.Forum, "No posts yet", "Start a discussion, ask a question or share a study tip.")
        } else shown.forEach { PostCard(it) }
        Spacer(Modifier.height(Space.Xl))
    }
}

@Composable
fun HallScreen(code: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    var composing by remember { mutableStateOf(false) }
    val course = app.content.courses().firstOrNull { it.code == code }
    val title = if (code.isEmpty()) app.profile.institutionName else "${course?.code ?: code} hall"
    val posts = app.posts.filter { !it.hidden && it.courseCode == code }

    ScreenScaffold(title, { nav.pop() }) {
        Text(if (code.isEmpty()) "Open to your whole institution." else course?.title.orEmpty(), style = UcType.Body, color = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        UcCard {
            Text("Hall rules", style = UcType.H2)
            Text("Be respectful. No spam, harassment or cheating. Share sources for claims. Report anything that breaks these rules.", style = UcType.BodySmall, color = UC.Muted)
        }
        Spacer(Modifier.height(Space.Md))
        PrototypeBanner("Prototype: you're the only member on this device. Channels, voice rooms, polls and moderator tools arrive with the server.")
        Spacer(Modifier.height(Space.Md))
        if (!composing) PrimaryButton("Post in this hall", Modifier.fillMaxWidth(), icon = Icons.Outlined.Add) { composing = true }
        else Composer(code) { composing = false }
        Spacer(Modifier.height(Space.Md))
        if (posts.isEmpty()) EmptyState(Icons.Outlined.Forum, "No posts in this hall", "Be the first to start a discussion.")
        posts.forEach { PostCard(it) }
        Spacer(Modifier.height(Space.Xl))
    }
}
