package com.seedbx.leafdisease

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class SubmitActivity : AppCompatActivity() {

    /** A [APIService] */
    lateinit var service: APIService

    /** A [Uri]? used to store the uri of image to be submitted */
    private var imageUri: Uri? = null

    private lateinit var previewImageView: ImageView

    private val activityScope = CoroutineScope(SupervisorJob())

    /**
     *
     *
     * @return A [File]
     */
    private fun getFile(): File {

        val imageName = "Image_"

        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(imageName, ".jpg", storageDir)
    }

    private fun showPopup(plantData: PlantData?,constraintLayout: ConstraintLayout) {
        if(plantData==null)
            return
        val detailPopupWindow = DetailPopupWindow()
        if (plantData.diseaseCategory == "Healthy")
            detailPopupWindow.showHealthyPopupWindow(constraintLayout, plantData.plantName)
        else
            detailPopupWindow.showDiseasePopupWindow(
                constraintLayout,
                plantData.plantName,
                plantData.diseaseCategory,
                plantData.diseaseName
            )
    }

    /**
     *
     *
     * @param file A [File]
     */
    private suspend fun sendImageToServer(file: File): PlantData? {
        return try {
            val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file)
            val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val responseBody = service.sendImage(multipartBody)
            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SubmitActivity", e.toString())
            //Toast.makeText(this,"Some Error Occurred Server",Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     *
     *
     * @param imageFile A [File]
     */
    private fun send(imageFile: File) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val submitActivityConstraintLayout =
            findViewById<ConstraintLayout>(R.id.submitActivityConstraintLayout)
        progressBar.visibility = View.VISIBLE
        submitActivityConstraintLayout.background =
            ColorDrawable(ContextCompat.getColor(this, R.color.shadow))
        var plantData: PlantData?
        activityScope.launch {
            plantData = sendImageToServer(imageFile)
            /*plantData?.let { showPopup(it) }
                ?: launch(Dispatchers.Main) {
                    Toast.makeText(this@SubmitActivity, "Some Error Occurred", Toast.LENGTH_SHORT)
                        .show()
                }*/
            if (plantData == null) {
                launch(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    submitActivityConstraintLayout.background = ColorDrawable(Color.TRANSPARENT)
                    Toast.makeText(this@SubmitActivity, "Some Error Occurred", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                launch(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    //submitActivityConstraintLayout.background = ColorDrawable(Color.TRANSPARENT)
                    showPopup(plantData,submitActivityConstraintLayout)
                }
            }
        }
    }

    /***/
    private val submitButtonClickListener = View.OnClickListener {
        if (imageUri == null)
            Toast.makeText(this, "Some Error Occurred", Toast.LENGTH_SHORT).show()
        else {
            val path = imageUri!!.path

            var imageFile = File(path)
            if (imageFile.exists())
                send(imageFile)
            else {

                val inputStream = contentResolver.openInputStream(imageUri!!)
                imageFile = getFile()
                val outputStream = FileOutputStream(imageFile)
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream!!.read(buf).also { len = it } > 0) {
                    outputStream.write(buf, 0, len)
                }

                outputStream.close()
                inputStream.close()

                //runBlocking { sendImageToServer(imageFile) }
                send(imageFile)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit)

        /** A [Retrofit] */
        val retrofit = Retrofit.Builder().baseUrl(getString(R.string.url))
            .addConverterFactory(GsonConverterFactory.create()).build()
        service = retrofit.create(APIService::class.java)

        /** A */
        val intent = intent
        imageUri = intent.getParcelableExtra(getString(R.string.imageUriIntent))

        //val previewImageView=findViewById<ImageView>(R.id.previewImageView)
        previewImageView = findViewById(R.id.previewImageView)
        previewImageView.setImageURI(imageUri)

        val submitButton = findViewById<Button>(R.id.submitButton)
        submitButton.setOnClickListener(submitButtonClickListener)
    }
}