package com.k8thegreat.devview.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Runs ML Kit text recognition and maps the result into [OcrDocument].
 *
 * Uses the bundled on-device Latin model: no network, no API key, no per-call cost.
 */
class OcrRunner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): OcrDocument {
        val text = recognizeRaw(bitmap)
        return text.toOcrDocument(bitmap.width, bitmap.height)
    }

    private suspend fun recognizeRaw(bitmap: Bitmap): Text =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
}

/**
 * Maps ML Kit's tree into our own, assigning each node its stable path id.
 *
 * The traversal stops at Element (word) level. ML Kit also exposes Symbols, but a
 * per-character dump inflates the JSON enormously while rarely changing how you would
 * write a parser. Revisit if a real case needs it.
 */
internal fun Text.toOcrDocument(imageWidth: Int, imageHeight: Int): OcrDocument {
    val blocks = textBlocks.mapIndexed { blockIndex, block ->
        val blockId = "B$blockIndex"

        val lines = block.lines.mapIndexed { lineIndex, line ->
            val lineId = "$blockId.L$lineIndex"

            val elements = line.elements.mapIndexed { elementIndex, element ->
                OcrElement(
                    id = "$lineId.E$elementIndex",
                    text = element.text,
                    box = element.boundingBox.toBoundingBox(),
                    confidence = element.confidence,
                    angle = element.angle,
                )
            }

            OcrLine(
                id = lineId,
                text = line.text,
                box = line.boundingBox.toBoundingBox(),
                confidence = line.confidence,
                angle = line.angle,
                recognizedLanguage = line.recognizedLanguage.takeIf { it.isNotBlank() },
                elements = elements,
            )
        }

        OcrBlock(
            id = blockId,
            text = block.text,
            box = block.boundingBox.toBoundingBox(),
            recognizedLanguage = block.recognizedLanguage.takeIf { it.isNotBlank() },
            lines = lines,
        )
    }

    return OcrDocument(imageWidth = imageWidth, imageHeight = imageHeight, blocks = blocks)
}

/** ML Kit's bounding boxes are nullable; an absent box collapses to zero. */
private fun Rect?.toBoundingBox(): BoundingBox =
    if (this == null) {
        BoundingBox(0, 0, 0, 0)
    } else {
        BoundingBox(left = left, top = top, right = right, bottom = bottom)
    }
