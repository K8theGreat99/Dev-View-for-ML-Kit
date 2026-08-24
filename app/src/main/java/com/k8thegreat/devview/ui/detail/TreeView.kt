package com.k8thegreat.devview.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k8thegreat.devview.ocr.BoundingBox
import com.k8thegreat.devview.ocr.OcrBlock
import com.k8thegreat.devview.ocr.OcrDocument
import com.k8thegreat.devview.ocr.OcrLine

/**
 * ML Kit's output as a collapsible tree.
 *
 * Each node shows its stable path id (B2.L1.E3) alongside its text, because that id is
 * what notes and exports will anchor to. Depth is carried by indentation and by a
 * colored rail down the left of each level, so nesting stays readable on a phone.
 */
@Composable
fun TreeView(document: OcrDocument, modifier: Modifier = Modifier) {
    // Blocks start expanded, lines collapsed: the block layout is what you usually
    // want to read first, and expanding every word at once is unreadable.
    val expanded = remember(document) { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(document.blocks, key = { it.id }) { block ->
            BlockNode(
                block = block,
                isExpanded = expanded[block.id] ?: true,
                onToggle = { expanded[block.id] = !(expanded[block.id] ?: true) },
                isLineExpanded = { expanded[it] ?: false },
                onToggleLine = { expanded[it] = !(expanded[it] ?: false) },
            )
        }
    }
}

@Composable
private fun BlockNode(
    block: OcrBlock,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isLineExpanded: (String) -> Boolean,
    onToggleLine: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            NodeHeader(
                id = block.id,
                label = "block",
                summary = "${block.lines.size} line${if (block.lines.size == 1) "" else "s"}",
                box = block.box,
                depthColor = BlockColor,
                expandable = block.lines.isNotEmpty(),
                isExpanded = isExpanded,
                onToggle = onToggle,
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(Modifier.padding(start = 12.dp)) {
                    block.lines.forEach { line ->
                        LineNode(
                            line = line,
                            isExpanded = isLineExpanded(line.id),
                            onToggle = { onToggleLine(line.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineNode(line: OcrLine, isExpanded: Boolean, onToggle: () -> Unit) {
    Column(Modifier.rail(LineColor)) {
        NodeHeader(
            id = line.id,
            label = "line",
            summary = line.text,
            box = line.box,
            confidence = line.confidence,
            depthColor = LineColor,
            expandable = line.elements.isNotEmpty(),
            isExpanded = isExpanded,
            onToggle = onToggle,
        )

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.padding(start = 12.dp)) {
                line.elements.forEach { element ->
                    Column(Modifier.rail(WordColor)) {
                        NodeHeader(
                            id = element.id,
                            label = "word",
                            summary = element.text,
                            box = element.box,
                            confidence = element.confidence,
                            depthColor = WordColor,
                            expandable = false,
                            isExpanded = false,
                            onToggle = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeHeader(
    id: String,
    label: String,
    summary: String,
    box: BoundingBox,
    depthColor: Color,
    expandable: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    confidence: Float? = null,
) {
    var showDetail by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable { if (expandable) onToggle() else showDetail = !showDetail }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = id,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = depthColor,
            )
            Text(
                text = "  $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (expandable) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )

        // Coordinates are the reason this app exists, but they are noise at rest, so
        // they stay one tap away on leaf nodes.
        if (showDetail || expandable) {
            Text(
                text = buildString {
                    append("x ${box.left}–${box.right}  y ${box.top}–${box.bottom}")
                    append("  (${box.width}×${box.height})")
                    confidence?.let { append("  conf ${"%.2f".format(it)}") }
                },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A colored rail down the left edge, marking one level of nesting. */
private fun Modifier.rail(color: Color): Modifier =
    this
        .fillMaxWidth()
        .padding(start = 2.dp)
        .background(color.copy(alpha = 0.10f), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
        .padding(start = 6.dp)

private val BlockColor = Color(0xFF3D6373)
private val LineColor = Color(0xFF7A5C3E)
private val WordColor = Color(0xFF4A5D3A)
