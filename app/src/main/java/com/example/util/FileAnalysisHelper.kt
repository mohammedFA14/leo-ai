package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class FileAnalysisResult(
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val isImage: Boolean,
    val base64Data: String? = null,
    val textContent: String? = null,
    val formattedSize: String
)

object FileAnalysisHelper {

    fun processUri(context: Context, uri: Uri): FileAnalysisResult? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        var fileName = "file_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        val formattedSize = formatFileSize(sizeBytes)
        val isImage = mimeType.startsWith("image/") || fileName.endsWith(".jpg", true) ||
                fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) ||
                fileName.endsWith(".webp", true)

        return if (isImage) {
            val base64 = encodeImageToBase64(context, uri)
            FileAnalysisResult(
                uriString = uri.toString(),
                fileName = fileName,
                mimeType = if (mimeType.startsWith("image/")) mimeType else "image/jpeg",
                isImage = true,
                base64Data = base64,
                formattedSize = formattedSize
            )
        } else {
            val text = readTextFromUri(context, uri)
            FileAnalysisResult(
                uriString = uri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                isImage = false,
                textContent = text,
                formattedSize = formattedSize
            )
        }
    }

    private fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Scale down to max 1024 dimension for optimal transmission
            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: byteArrayOf()
            inputStream?.close()

            // Limit read to 64KB for safety
            val textSample = String(bytes.take(65536).toByteArray(), Charsets.UTF_8)
            textSample
        } catch (e: Exception) {
            "تعذر قراءة محتوى الملف كنص: ${e.localizedMessage}"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
