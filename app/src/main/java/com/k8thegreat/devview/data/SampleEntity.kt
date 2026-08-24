package com.k8thegreat.devview.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One image in the gallery, together with the OCR result it produced.
 *
 * The OCR tree is stored as serialized JSON rather than in relational tables. It is
 * always read whole, never queried across, and JSON is the export format anyway — so
 * three extra tables and their joins would buy nothing.
 */
@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey val id: String,
    /** File name inside the app's private images directory. */
    val fileName: String,
    val displayName: String,
    val addedAt: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    /** Serialized [com.k8thegreat.devview.ocr.OcrDocument]. */
    val ocrJson: String,
    // Denormalized so the gallery grid does not have to parse every document to
    // render its badges.
    val blockCount: Int,
    val lineCount: Int,
    val wordCount: Int,
)
