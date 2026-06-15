package com.example.assignment2mobiledev


import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import kotlin.math.max

data class Photo(
    val id: String,
    val orientation: Int,
    val width: Int,
    val height: Int
)

object ThumbCache {
    private val cacheSize = (Runtime.getRuntime().maxMemory() / 8).toInt()
    private val lru = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    fun get(key: String): Bitmap? = lru.get(key)
    fun put(key: String, bmp: Bitmap) { lru.put(key, bmp) }
}

fun photoUri(id: String): Uri =
    Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

fun decodeScaledBitmap(cr: ContentResolver, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    // 1) Decode bounds only
    cr.openInputStream(uri)?.use { input ->
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, opts)
        // 2) Calculate inSampleSize
        val sample = calculateInSampleSize(opts.outWidth, opts.outHeight, reqWidth, reqHeight)
        // 3) Decode actual bitmap
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        cr.openInputStream(uri)?.use { input2 ->
            return BitmapFactory.decodeStream(input2, null, opts2)
        }
    }
    return null
}

fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
    var inSampleSize = 1
    if (srcH > reqH || srcW > reqW) {
        var halfH = srcH / 2
        var halfW = srcW / 2
        while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
            inSampleSize *= 2
        }
    }
    return max(1, inSampleSize)
}
