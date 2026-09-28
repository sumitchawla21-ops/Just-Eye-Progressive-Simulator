package com.justeye.progressivesimulator

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.platform.LocalContext
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermission.launch(Manifest.permission.CAMERA)
        setContent { JustEyeApp() }
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
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Header()
                if (compare == null) {
                    SingleSimulation(design, onDesign = { design = it }, onCompare = { compare = it })
                } else {
                    CompareSimulation(compare!!) { compare = null }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF080808)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JUST EYE", color = Color(0xFFE4B95E), style = MaterialTheme.typography.headlineSmall)
        Text("PROGRESSIVE VISION SIMULATOR", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SingleSimulation(design: Int, onDesign: (Int) -> Unit, onCompare: (String) -> Unit) {
   Column(modifier = Modifier.fillMaxWidth()) {
        CameraPreview(Modifier.fillMaxSize())
        ProgressiveOverlay(design, Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xDD050505)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when(design) {
                    1 -> "FRAME 1  •  CONVENTIONAL  •  NARROW FOV"
                    2 -> "FRAME 2  •  IMPROVED  •  WIDER FOV"
                    else -> "FRAME 3  •  ADVANCED  •  WIDEST FOV"
                },
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { n ->
                    Button(
                        onClick = { onDesign(n) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (design == n) Color(0xFFD7A84A) else Color(0xFF252525)
                        )
                    ) { Text("$n") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("1 vs 2", "1 vs 3", "2 vs 3", "1 vs 2 vs 3").forEach { label ->
                    OutlinedButton(onClick = { onCompare(label) }) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
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
private fun CompareSimulation(mode: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CameraPreview(Modifier.fillMaxSize())
        // Prototype comparison overlay: same live camera feed, divided comparison zones.
        CompareOverlay(mode, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xDD050505)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("COMPARE $mode", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("BACK") }
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
    PreviewView(ctx).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
    }
}
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
val preview = androidx.camera.core.Preview.Builder().build()
preview.setSurfaceProvider(it.surfaceProvider)
provider.unbindAll()
provider.bindToLifecycle(
    lifecycleOwner,
    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
    preview
)
                    )
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}
