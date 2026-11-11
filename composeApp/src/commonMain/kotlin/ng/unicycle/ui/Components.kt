package ng.unicycle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ng.unicycle.data.Provenance

private val CardShape = RoundedCornerShape(16.dp)
private val ControlShape = RoundedCornerShape(12.dp)

// ---- layout containers ---------------------------------------------------------------

@Composable
fun UcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = UC.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = UC.Border
    val base = modifier.fillMaxWidth().clip(CardShape).background(container).border(1.dp, border, CardShape)
    Column(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(Space.Lg),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = Space.Xl, bottom = Space.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = UcType.H2, modifier = Modifier.weight(1f).semantics { heading() })
        if (actionLabel != null && onAction != null) TertiaryButton(actionLabel, onClick = onAction)
    }
}

@Composable
fun UcTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.Xs, vertical = Space.Xs).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconAction(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack) else Spacer(Modifier.width(Space.Md))
        Text(
            title, style = UcType.H1, maxLines = 2,
            modifier = Modifier.weight(1f).padding(horizontal = Space.Xs).semantics { heading() },
        )
        actions()
    }
}

@Composable
fun PageTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = Space.Lg, bottom = Space.Md)) {
        Text(title, style = UcType.Display, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = UcType.Body, color = UC.Muted)
    }
}

// ---- buttons ---------------------------------------------------------------------------

@Composable
fun PrimaryButton(
    text: String, modifier: Modifier = Modifier, enabled: Boolean = true,
    loading: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit,
) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 48.dp), shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = UC.Primary, contentColor = UC.OnPrimary,
            disabledContainerColor = UC.Elevated, disabledContentColor = UC.Muted,
        ),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = UC.Muted)
        else {
            if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(Space.Sm)) }
            Text(text, style = UcType.BodyStrong)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String, modifier: Modifier = Modifier, enabled: Boolean = true,
    icon: ImageVector? = null, onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = ControlShape,
        border = BorderStroke(1.dp, if (enabled) UC.Primary else UC.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UC.Primary, disabledContentColor = UC.Muted),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(Space.Sm)) }
        Text(text, style = UcType.BodyStrong)
    }
}

@Composable
fun TertiaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = UC.Primary, disabledContentColor = UC.Muted),
    ) { Text(text, style = UcType.BodyStrong) }
}

@Composable
fun DestructiveButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = ControlShape,
        border = BorderStroke(1.dp, UC.Danger),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UC.Danger),
    ) { Text(text, style = UcType.BodyStrong) }
}

@Composable
fun IconAction(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = UC.Text, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

// ---- inputs ---------------------------------------------------------------------------------

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = UC.Surface, focusedContainerColor = UC.Surface, disabledContainerColor = UC.Surface,
    errorContainerColor = UC.Surface,
    unfocusedBorderColor = UC.Border, focusedBorderColor = UC.Primary, errorBorderColor = UC.Danger,
    focusedLabelColor = UC.Primary, unfocusedLabelColor = UC.Muted, cursorColor = UC.Primary,
)

@Composable
fun UcTextField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    password: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null, singleLine: Boolean = true, minLines: Int = 1, supporting: String? = null,
) {
    val note = error ?: supporting
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else minLines,
        shape = ControlShape, isError = error != null,
        supportingText = if (note != null) ({ Text(note) }) else null,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        colors = fieldColors(),
    )
}

@Composable
fun UcSearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), singleLine = true,
        placeholder = { Text(placeholder, color = UC.Muted) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = UC.Muted) },
        shape = ControlShape, colors = fieldColors(),
    )
}

@Composable
fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, placeholder: String = "Select") {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = UcType.Caption, color = UC.Muted, modifier = Modifier.padding(bottom = Space.Xs))
        Box {
            OutlinedButton(
                onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = ControlShape,
                border = BorderStroke(1.dp, UC.Border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = UC.Surface, contentColor = UC.Text),
            ) {
                Text(value.ifBlank { placeholder }, Modifier.weight(1f), color = if (value.isBlank()) UC.Muted else UC.Text)
                Icon(Icons.Outlined.ArrowDropDown, null, tint = UC.Muted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); open = false }) }
            }
        }
    }
}

@Composable
fun UcChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) UC.Selected else UC.Surface)
            .border(1.dp, if (selected) UC.Primary else UC.Border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = Space.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A check mark is shown for the selected state so selection is not colour-only.
        if (selected) { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = UC.OnSelected); Spacer(Modifier.width(Space.Xs)) }
        else if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = UC.Muted); Spacer(Modifier.width(Space.Xs)) }
        Text(label, style = UcType.BodySmall, color = if (selected) UC.OnSelected else UC.Text,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.Sm)) {
        items(options) { o -> UcChip(o, o == selected) { onSelect(o) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Sm), verticalArrangement = Arrangement.spacedBy(Space.Sm)) { content() }
}

@Composable
fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = Space.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Space.Md)) {
            Text(title, style = UcType.Body)
            if (subtitle != null) Text(subtitle, style = UcType.BodySmall, color = UC.Muted)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = UC.Primary, checkedThumbColor = UC.OnPrimary,
                uncheckedTrackColor = UC.Elevated, uncheckedThumbColor = UC.Muted, uncheckedBorderColor = UC.Border,
            ),
        )
    }
}

@Composable
fun ListRow(title: String, subtitle: String? = null, icon: ImageVector? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(vertical = Space.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = UC.Primary); Spacer(Modifier.width(Space.Md)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = UcType.Body)
            if (subtitle != null) Text(subtitle, style = UcType.BodySmall, color = UC.Muted)
        }
        trailing()
    }
}

// ---- status & feedback ------------------------------------------------------------------------

@Composable
fun ProvenanceBadge(p: Provenance) {
    val icon: ImageVector
    val bg: Color
    val fg: Color
    when (p) {
        Provenance.Verified -> { icon = Icons.Outlined.Verified; bg = UC.SuccessContainer; fg = UC.Success }
        Provenance.Community -> { icon = Icons.Outlined.Groups; bg = UC.Elevated; fg = UC.Text }
        Provenance.Ai -> { icon = Icons.Outlined.AutoAwesome; bg = UC.Selected; fg = UC.OnSelected }
        Provenance.UnderReview -> { icon = Icons.Outlined.HourglassEmpty; bg = UC.AmberContainer; fg = UC.Amber }
    }
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = Space.Sm, vertical = 4.dp)
            .semantics { contentDescription = "Content status: ${p.label}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = fg)
        Spacer(Modifier.width(Space.Xs))
        Text(p.label, style = UcType.Caption, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, tint: Color = UC.Primary) {
    UcCard(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(Space.Sm))
        Text(value, style = UcType.Numeric)
        Text(label, style = UcType.Caption, color = UC.Muted)
    }
}

@Composable
fun UcProgress(progress: Float, modifier: Modifier = Modifier) {
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            .semantics { contentDescription = "Progress $pct percent" },
        color = UC.Success, trackColor = UC.Elevated,
    )
}

@Composable
fun Avatar(initial: String, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(UC.Selected),
        contentAlignment = Alignment.Center,
    ) { Text(initial.take(1).uppercase(), style = UcType.H2, color = UC.OnSelected) }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.Xl, horizontal = Space.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(40.dp), tint = UC.Muted)
        Spacer(Modifier.height(Space.Md))
        Text(title, style = UcType.H2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.Xs))
        Text(body, style = UcType.Body, color = UC.Muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.Lg))
            PrimaryButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    UcCard(container = UC.DangerContainer) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = UC.Danger)
            Spacer(Modifier.width(Space.Md))
            Text(message, style = UcType.Body, modifier = Modifier.weight(1f).semantics { contentDescription = "Error: $message" })
        }
        if (onRetry != null) { Spacer(Modifier.height(Space.Md)); SecondaryButton("Try again", onClick = onRetry) }
    }
}

@Composable
fun SkeletonBlock(height: Int = 16, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(8.dp)).background(UC.Elevated))
}

/** Clearly marks features that run on prototype data or local-only storage. */
@Composable
fun PrototypeBanner(text: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(UC.AmberContainer).padding(Space.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Science, null, tint = UC.Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Space.Sm))
        Text(text, style = UcType.BodySmall)
    }
}

@Composable
fun ConfirmDialog(
    title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    destructive: Boolean = false, dismissLabel: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = UC.Elevated, titleContentColor = UC.Text, textContentColor = UC.Muted,
        title = { Text(title, style = UcType.H2) },
        text = { Text(body, style = UcType.Body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (destructive) UC.Danger else UC.Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel, color = UC.Muted) } },
    )
}
