# Example of CameraX with Compose and MLKit

During this workshop we are going to build camera app with CameraX, Compose and MLKit.

[CameraX](https://developer.android.com/training/camerax) will simplify working with camera on Android for us.

[Jetpack Compose](https://developer.android.com/jetpack/compose) is the medern standard of UI on Android.

[MLKit](https://developers.google.com/ml-kit) is a great tool for machine learnining processing of images for tasks such as text recognition, face detection and many more. 

Note: everythings can be broken because of new versions of CameraX and especially Compose (now in Beta)

For code, please, checkout Pull Requests: 

* [Create empty project](https://github.com/nekdenis/camera_workshop/pull/1/files)
* [Request camera pemission](https://github.com/nekdenis/camera_workshop/pull/2/files)
* [Show Camera Preview](https://github.com/nekdenis/camera_workshop/pull/3/files)
* [Add camera switch](https://github.com/nekdenis/camera_workshop/pull/4/files)
* [Add face detection and preview scale](https://github.com/nekdenis/camera_workshop/pull/5/files)
* [Add pose detection](https://github.com/nekdenis/camera_workshop/pull/6/files)


## New project

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

## Requesting permissions

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


## Showing camera preview

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

## Adding camera switch button

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

## Adding face detection

[PR with changes](https://github.com/nekdenis/camera_workshop/pull/5/files)

![face_detection mp4](https://user-images.githubusercontent.com/2456891/114136957-5a3caa00-98c0-11eb-8b3b-11665c3c2865.gif)
