package com.k8thegreat.devview.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.k8thegreat.devview.ui.GalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sampleId: String,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
) {
    val sampleFlow = remember(sampleId) { viewModel.observeSample(sampleId) }
    val sample by sampleFlow.collectAsStateWithLifecycle(initialValue = null)
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val current = sample
    // Serializing the whole tree is not free, and composition can run many times per
    // frame. Do it once per sample, not once per recomposition.
    val prettyJson = remember(current?.id, current?.entity?.ocrJson) {
        current?.let(viewModel::prettyJson).orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.entity?.displayName ?: "Sample") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = {
                            copyToClipboard(context, prettyJson)
                            scope.launch { snackbarHostState.showSnackbar("JSON copied") }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy JSON")
                        }
                        IconButton(onClick = {
                            viewModel.delete(sampleId)
                            onBack()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            AsyncImage(
                model = current.file,
                contentDescription = current.entity.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
            )

            Text(
                text = "${current.entity.imageWidth}×${current.entity.imageHeight}px · " +
                    "${current.document.blockCount} blocks · " +
                    "${current.document.lineCount} lines · " +
                    "${current.document.wordCount} words",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider()

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tree") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("JSON") })
            }

            when (tab) {
                0 -> TreeView(current.document, Modifier.weight(1f))
                else -> JsonView(prettyJson, Modifier.weight(1f))
            }
        }
    }
}

/**
 * The raw stored JSON, monospaced and scrollable in both directions.
 *
 * Horizontal scrolling rather than wrapping: wrapped JSON is much harder to scan, and
 * this pane exists to show the exact structure that would be handed to an LLM.
 */
@Composable
private fun JsonView(json: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = json,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ML Kit output", text))
}
