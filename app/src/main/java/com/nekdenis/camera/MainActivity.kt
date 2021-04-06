package com.nekdenis.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.TaskExecutors
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.Face
import com.nekdenis.camera.detection.FaceDetectorProcessor
import com.nekdenis.camera.ui.theme.CameraComposeWorkshopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permissionGranted()) {
            initView()
        } else {
            requestPermission()
        }

    }

    private fun permissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), 0
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initView()
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initView() {
        setContent {
            CameraComposeWorkshopTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    var lens by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
                    CameraPreview(
                        cameraLens = lens
                    )
                    Controls(
                        onLensChange = { lens = switchLens(lens) }
                    )
                }
            }
        }
    }

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        cameraLens: Int
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        var sourceInfo by remember { mutableStateOf(SourceInfo(10, 10, false)) }
        var detectedFaces by remember { mutableStateOf<List<Face>>(emptyList()) }
        val previewView = remember { PreviewView(context) }
        val cameraProvider = remember(sourceInfo, cameraLens) {
            ProcessCameraProvider.getInstance(context)
                .configureCamera(
                    previewView, lifecycleOwner, cameraLens, context,
                    setSourceInfo = { sourceInfo = it },
                    onFacesDetected = { detectedFaces = it },
                )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            with(LocalDensity.current) {
                Box(
                    modifier = Modifier
                        .size(
                            height = sourceInfo.height.toDp(),
                            width = sourceInfo.width.toDp()
                        )
                        .scale(
                            calculateScale(
                                constraints,
                                sourceInfo,
                                PreviewScaleType.CENTER_CROP
                            )
                        )
                )
                {
                    CameraPreview(previewView)
                    DetectedFaces(faces = detectedFaces, sourceInfo = sourceInfo)
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(previewView: PreviewView) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            previewView.apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Preview is incorrectly scaled in Compose on some devices without this
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            previewView
        })
}

@Composable
fun DetectedFaces(
    faces: List<Face>,
    sourceInfo: SourceInfo
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val needToMirror = sourceInfo.isImageFlipped
        for (face in faces) {
            val left =
                if (needToMirror) size.width - face.boundingBox.right.toFloat() else face.boundingBox.left.toFloat()
            drawRect(
                Color.Gray, style = Stroke(2.dp.toPx()),
                topLeft = Offset(left, face.boundingBox.top.toFloat()),
                size = Size(face.boundingBox.width().toFloat(), face.boundingBox.height().toFloat())
            )
        }
    }
}

@Composable
fun Controls(
    onLensChange: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Button(
            onClick = onLensChange,
            modifier = Modifier.wrapContentSize()
        ) { Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch camera") }
    }
}

private fun ListenableFuture<ProcessCameraProvider>.configureCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraLens: Int,
    context: Context,
    setSourceInfo: (SourceInfo) -> Unit,
    onFacesDetected: (List<Face>) -> Unit
): ListenableFuture<ProcessCameraProvider> {
    addListener({
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraLens).build()

        val preview = androidx.camera.core.Preview.Builder()
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        val analysis = bindAnalysisUseCase(cameraLens, setSourceInfo, onFacesDetected)
        try {
            get().apply {
                unbindAll()
                bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
            }
        } catch (exc: Exception) {
            TODO("process errors")
        }
    }, ContextCompat.getMainExecutor(context))
    return this
}

private fun switchLens(lens: Int) = if (CameraSelector.LENS_FACING_FRONT == lens) {
    CameraSelector.LENS_FACING_BACK
} else {
    CameraSelector.LENS_FACING_FRONT
}


private fun bindAnalysisUseCase(
    lens: Int,
    setSourceInfo: (SourceInfo) -> Unit,
    onFacesDetected: (List<Face>) -> Unit
): ImageAnalysis? {

    val imageProcessor = try {
        FaceDetectorProcessor()
    } catch (e: Exception) {
        Log.e("CAMERA", "Can not create image processor", e)
        return null
    }
    val builder = ImageAnalysis.Builder()
    val analysisUseCase = builder.build()

    var sourceInfoUpdated = false

    analysisUseCase.setAnalyzer(
        TaskExecutors.MAIN_THREAD,
        { imageProxy: ImageProxy ->
            if (!sourceInfoUpdated) {
                setSourceInfo(obtainSourceInfo(lens, imageProxy))
                sourceInfoUpdated = true
            }
            try {
                imageProcessor.processImageProxy(imageProxy, onFacesDetected)
            } catch (e: MlKitException) {
                Log.e(
                    "CAMERA", "Failed to process image. Error: " + e.localizedMessage
                )
            }
        }
    )
    return analysisUseCase
}

private fun obtainSourceInfo(lens: Int, imageProxy: ImageProxy): SourceInfo {
    val isImageFlipped = lens == CameraSelector.LENS_FACING_FRONT
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    return if (rotationDegrees == 0 || rotationDegrees == 180) {
        SourceInfo(
            height = imageProxy.height, width = imageProxy.width, isImageFlipped = isImageFlipped
        )
    } else {
        SourceInfo(
            height = imageProxy.width, width = imageProxy.height, isImageFlipped = isImageFlipped
        )
    }
}

private fun calculateScale(
    constraints: Constraints,
    sourceInfo: SourceInfo,
    scaleType: PreviewScaleType
): Float {
    val heightRatio = constraints.maxHeight.toFloat() / sourceInfo.height
    val widthRatio = constraints.maxWidth.toFloat() / sourceInfo.width
    return when (scaleType) {
        PreviewScaleType.FIT_CENTER -> kotlin.math.min(heightRatio, widthRatio)
        PreviewScaleType.CENTER_CROP -> kotlin.math.max(heightRatio, widthRatio)
    }
}

data class SourceInfo(
    val width: Int,
    val height: Int,
    val isImageFlipped: Boolean,
)

private enum class PreviewScaleType {
    FIT_CENTER,
    CENTER_CROP
}

