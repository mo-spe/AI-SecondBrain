package com.secondbrain.android.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.tasks.await

/** Runs recognition on the device so users can inspect text before any knowledge is sent to the server. */
class OcrRecognizer {
    suspend fun recognize(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            .process(image)
            .await()
            .text
    }
}
