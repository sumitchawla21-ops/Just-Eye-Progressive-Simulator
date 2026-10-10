package com.justeye.progressivesimulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Bitmap
import android.util.Size
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        setContent {
            JustEyeApp(cameraPermissionGranted = granted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            setContent {
                JustEyeApp(cameraPermissionGranted = true)
            }
        } else {
            setContent {
                JustEyeApp(cameraPermissionGranted = false)
            }

            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun JustEyeApp(cameraPermissionGranted: Boolean) {

    var design by remember { mutableIntStateOf(1) }
    var compare by remember { mutableStateOf<String?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD7A84A),
            background = Color(0xFF050505),
            surface = Color(0xFF111111)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Header()

                if (!cameraPermissionGranted) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera permission is required\nfor the Progressive Vision Simulator.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                } else {

                    if (compare == null) {
                        SingleSimulation(
                            design = design,
                            onDesign = { design = it },
                            onCompare = { compare = it }
                        )
                    } else {
                        CompareSimulation(compare!!) {
                            compare = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080808))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "JUST EYE",
            color = Color(0xFFE4B95E),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "PROGRESSIVE VISION SIMULATOR",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ColumnScope.SingleSimulation(
    design: Int,
    onDesign: (Int) -> Unit,
    onCompare: (String) -> Unit
) {
    var blurredFrame by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            blurredFrame?.recycle()
        }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {

        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onBlurFrame = { frame ->
                val previous = blurredFrame
                blurredFrame = frame
                if (previous != null && previous !== frame && !previous.isRecycled) {
                    previous.recycle()
                }
            }
        )

        PeripheralBlurLayer(
            bitmap = blurredFrame,
            design = design,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xDD050505))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                when (design) {
                    1 -> "FRAME 1  •  CONVENTIONAL  •  NARROW FOV"
                    2 -> "FRAME 2  •  IMPROVED  •  WIDER FOV"
                    else -> "FRAME 3  •  ADVANCED  •  WIDEST FOV"
                },
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..3).forEach { n ->

                    Button(
                        onClick = { onDesign(n) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (design == n)
                                    Color(0xFFD7A84A)
                                else
                                    Color(0xFF252525)
                        )
                    ) {
                        Text("$n")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                listOf(
                    "1 vs 2",
                    "1 vs 3",
                    "2 vs 3",
                    "1 vs 2 vs 3"
                ).forEach { label ->

                    OutlinedButton(
                        onClick = { onCompare(label) }
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                "DEMO ONLY — simulated field of view, not a prescription or exact lens performance.",
                color = Color(0xFFAAAAAA),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CompareSimulation(
    mode: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )

        CompareOverlay(
            mode,
            Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xDD050505))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "COMPARE $mode",
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onBack
            ) {
                Text("BACK")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    onBlurFrame: ((Bitmap) -> Unit)? = null
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = surfaceProvider
                    }

                    val analysis = if (onBlurFrame != null) {
                        ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 480))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also { useCase ->
                                val frameCounter = AtomicInteger(0)
                                useCase.setAnalyzer(analysisExecutor) { image: ImageProxy ->
                                    if (frameCounter.incrementAndGet() % 4 != 0) {
                                        image.close()
                                    } else {
                                        image.toPeripheralBlurBitmap()?.let(onBlurFrame)
                                    }
                                }
                            }
                    } else null

                    provider.unbindAll()
                    if (analysis != null) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } else {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

@Composable
private fun PeripheralBlurLayer(
    bitmap: Bitmap?,
    design: Int,
    modifier: Modifier = Modifier
) {
    if (bitmap == null || bitmap.isRecycled) return

    val corridorFraction = when (design) {
        1 -> 0.36f
        2 -> 0.50f
        else -> 0.64f
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Simulated peripheral blur",
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            val w = size.width
            val h = size.height
            val topClearY = h * 0.20f
            val halfCorridorAtBottom = w * corridorFraction / 2f
            val corridor = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, topClearY)
                lineTo(w / 2f + halfCorridorAtBottom, h)
                lineTo(w / 2f - halfCorridorAtBottom, h)
                lineTo(0f, topClearY)
                close()
            }
            clipPath(corridor, ClipOp.Difference) {
                this@drawWithContent.drawContent()
            }
        }
    )
}
