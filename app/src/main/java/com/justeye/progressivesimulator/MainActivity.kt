package com.justeye.progressivesimulator

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPermission.launch(Manifest.permission.CAMERA)

        setContent {
            JustEyeApp()
        }
    }
}

@Composable
fun JustEyeApp() {

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

                if (compare == null) {

                    SingleSimulation(
                        design = design,
                        onDesign = { design = it },
                        onCompare = { compare = it }
                    )

                } else {

                    CompareSimulation(
                        mode = compare!!,
                        onBack = { compare = null }
                    )
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
            text = "JUST EYE",
            color = Color(0xFFE4B95E),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "PROGRESSIVE VISION SIMULATOR",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SingleSimulation(
    design: Int,
    onDesign: (Int) -> Unit,
    onCompare: (String) -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )

        ProgressiveOverlay(
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
                text = when (design) {
                    1 -> "FRAME 1 • CONVENTIONAL • NARROW FOV"
                    2 -> "FRAME 2 • IMPROVED • WIDER FOV"
                    else -> "FRAME 3 • ADVANCED • WIDEST FOV"
                },
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                (1..3).forEach { number ->

                    Button(
                        onClick = {
                            onDesign(number)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (design == number)
                                    Color(0xFFD7A84A)
                                else
                                    Color(0xFF252525)
                        )
                    ) {
                        Text("$number")
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

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
                        onClick = {
                            onCompare(label)
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "DEMO ONLY — simulated field of view, not a prescription or exact lens performance.",
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
            mode = mode,
            modifier = Modifier.fillMaxSize()
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
                text = "COMPARE $mode",
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

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
    modifier: Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode =
                PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            previewView
        }
    )

    DisposableEffect(
        lifecycleOwner,
        previewView
    ) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        var cameraProvider: ProcessCameraProvider? = null

        val executor =
            ContextCompat.getMainExecutor(context)

        val listener = Runnable {

            try {

                cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    Preview.Builder().build()

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )

            } catch (_: Exception) {
            }
        }

        cameraProviderFuture.addListener(
            listener,
            executor
        )

        onDispose {

            cameraProvider?.unbindAll()
        }
    }
}
