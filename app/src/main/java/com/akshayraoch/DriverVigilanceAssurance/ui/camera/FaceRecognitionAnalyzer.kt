package com.akshayraoch.drivervigilanceassurance.ui.camera

import android.content.Context
import android.media.Image
import android.media.MediaPlayer
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.akshayraoch.drivervigilanceassurance.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch


class FaceRecognitionAnalyzer(
    private val onDetectedTextUpdated: (String) -> Unit
, context : Context
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


    val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.alarm)

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

                        if(faces[0].rightEyeOpenProbability!=null) {
                            rightEyeOpenProb = face.rightEyeOpenProbability!!
                        }
                        if(faces[0].leftEyeOpenProbability!=null){
                            leftEyeOpenProb = face.leftEyeOpenProbability!!
                        }
                        if(rightEyeOpenProb<0.3f && leftEyeOpenProb<0.3f) {
                            onDetectedTextUpdated("eyes are closed")
                            mediaPlayer.start()
                            println("audio is playing")

                        }else{
                            onDetectedTextUpdated("eyes are open")

                            try{mediaPlayer.stop()
                                mediaPlayer.prepare()}
                            catch(e:Error){
                                println(e)
                            }
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
