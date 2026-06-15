package com.example.assignment2mobiledev


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GalleryScreen()
            }
        }
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(perm)
        }
    }
}

@Composable
fun GalleryScreen() {
    val ctx = LocalContext.current
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        photos = withContext(Dispatchers.IO) { queryAllPhotos(ctx.contentResolver) }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading photos…")
        }
        return
    }

    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No photos found")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp)
    ) {
        items(photos, key = { it.id }) { p ->
            ThumbnailItem(p)
        }
    }
}

@Composable
fun ThumbnailItem(photo: Photo) {
    val ctx = LocalContext.current
    var bmp by remember(photo.id) { mutableStateOf(android.graphics.Bitmap.createBitmap(1,1, android.graphics.Bitmap.Config.ARGB_8888)) }
    var isLoaded by remember(photo.id) { mutableStateOf(false) }

    // Load or get from cache (IO thread)
    LaunchedEffect(photo.id) {
        val cached = ThumbCache.get(photo.id)
        if (cached != null) {
            bmp = cached
            isLoaded = true
        } else {
            val uri = photoUri(photo.id)
            val decoded = withContext(Dispatchers.IO) {
                decodeScaledBitmap(ctx.contentResolver, uri, 360, 360) // good size for grid cells
            }
            decoded?.let {
                ThumbCache.put(photo.id, it)
                bmp = it
                isLoaded = true
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .aspectRatio(1f) // square cells
            .clickable {
                // Open full screen viewer
                val i = Intent(ctx, PhotoViewerActivity::class.java)
                i.putExtra("photo_id", photo.id)
                ctx.startActivity(i)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoaded) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("…")
        }
    }
}

suspend fun queryAllPhotos(cr: android.content.ContentResolver): List<Photo> = withContext(Dispatchers.IO) {
    val result = mutableListOf<Photo>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    cr.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val oCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
        val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol).toString()
            val o = cursor.getInt(oCol)
            val w = cursor.getInt(wCol)
            val h = cursor.getInt(hCol)
            result.add(Photo(id, o, w, h))
        }
    }
    result
}
