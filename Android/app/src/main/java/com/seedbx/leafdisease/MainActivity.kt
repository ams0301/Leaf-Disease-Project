package com.seedbx.leafdisease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream


/*interface APIService{
    @Multipart
    @POST("image")
    suspend fun sendImage(@Part imageFile: MultipartBody.Part): PlantData
}*/

/*data class PlantData(
    @SerializedName("Plant Name")
    val plantName:String,
    @SerializedName("Infection Type")
    val infectionType:String,
    @SerializedName("Disease Name")
    val diseaseName:String?,
)*/

class MainActivity : AppCompatActivity() {

    private var recentImageUri:Uri?=null

    private lateinit var leafImage:ImageView

    lateinit var service: APIService

    /*private fun checkAndRequestPermissions():Boolean{

    }*/

    private suspend fun sendImageToServer(leafImageFile:File){
        try {
            val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), leafImageFile)
            val multipartBody = MultipartBody.Part.createFormData("image", leafImageFile.name, requestBody)
            val responseBody = service.sendImage(multipartBody)
            Log.d("MainActivity", responseBody.toString())
        }
        catch(e:Exception){
            Log.e("MainActivity","sendImageToServer: $e")
        }
    }

    private fun getImageUri():Uri{
        val file=getFile()
        recentImageUri=Uri.fromFile(file)

        return recentImageUri!!
    }

    private fun getFile(): File {

        val imageName = "Image_"

        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(imageName, ".jpg", storageDir)
    }

    private val captureImageClickListener=registerForActivityResult(ActivityResultContracts.TakePicture()){ success->
        if(success)
            leafImage.setImageURI(recentImageUri)

    }

    private val galleryImageClickListener=registerForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->
        uri?.let {
            recentImageUri=uri
            leafImage.setImageURI(recentImageUri)

        }
    }

    private val submitButtonClickListener= View.OnClickListener {
        if(recentImageUri==null)
            Toast.makeText(this,"No Image Captured",Toast.LENGTH_SHORT).show()
        else {
            val path=recentImageUri!!.path

            var imageFile = File(path)
            if(imageFile.exists())
                runBlocking {sendImageToServer(imageFile)}
            else {

                val inputStream = contentResolver.openInputStream(recentImageUri!!)
                imageFile=getFile()
                val outputStream = FileOutputStream(imageFile)
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream!!.read(buf).also { len = it } > 0) {
                    outputStream.write(buf, 0, len)
                }

                outputStream.close()
                inputStream.close()

                runBlocking { sendImageToServer(imageFile) }
            }
            /*else
                Toast.makeText(this,"Some Error",Toast.LENGTH_SHORT).show()*/
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit=Retrofit.Builder().baseUrl(getString(R.string.url)).addConverterFactory(GsonConverterFactory.create()).build()
        service=retrofit.create(APIService::class.java)

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        leafImage=findViewById(R.id.leafImage)

        val captureButton=findViewById<Button>(R.id.captureBtn)
        captureButton.setOnClickListener{ captureImageClickListener.launch(getImageUri()) }

        val galleryButton=findViewById<Button>(R.id.galleryBtn)
        galleryButton.setOnClickListener{galleryImageClickListener.launch("image/*")}

        val submitButton=findViewById<Button>(R.id.submitBtn)
        submitButton.setOnClickListener(submitButtonClickListener)

        val button=findViewById<Button>(R.id.button)
        button.setOnClickListener{
            val intent= Intent(this,GalleryActivity::class.java)
            startActivity(intent)
        }

        val button2=findViewById<Button>(R.id.testBtn)
        button2.setOnClickListener{
            val intent= Intent(this,CameraActivity::class.java)
            startActivity(intent)
        }
    }
}