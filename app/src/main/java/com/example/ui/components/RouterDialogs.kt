package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberShield
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseOffline
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_permission_banner"),
        shape = RoundedCornerShape(16.dp),
        color = AmberContainer,
        border = BorderStroke(1.dp, AmberShield.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = AmberShield,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.notification_permission_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("grant_notification_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberShield,
                    contentColor = MidnightBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.notification_permission_action),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun BottomActionsRow(
    onAddPriorClick: () -> Unit,
    onMonthlyResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddPriorClick,
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("add_prior_gb_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanContainer,
                contentColor = CyanAccent
            ),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = stringResource(R.string.cd_add_icon),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_add_prior),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = onMonthlyResetClick,
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("monthly_reset_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = RoseContainer.copy(alpha = 0.65f),
                contentColor = RoseOffline
            ),
            border = BorderStroke(1.dp, RoseOffline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.cd_reset_icon),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_monthly_reset),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AddPriorUsageDialog(
    onDismiss: () -> Unit,
    onConfirm: (downloadGb: Double, uploadGb: Double) -> Unit
) {
    var downloadText by remember { mutableStateOf("") }
    var uploadText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.dialog_add_prior_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.dialog_add_prior_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = downloadText,
                    onValueChange = {
                        downloadText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.dialog_download_gb_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_gb_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = SlateMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prior_download_gb_input")
                )

                OutlinedTextField(
                    value = uploadText,
                    onValueChange = {
                        uploadText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.dialog_upload_gb_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_gb_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = PurpleAccent,
                        unfocusedLabelColor = SlateMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PurpleAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prior_upload_gb_input")
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.dialog_invalid_number),
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseOffline
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rx = parseOptionalNonNegativeDouble(downloadText)
                    val tx = parseOptionalNonNegativeDouble(uploadText)
                    if (rx == null || tx == null || (rx == 0.0 && tx == 0.0)) {
                        showError = true
                    } else {
                        onConfirm(rx, tx)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = MidnightBg
                ),
                modifier = Modifier.testTag("confirm_add_prior_button")
            ) {
                Text(stringResource(R.string.dialog_confirm_add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_prior_button")
            ) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = SlateMuted
                )
            }
        }
    )
}

@Composable
fun MonthlyResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.dialog_reset_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_reset_message),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoseOffline,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.testTag("confirm_monthly_reset_button")
            ) {
                Text(stringResource(R.string.dialog_confirm_reset))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_monthly_reset_button")
            ) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = SlateMuted
                )
            }
        }
    )
}

private fun parseOptionalNonNegativeDouble(raw: String): Double? {
    val trimmed = raw.trim().replace(',', '.')
    if (trimmed.isEmpty()) return 0.0
    val parsed = trimmed.toDoubleOrNull() ?: return null
    return if (parsed >= 0.0 && parsed.isFinite()) parsed else null
}
