package com.sathish.soundharajan.passwd.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sathish.soundharajan.passwd.data.DocumentInfo
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewer(
    documentInfo: DocumentInfo,
    documentData: ByteArray,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            // Top toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Document info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = documentInfo.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${documentInfo.fileType} • ${(documentData.size / 1024)} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Zoom controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scale = (scale * 0.8f).coerceAtLeast(0.5f) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { scale = (scale * 1.2f).coerceAtMost(3f) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Document content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    documentInfo.fileType.startsWith("image/") -> {
                        // Display image with zoom and pan
                        val bitmap = remember(documentData) {
                            BitmapFactory.decodeByteArray(documentData, 0, documentData.size)
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = documentInfo.fileName,
                                modifier = Modifier
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y,
                                        rotationZ = rotation
                                    )
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, rot ->
                                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                                            offset += pan
                                            rotation += rot
                                        }
                                    }
                            )
                        } else {
                            Text(
                                text = "Unable to display image",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    documentInfo.fileType == "application/pdf" -> {
                        // PDF viewer placeholder - would need PDF library
                        GlassCard(
                            modifier = Modifier.padding(16.dp),
                            backgroundColor = Color.White.copy(alpha = 0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "PDF Viewer",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PDF viewing requires additional library integration",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "${documentData.size} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    documentInfo.fileType.startsWith("text/") -> {
                        // Text file viewer
                        val textContent = remember(documentData) {
                            String(documentData, Charsets.UTF_8)
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            backgroundColor = Color.White.copy(alpha = 0.95f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Text Document",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = textContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }

                    else -> {
                        // Unsupported file type
                        GlassCard(
                            modifier = Modifier.padding(16.dp),
                            backgroundColor = Color.White.copy(alpha = 0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Unsupported File Type",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cannot display ${documentInfo.fileType} files",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "${documentInfo.fileName}\n${documentData.size} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Bottom info bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                backgroundColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoom: ${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Pinch to zoom • Drag to pan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotation = 0f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentThumbnail(
    documentInfo: DocumentInfo,
    documentData: ByteArray,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassCard(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        backgroundColor = Color.White.copy(alpha = 0.1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                documentInfo.fileType.startsWith("image/") -> {
                    val bitmap = remember(documentData) {
                        try {
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeByteArray(documentData, 0, documentData.size, options)

                            val scale = minOf(
                                options.outWidth / 80,
                                options.outHeight / 80
                            ).coerceAtLeast(1)

                            BitmapFactory.Options().apply {
                                inSampleSize = scale
                            }.let { scaledOptions ->
                                BitmapFactory.decodeByteArray(documentData, 0, documentData.size, scaledOptions)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = documentInfo.fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                documentInfo.fileType == "application/pdf" -> {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }

                documentInfo.fileType.startsWith("text/") -> {
                    Icon(
                        Icons.Default.TextSnippet,
                        contentDescription = null,
                        tint = Color.Blue.copy(alpha = 0.7f)
                    )
                }

                else -> {
                    Icon(
                        Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            // File type indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = getFileExtension(documentInfo.fileName).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

private fun getFileExtension(fileName: String): String {
    return fileName.substringAfterLast('.', "").take(3)
}
