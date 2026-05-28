package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object IconProvider {
    private val cache = LruCache<String, ImageBitmap>(50)

    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            val bitmap = icon.toBitmap(width = 64, height = 64)
            val imageBitmap = bitmap.asImageBitmap()
            cache.put(packageName, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }
}
