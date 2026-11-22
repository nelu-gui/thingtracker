package com.N3looG.thingtracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun saveResizedImage(context: Context, sourceUri: Uri, maxDimension: Int = 1800): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            saveBitmapToInternalStorage(context, bitmap, maxDimension)
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, maxDimension: Int = 1800): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height

            val scale = if ((width > maxDimension) || (height > maxDimension)) {
                maxDimension.toFloat() / maxOf(width, height)
            } else {
                1f
            }

            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()

            val resizedBitmap = if (scale < 1f) {
                bitmap.scale(newWidth, newHeight, true)
            } else {
                bitmap
            }

            val file = File(context.filesDir, "item_img_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
