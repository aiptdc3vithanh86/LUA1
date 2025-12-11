
package com.example.eiapp

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var labels: List<String>
    private val executor = Executors.newSingleThreadExecutor()
    private val imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simple layout: PreviewView and TextView created programmatically
        previewView = PreviewView(this)
        previewView.layoutParams = androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
            androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.MATCH_PARENT,
            androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.MATCH_PARENT
        )
        resultText = TextView(this)
        resultText.setTextColor(Color.WHITE)
        resultText.textSize = 20f
        resultText.setPadding(16,16,16,32)

        val root = androidx.constraintlayout.widget.ConstraintLayout(this)
        root.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(previewView)
        root.addView(resultText)
        setContentView(root)

        // Load model and labels
        val model = FileUtil.loadMappedFile(this, "model.tflite")
        labels = FileUtil.loadLabels(this, "labels.txt")
        interpreter = Interpreter(model)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                val bitmap = toBitmap(imageProxy)
                if (bitmap != null) {
                    val resized = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
                    val result = classify(resized)
                    runOnUiThread {
                        resultText.text = result
                    }
                }
                imageProxy.close()
            })

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun classify(bitmap: Bitmap): String {
        val inputBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(imageSize * imageSize)
        bitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)
        for (pixel in intValues) {
            inputBuffer.putFloat(((pixel shr 16 and 0xFF) / 255.0f))
            inputBuffer.putFloat(((pixel shr 8 and 0xFF) / 255.0f))
            inputBuffer.putFloat(((pixel and 0xFF) / 255.0f))
        }
        inputBuffer.rewind()
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputBuffer, output)
        val probs = output[0]
        var maxIdx = 0
        var maxVal = probs[0]
        for (i in probs.indices) {
            if (probs[i] > maxVal) {
                maxVal = probs[i]; maxIdx = i
            }
        }
        val label = if (maxIdx < labels.size) labels[maxIdx] else "unknown"
        val percent = (maxVal * 100).toInt()
        return "$label: $percent%"
    }

    private fun toBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        // Convert YUV to RGB
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
