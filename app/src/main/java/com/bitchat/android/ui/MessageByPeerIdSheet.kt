package com.bitchat.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bitchat.android.R
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.bitchat.android.core.ui.component.sheet.BitchatSheetTopBar
import com.bitchat.android.util.PeerIdInputNormalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageByPeerIdSheet(
    isPresented: Boolean,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    if (!isPresented) return

    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var idInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val errEmpty = stringResource(R.string.message_by_id_err_empty)
    val errHex = stringResource(R.string.message_by_id_err_not_hex)
    val errShort = stringResource(R.string.message_by_id_err_short)

    fun trySubmit() {
        when (val r = PeerIdInputNormalizer.parse(idInput)) {
            is PeerIdInputNormalizer.ParseResult.Ok -> {
                errorText = null
                viewModel.openPrivateChatFromPeerIdInput(r.peerId)
                idInput = ""
                onDismiss()
            }
            is PeerIdInputNormalizer.ParseResult.Err -> {
                errorText = when (r.error) {
                    PeerIdInputNormalizer.ParseError.EMPTY -> errEmpty
                    PeerIdInputNormalizer.ParseError.NOT_HEX -> errHex
                    PeerIdInputNormalizer.ParseError.TOO_SHORT -> errShort
                }
            }
        }
    }

    BitchatBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp))
        ) {
            BitchatSheetTopBar(
                onClose = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = stringResource(R.string.message_by_id_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                }
            )

            Text(
                text = stringResource(R.string.message_by_id_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = idInput,
                onValueChange = {
                    idInput = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.message_by_id_field_label)) },
                placeholder = { Text(stringResource(R.string.message_by_id_field_hint)) },
                singleLine = false,
                minLines = 2,
                isError = errorText != null,
                supportingText = errorText?.let { msg -> { Text(msg) } },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { trySubmit() })
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.message_by_id_cancel))
                }
                Button(onClick = { trySubmit() }) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.message_by_id_open_chat))
                }
            }
        }
    }
}
