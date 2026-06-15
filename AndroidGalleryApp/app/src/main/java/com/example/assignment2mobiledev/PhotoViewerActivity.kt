package com.example.assignment2mobiledev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoId = intent.getStringExtra("photo_id") ?: ""
        setContent {
            MaterialTheme {
                SinglePhotoScreen(photoId)
            }
        }
    }
}

@Composable
fun SinglePhotoScreen(photoId: String) {
    val ctx = LocalContext.current
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(photoId) {
        val uri = photoUri(photoId)
        bmp = withContext(Dispatchers.IO) {
            // Request something larger than grid: e.g., 1440x1440
            decodeScaledBitmap(ctx.contentResolver, uri, 1440, 1440)
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)  // pinch zoom limit
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        bmp?.let { b ->
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )
        }
    }
}
