package com.secondbrain.android.capture

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/** Uses CameraX in-app preview so capture feels continuous while still keeping the image only in local cache. */
@Composable
fun CameraCaptureScreen(
    onCaptured: (android.net.Uri) -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PreviewView(it).apply { this.controller = controller } },
            // A bounded preview leaves the shutter reachable on compact devices.
            modifier = Modifier.fillMaxWidth().height(520.dp)
        )
        Button(
            onClick = {
                val file = createCaptureFile(context)
                val options = ImageCapture.OutputFileOptions.Builder(file).build()
                controller.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        onCaptured(androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file))
                    }
                    override fun onError(exception: ImageCaptureException) {
                        onError(exception.message ?: "相机拍摄失败，请重试。")
                    }
                })
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) { Text("拍摄并识别") }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Text("取消") }
    }
}

private fun createCaptureFile(context: Context): File =
    File(context.cacheDir, "captures").apply { mkdirs() }.let { File.createTempFile("capture_", ".jpg", it) }
