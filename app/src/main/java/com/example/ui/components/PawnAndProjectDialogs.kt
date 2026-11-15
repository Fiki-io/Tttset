package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.ProjectSummary
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens
import java.text.DateFormat
import java.util.Date

@Composable
fun ImportPawnDialog(
    onImport: (code: String, replaceExisting: Boolean) -> String?,
    onDismiss: () -> Unit
) {
    var codeText by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.import_dialog_title),
        maxWidth = 600.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)
        ) {
            ModeChoiceButton(
                label = stringResource(R.string.import_mode_replace),
                selected = replaceExisting,
                onClick = { replaceExisting = true },
                modifier = Modifier.weight(1f)
            )
            ModeChoiceButton(
                label = stringResource(R.string.import_mode_append),
                selected = !replaceExisting,
                onClick = { replaceExisting = false },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space8))

        OutlinedTextField(
            value = codeText,
            onValueChange = {
                codeText = it
                errorMessage = null
            },
            placeholder = { Text(stringResource(R.string.import_dialog_hint), style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = inspectorTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .testTag("import_pawn_input")
        )

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(AppDimens.Space6))
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.StatusError
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            StudioSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            StudioPrimaryButton(
                text = stringResource(R.string.action_process_import),
                onClick = {
                    val err = onImport(codeText, replaceExisting)
                    if (err == null) {
                        onDismiss()
                    } else {
                        errorMessage = err
                    }
                },
                modifier = Modifier.weight(1f).testTag("import_pawn_confirm")
            )
        }
    }
}

@Composable
fun ExportPawnDialog(
    syntaxMode: PawnSyntaxMode,
    onChangeSyntaxMode: (PawnSyntaxMode) -> Unit,
    generateCode: (includeShowCalls: Boolean) -> String,
    onCopyCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var includeShowCalls by remember { mutableStateOf(false) }
    val generated = remember(syntaxMode, includeShowCalls) { generateCode(includeShowCalls) }

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.export_dialog_title),
        maxWidth = 620.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)
        ) {
            ModeChoiceButton(
                label = stringResource(R.string.syntax_samp_classic),
                selected = syntaxMode == PawnSyntaxMode.SAMP_CLASSIC,
                onClick = { onChangeSyntaxMode(PawnSyntaxMode.SAMP_CLASSIC) },
                modifier = Modifier.weight(1f)
            )
            ModeChoiceButton(
                label = stringResource(R.string.syntax_open_mp),
                selected = syntaxMode == PawnSyntaxMode.OPEN_MP,
                onClick = { onChangeSyntaxMode(PawnSyntaxMode.OPEN_MP) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space4))
        StudioSwitchRow(
            label = stringResource(R.string.export_include_show),
            checked = includeShowCalls,
            onCheckedChange = { includeShowCalls = it },
            testTag = "export_include_show_switch"
        )

        Spacer(modifier = Modifier.height(AppDimens.Space6))
        if (generated.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                    .background(AppColors.CanvasBackdrop)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.export_empty_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                    .background(AppColors.CanvasBackdrop)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
                    .padding(AppDimens.Space8)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = generated,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = AppColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            StudioSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            StudioPrimaryButton(
                text = stringResource(R.string.action_copy_code),
                enabled = generated.isNotEmpty(),
                onClick = {
                    onCopyCode(generated)
                    onDismiss()
                },
                modifier = Modifier.weight(1f).testTag("export_copy_button")
            )
        }
    }
}

@Composable
fun ProjectManagerDialog(
    projects: List<ProjectSummary>,
    activeProjectId: Long?,
    onOpenProject: (Long) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }
    var renamingTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var renameBuffer by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<ProjectSummary?>(null) }
    val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        StudioDialog(
            onDismissRequest = { deleteCandidate = null },
            title = stringResource(R.string.project_delete_confirm_title),
            maxWidth = 420.dp
        ) {
            Text(
                text = stringResource(R.string.project_delete_confirm_body, target.name),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(AppDimens.Space16))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
            ) {
                StudioSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { deleteCandidate = null },
                    modifier = Modifier.weight(1f)
                )
                StudioPrimaryButton(
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        onDeleteProject(target.id)
                        deleteCandidate = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (renamingTarget != null) {
        val target = renamingTarget!!
        StudioDialog(
            onDismissRequest = { renamingTarget = null },
            title = stringResource(R.string.action_rename),
            maxWidth = 420.dp
        ) {
            OutlinedTextField(
                value = renameBuffer,
                onValueChange = { renameBuffer = it },
                label = { Text(stringResource(R.string.project_name_hint), style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                colors = inspectorTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.Space12))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
            ) {
                StudioSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { renamingTarget = null },
                    modifier = Modifier.weight(1f)
                )
                StudioPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        onRenameProject(target.id, renameBuffer)
                        renamingTarget = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.projects_dialog_title),
        maxWidth = 560.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)
        ) {
            OutlinedTextField(
                value = newProjectName,
                onValueChange = { newProjectName = it },
                placeholder = { Text(stringResource(R.string.project_name_hint), style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                colors = inspectorTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("new_project_name_input")
            )
            StudioPrimaryButton(
                text = stringResource(R.string.action_create_project),
                onClick = {
                    onCreateProject(newProjectName)
                    newProjectName = ""
                    onDismiss()
                },
                modifier = Modifier.testTag("create_project_button")
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space6)
        ) {
            items(projects, key = { it.id }) { proj ->
                val isCurrent = proj.id == activeProjectId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                        .background(if (isCurrent) AppColors.SurfaceSelected else AppColors.SurfaceCard)
                        .border(
                            AppDimens.BorderThin,
                            if (isCurrent) AppColors.AccentBlue else AppColors.BorderSubtle,
                            RoundedCornerShape(AppDimens.RadiusSmall)
                        )
                        .clickable {
                            onOpenProject(proj.id)
                            onDismiss()
                        }
                        .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.project_meta_format,
                                proj.elementCount,
                                dateFormatter.format(Date(proj.updatedAt))
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AppColors.AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.Space6))
                    }
                    IconButton(
                        onClick = {
                            renamingTarget = proj
                            renameBuffer = proj.name
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_rename),
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { deleteCandidate = proj },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(if (selected) AppColors.AccentBlueSoft else AppColors.SurfaceInteractive)
            .border(
                AppDimens.BorderThin,
                if (selected) AppColors.AccentBlue else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space8),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
