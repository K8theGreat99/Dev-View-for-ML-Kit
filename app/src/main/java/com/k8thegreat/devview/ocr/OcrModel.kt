package com.k8thegreat.devview.ocr

import kotlinx.serialization.Serializable

/**
 * A serializable snapshot of one ML Kit recognition result.
 *
 * ML Kit's own [com.google.mlkit.vision.text.Text] cannot be stored or exported, so it
 * is mapped into these types on import. The shape deliberately mirrors ML Kit's tree
 * — Block, Line, Element — because the whole point of the app is to show what ML Kit
 * actually returns, not a tidied-up version of it.
 *
 * There is no flat-text field. ML Kit's `Text.text` is derived from this tree (block
 * texts joined with newlines), so storing it would duplicate data we already have,
 * while losing the block boundaries that make the tree worth having.
 */
@Serializable
data class OcrDocument(
    /** Dimensions of the bitmap ML Kit was given. Every box below is relative to it. */
    val imageWidth: Int,
    val imageHeight: Int,
    val blocks: List<OcrBlock>,
) {
    val blockCount: Int get() = blocks.size
    val lineCount: Int get() = blocks.sumOf { it.lines.size }
    val wordCount: Int get() = blocks.sumOf { block -> block.lines.sumOf { it.elements.size } }
}

/**
 * A paragraph-ish region.
 *
 * Note that block order is ML Kit's *detection* order, not reading order. On
 * multi-column layouts the indices genuinely jump around, which is exactly the
 * behaviour Dev View exists to make visible.
 */
@Serializable
data class OcrBlock(
    /** Stable path id, e.g. `B2`. Used to anchor notes and cross-reference exports. */
    val id: String,
    val text: String,
    val box: BoundingBox,
    val recognizedLanguage: String? = null,
    val lines: List<OcrLine>,
)

@Serializable
data class OcrLine(
    /** Stable path id, e.g. `B2.L1`. */
    val id: String,
    val text: String,
    val box: BoundingBox,
    val confidence: Float? = null,
    val angle: Float? = null,
    val recognizedLanguage: String? = null,
    val elements: List<OcrElement>,
)

/** Roughly a word. */
@Serializable
data class OcrElement(
    /** Stable path id, e.g. `B2.L1.E3`. */
    val id: String,
    val text: String,
    val box: BoundingBox,
    val confidence: Float? = null,
    val angle: Float? = null,
)

/** Pixel coordinates in the source bitmap. */
@Serializable
data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}
