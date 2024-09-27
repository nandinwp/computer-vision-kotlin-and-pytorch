package com.demo.loginapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ImageView
import android.widget.TextView

class DashboardActivity : ComponentActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CAMERA_PERMISSION = 100
    private lateinit var module: Module
    private var capturedImage: Bitmap? = null
    private lateinit var classes: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboardactivity)

        // Carregar as classes
        classes = loadClasses()

        try {
            module = Module.load(assetFilePath("mobilenet_v2.ptl"))
            Toast.makeText(this, "Modelo carregado com sucesso!", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Erro ao carregar o modelo: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Argumento inválido ao carregar o modelo: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro desconhecido: ${e.message}", Toast.LENGTH_LONG).show()
        }

        val openCameraButton: Button = this.findViewById(R.id.open_camera_button)
        openCameraButton.setOnClickListener {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent()
            } else {
                requestCameraPermission()
            }
        }

        val finishSessionButton: Button = this.findViewById(R.id.finishSession)
        finishSessionButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    @Throws(IOException::class)
    private fun assetFilePath(assetName: String): String {
        val file = File(filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }

    private fun loadClasses(): List<String> {
        val classes = mutableListOf<String>()
        try {
            val inputStream = assets.open("imagenet-classes.txt")
            inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    val classNames = line.split(",").map { it.trim() }
                    classes.addAll(classNames)
                }
            }
        } catch (e: IOException) {
            Log.e("DashboardActivity", "Error loading classes: ${e.message}")
        } catch (e: NullPointerException) {
            Log.e("DashboardActivity", "Null pointer error: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("DashboardActivity", "Security error: ${e.message}")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Unknown error: ${e.message}")
        }
        return classes
    }



    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } ?: run {
                Toast.makeText(this, "Nenhuma aplicação de câmera encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                capturedImage = data?.extras?.get("data") as? Bitmap
                capturedImage?.let {

                    val imageView: ImageView = findViewById(R.id.captured_image_view)
                    imageView.setImageBitmap(it)

                    Toast.makeText(this, "Foto tirada com sucesso", Toast.LENGTH_SHORT).show()
                    processImage(it)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val textPredictionLabel: TextView = this.findViewById(R.id.prediction_text_model)

        try {
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )

            val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
            val probabilities = outputTensor.dataAsFloatArray


            val class1Probability = probabilities[0]
            val class2Probability = probabilities[1]

            val classProbabilities = listOf(class1Probability, class2Probability)

            val maxIndex = classProbabilities.indices.maxByOrNull { classProbabilities[it] }
                ?: throw IllegalStateException("Could not find maximum likelihood index.")

            val predictedClass = if (maxIndex in classes.indices) classes[maxIndex] else "Desconhecido"
            val score = classProbabilities[maxIndex]

            val formattedScore = String.format("%.2f", score * 100).replace(".", ",")
            val firstTwoDigits = formattedScore.take(2)

            textPredictionLabel.text = "The image contains a $predictedClass !"
            Toast.makeText(this, "Prediction: $firstTwoDigits", Toast.LENGTH_SHORT).show()

        } catch (e: IllegalStateException) {
            Log.e("DashboardActivity", "Error in correspondence between probabilities and classes: ${e.message}")
            Toast.makeText(this, "Error loading classes!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error processing image: ${e.localizedMessage}", e)
            Toast.makeText(this, "Error processing image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
