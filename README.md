# Camera Workshop that uses Camera X with Compose


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
