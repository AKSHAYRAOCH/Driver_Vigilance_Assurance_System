package com.akshayraoch.drivervigilanceassurance.ui.camera

import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class FaceRecognitionAnalyzer(
    private val onDetectedTextUpdated: (String) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        const val THROTTLE_TIMEOUT_MS = 1_000L
    }

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(highAccuracyOpts)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage: Image = imageProxy.image ?: run { imageProxy.close(); return@launch }
            val inputImage: InputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            var leftEyeOpenProb =0f
            var rightEyeOpenProb =0f
            val result = detector.process(inputImage)
                .addOnSuccessListener{faces ->
                    if(faces.size==0){
                        onDetectedTextUpdated("no faces found")
                    }
                    for (face in faces) {

                        if(face.rightEyeOpenProbability!=null) {
                            rightEyeOpenProb = face.rightEyeOpenProbability!!
                        }
                        if(face.leftEyeOpenProbability!=null){
                            leftEyeOpenProb = face.leftEyeOpenProbability!!
                        }
                        if(rightEyeOpenProb<0.3f && leftEyeOpenProb<0.3f) {
                            onDetectedTextUpdated("eyes are closed")
                        }else{
                            onDetectedTextUpdated("eyes are open")
                        }
                    }
                }
                .addOnFailureListener{ e ->
                    onDetectedTextUpdated("no faces found")
                }
            delay(THROTTLE_TIMEOUT_MS)
        }.invokeOnCompletion { exception ->
            exception?.printStackTrace()
            imageProxy.close()
        }
    }
}
