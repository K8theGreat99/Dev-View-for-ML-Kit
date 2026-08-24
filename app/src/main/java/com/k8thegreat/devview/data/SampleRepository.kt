package com.k8thegreat.devview.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.k8thegreat.devview.ocr.OcrDocument
import com.k8thegreat.devview.ocr.OcrRunner
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SampleRepository(
    private val context: Context,
    private val dao: SampleDao,
    private val ocrRunner: OcrRunner = OcrRunner(),
) {

    private val json = Json { prettyPrint = true; encodeDefaults = true }
    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    fun observeAll(): Flow<List<Sample>> = dao.observeAll().map { list -> list.map(::toSample) }

    fun observeById(id: String): Flow<Sample?> = dao.observeById(id).map { it?.let(::toSample) }

    fun imageFile(fileName: String): File = File(imagesDir, fileName)

    /**
     * Copies an image into app storage, runs OCR on it, and stores both.
     *
     * The picker hands out temporary URIs that stop working once the app restarts, so
     * the bytes have to be copied in for the gallery to survive.
     *
     * Note that OCR runs on the *same* downscaled bitmap that gets saved. That keeps
     * every bounding box valid against the stored file forever, with no scale factor
     * to track and no chance of the overlay drifting out of alignment later.
     */
    suspend fun addImage(uri: Uri): Result<Sample> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(uri) ?: error("Could not read that image")

            val id = UUID.randomUUID().toString()
            val fileName = "$id.jpg"
            File(imagesDir, fileName).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            val document = ocrRunner.recognize(bitmap)
            val entity = SampleEntity(
                id = id,
                fileName = fileName,
                displayName = displayName(uri),
                addedAt = System.currentTimeMillis(),
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                ocrJson = json.encodeToString(OcrDocument.serializer(), document),
                blockCount = document.blockCount,
                lineCount = document.lineCount,
                wordCount = document.wordCount,
            )
            dao.insert(entity)
            bitmap.recycle()
            toSample(entity)
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.findById(id)?.let { File(imagesDir, it.fileName).delete() }
        dao.deleteById(id)
    }

    private fun toSample(entity: SampleEntity) = Sample(
        entity = entity,
        document = json.decodeFromString(OcrDocument.serializer(), entity.ocrJson),
        file = File(imagesDir, entity.fileName),
    )

    /** Pretty-printed JSON exactly as stored, for the JSON tab and later export. */
    fun prettyJson(document: OcrDocument): String =
        json.encodeToString(OcrDocument.serializer(), document)

    private fun displayName(uri: Uri): String {
        val last = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
        return last?.takeIf { it.isNotBlank() } ?: "Image"
    }

    /**
     * Decodes at most [MAX_DIMENSION] on the long edge, with EXIF rotation applied.
     *
     * A full-resolution phone photo is tens of megabytes as a bitmap and several of
     * them at once will exhaust memory. Rotation matters because ML Kit reads the
     * pixels as given: a photo whose upright orientation lives only in its EXIF tag
     * would otherwise be recognized sideways.
     */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val rotation = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (rotation == 0f) return decoded
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it != decoded) decoded.recycle() }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > MAX_DIMENSION) sample *= 2
        return sample
    }

    companion object {
        /** Long-edge cap. Well above ML Kit's needs, well below what exhausts memory. */
        const val MAX_DIMENSION = 2048
        private const val JPEG_QUALITY = 92
    }
}

/** An entity paired with its parsed OCR tree and on-disk image. */
data class Sample(
    val entity: SampleEntity,
    val document: OcrDocument,
    val file: File,
) {
    val id: String get() = entity.id
}
