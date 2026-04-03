package com.bitchat.android.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.R
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.bitchat.android.core.ui.component.sheet.BitchatSheetTopBar
import com.bitchat.android.ledger.LedgerEntryRow
import com.bitchat.android.ledger.LedgerRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerLibrarySheet(
    isPresented: Boolean,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    if (!isPresented) return

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val repo = remember(context) { LedgerRepository.getInstance(context.applicationContext) }
    val entries by repo.entries.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.publishLedgerFromUri(it) }
    }

    BitchatBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BitchatSheetTopBar(
                onClose = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = stringResource(R.string.ledger_library_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            openDoc.launch(arrayOf("*/*"))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.ledger_add_document)
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.ledger_library_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.contentHashHex }) { row ->
                    val canOpen = row.hasBlob || repo.hasBlob(row.contentHashHex)
                    LedgerEntryRowUi(
                        row = row,
                        canOpen = canOpen,
                        onOpen = {
                            val f = repo.blobFile(row.contentHashHex)
                            if (!f.isFile) return@LedgerEntryRowUi
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                f
                            )
                            openLedgerBlob(context, uri, row.mimeType)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryRowUi(
    row: LedgerEntryRow,
    canOpen: Boolean,
    onOpen: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpen) { onOpen() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            tint = if (canOpen) colorScheme.primary else colorScheme.outline
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.ledger_entry_meta,
                    row.creatorPeerId.take(8),
                    DateUtils.getRelativeTimeSpanString(
                        row.timestampMs,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.65f),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = row.contentHashHex.take(14) + "…",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!canOpen) {
                Text(
                    text = stringResource(R.string.ledger_pending_blob),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.tertiary
                )
            }
        }
        if (canOpen) {
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.ledger_open),
                tint = colorScheme.primary
            )
        }
    }
    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
}

private fun openLedgerBlob(context: Context, uri: Uri, mimeType: String) {
    val tryOpen: (String) -> Boolean = { type ->
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = android.content.ClipData.newUri(context.contentResolver, "ledger_blob", uri)
        }
        try {
            context.startActivity(openIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    if (tryOpen(mimeType)) return
    if (tryOpen("*/*")) return

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        clipData = android.content.ClipData.newUri(context.contentResolver, "ledger_blob", uri)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Open with"))
    } catch (_: Exception) {
        Toast.makeText(context, "No app available to open this document", Toast.LENGTH_SHORT).show()
    }
}
