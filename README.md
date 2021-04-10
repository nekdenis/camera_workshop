# Example of CameraX with Compose and MLKit

During this workshop we are going to build camera app with CameraX, Compose and MLKit.

[CameraX](https://developer.android.com/training/camerax) will simplify working with camera on Android for us.

[Jetpack Compose](https://developer.android.com/jetpack/compose) is the medern standard of UI on Android.

[MLKit](https://developers.google.com/ml-kit) is a great tool for machine learnining processing of images for tasks such as text recognition, face detection and many more. 

Note: everythings can be broken because of new versions of CameraX and especially Compose (now in Beta)

You can pull code branch-by-branch, or follow steps below, or watch [my presentation here](https://docs.google.com/presentation/d/1sJoKcrmwnAVeF-MhHVvYnHD32-J-HVDLhsx2U3wKiWo/edit?usp=sharing).

For code, please, checkout Pull Requests: 

* [Create empty project](https://github.com/nekdenis/camera_workshop/pull/1/files)
* [Request camera pemission](https://github.com/nekdenis/camera_workshop/pull/2/files)
* [Show Camera Preview](https://github.com/nekdenis/camera_workshop/pull/3/files)
* [Add camera switch](https://github.com/nekdenis/camera_workshop/pull/4/files)
* [Add face detection and preview scale](https://github.com/nekdenis/camera_workshop/pull/5/files)
* [Add pose detection](https://github.com/nekdenis/camera_workshop/pull/6/files)


## Step 1. New project

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/1/files)
   
Adding compose support into build.gradle of your module:
    
    implementation("androidx.compose.ui:ui:${rootProject.extra["compose_version"]}")
    implementation("androidx.compose.material:material:${rootProject.extra["compose_version"]}")
    implementation("androidx.compose.ui:ui-tooling:${rootProject.extra["compose_version"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.activity:activity-compose:${rootProject.extra["activity_compose_version"]}")
   
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["compose_version"] as String
    }
    
Adding cameraX dependencies:

    implementation("androidx.camera:camera-core:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-camera2:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-lifecycle:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-view:${rootProject.extra["cameraview_version"]}")
    
Creating basic composable:

    setContent {
      Greeting("Android")
    }

    @Composable
    fun Greeting(name: String) {
      Text(text = "Hello $name!")
    }

## Step 2. Requesting permissions

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/2/files)
   
Adding permission to Manifest.xml:

      <uses-permission android:name="android.permission.CAMERA" />

Requesting permission:

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initView()
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }


## Step 3. Showing camera preview

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/3/files)
   
Adding preview composable function:

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val previewView = remember { PreviewView(context) }
        val cameraProviderFuture = remember {
            ProcessCameraProvider.getInstance(context)
                .configureCamera(previewView, lifecycleOwner, cameraSelector, context)
        }
        AndroidView(
            modifier = modifier,
            factory = {
                previewView.apply {
                    this.scaleType = scaleType
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewView
            })
    }
    
Configuring basic camera: 

      private fun ListenableFuture<ProcessCameraProvider>.configureCamera(
         previewView: PreviewView,
         lifecycleOwner: LifecycleOwner,
         cameraSelector: CameraSelector,
         context: Context
      ): ListenableFuture<ProcessCameraProvider> {
         addListener({
         val preview = androidx.camera.core.Preview.Builder()
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
         try {
            get().apply {
                unbindAll()
                bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview
                )
            }
          } catch (exc: Exception) {
            TODO("process errors")
          }
       }, ContextCompat.getMainExecutor(context))
       return this
      }

## Step 4. Adding camera switch button

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/4/files)

<img src="https://user-images.githubusercontent.com/2456891/114135203-c4078480-98bd-11eb-9dff-0fe6d2849ba1.png" width="200" />

Adding preview composable function:

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

For binding new lens value to camera [see changes in PR](https://github.com/nekdenis/camera_workshop/pull/4/files)

## Step 5. Adding face detection

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/5/files)

![face_detection mp4](https://user-images.githubusercontent.com/2456891/114136957-5a3caa00-98c0-11eb-8b3b-11665c3c2865.gif)

Adding MLKit dependencies to build.gradle

      implementation("com.google.mlkit:face-detection:16.0.6")
      implementation("com.google.android.gms:play-services-mlkit-face-detection:16.1.5")

find latests versions of library [here](https://developers.google.com/ml-kit/release-notes)


Here is a simple class that wraps MLKit face detection processor

      class FaceDetectorProcessor {

          private val detector: FaceDetector

          private val executor = TaskExecutors.MAIN_THREAD

          init {
              val faceDetectorOptions = FaceDetectorOptions.Builder()
                  .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                  .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                  .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                  .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                  .setMinFaceSize(0.4f)
                  .build()

              detector = FaceDetection.getClient(faceDetectorOptions)
          }

          fun stop() {
              detector.close()
          }

          @SuppressLint("UnsafeExperimentalUsageError")
          fun processImageProxy(image: ImageProxy, onDetectionFinished: (List<Face>) -> Unit) {
              detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
                  .addOnSuccessListener(executor) { results: List<Face> -> onDetectionFinished(results) }
                  .addOnFailureListener(executor) { e: Exception ->
                      Log.e("Camera", "Error detecting face", e)
                  }
                  .addOnCompleteListener { image.close() }
          }
      }


Bind analysis use case to camera that is calling FaceDetectorProcessor

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

Adding composable function that draws face oval

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
      
      
Where SourceInfo is just the info about selected camera preview

      data class SourceInfo(
          val width: Int,
          val height: Int,
          val isImageFlipped: Boolean,
      )
      
Face detection working! But there is one problem:

![face_detection_broken mp4](https://user-images.githubusercontent.com/2456891/114258840-4192c980-997e-11eb-8754-06a1c6c000a0.gif)

Fix scale by placing preview and face into the same coordinates

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

where the scale calculation is very simple

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
      
Now it is working fine:

![face_detection mp4](https://user-images.githubusercontent.com/2456891/114136957-5a3caa00-98c0-11eb-8b3b-11665c3c2865.gif)

For full list of changes in the code [see changes in PR](https://github.com/nekdenis/camera_workshop/pull/5/files)


## Step 6. Adding pose detection

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/6/files)

![pose-detection mp4](https://user-images.githubusercontent.com/2456891/114259291-a996df00-9981-11eb-8849-b9a582e87fe8.gif)

To refer changes in this PR please look into files. Changes are very trivial.





