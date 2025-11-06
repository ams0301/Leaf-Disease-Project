package com.seedbx.leafdisease

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_submit.*
import kotlinx.android.synthetic.main.camera_view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
class CameraActivity : AppCompatActivity() {

    /** A [Int] specifying the external storage permission code */
    private val EXTERNAL_STORAGE_PERMISSION_CODE = 23

    /** A [ConstraintLayout] */
    //private lateinit var galleryBottomSheet:ConstraintLayout
    /** A [BottomSheetBehavior]<[ConstraintLayout]> */
    //private lateinit var galleryBottomSheetBehaviour:BottomSheetBehavior<ConstraintLayout>
    /** A [ConstraintLayout] object  */
    //private lateinit var galleryPeekView:ConstraintLayout
    /** A [ConstraintLayout] */
    //private lateinit var galleryCollapseView:ConstraintLayout
    /** A [RecyclerView] object used to display recent images in BottomSheet */
    //private lateinit var peekRecyclerView: RecyclerView
    /** A [RecyclerView] object used to display images in expanded BottomSheet */
    //private lateinit var galleryRecyclerView: RecyclerView

    /** A [CameraKitView] */
    private lateinit var camera:CameraKitView

    /** A [CameraViewModel] */
    private val cameraViewModel:CameraViewModel by viewModels()

    /**
     *
     *
     * @param imageUriDateList [Array]<[Pair]<[Uri],[Long]>>
     */
    /*private fun connectWithPeekRecyclerView(imageUriDateList: Array<Pair<Uri,Long>>){
        peekRecyclerView.layoutManager= LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        peekRecyclerView.adapter=GalleryPeekViewAdapter(this,R.layout.gallery_image_bottom_sheet,imageUriDateList)
    }*/

    /**
     *
     *
     * @param imageUriList [Array]<[Pair]<[String],[Array]<[Uri]>>>
     */
    /*private fun connectWithRecyclerView(imageUriList:Array<Pair<String,Array<Uri>>>){
        galleryRecyclerView.layoutManager=LinearLayoutManager(this)
        galleryRecyclerView.adapter=GalleryAdapter(this, R.layout.gallery_item, imageUriList)
    }*/

    /**
     *
     *
     * @param context A [Context]
     * @param activity A [AppCompatActivity]
     * @param imageUriDateList A [MutableList]<[Pair]<[Uri],[Long]>>
     */
    private fun getImages(context: Context,activity: AppCompatActivity,imageUriDateList: MutableList<Pair<Uri, Long>>) {
        if(ActivityCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), EXTERNAL_STORAGE_PERMISSION_CODE)

        /***/
        val columns = arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DATE_TAKEN)

        /***/
        val orderBy = MediaStore.Images.Media.DATE_TAKEN

        /** A  */
        val contentResolver = context.contentResolver

        val source = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        contentResolver.query(
            source, columns, null, null, "$orderBy DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val dateColumn=cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                do {
                    val id = cursor.getLong(idColumn)
                    val date=cursor.getLong(dateColumn)
                    if(date==0L)
                        continue
                    val uri = Uri.withAppendedPath(source, id.toString())
                    imageUriDateList.add(Pair(uri,date))
                } while (cursor.moveToNext())
            }
        }
    }

    /**
     *
     *
     * @param image A [ByteArray]
     * @return A [ByteArray]
     */
    private fun flipImage(image: ByteArray):ByteArray{
        val bmp =BitmapFactory.decodeByteArray(image,0,image.size)
        val matrix=Matrix()
        matrix.preScale(-1.0f,1.0f)
        val bmp2=Bitmap.createBitmap(bmp,0,0,bmp.width,bmp.height,matrix,true)
        val stream=ByteArrayOutputStream()
        bmp2.compress(Bitmap.CompressFormat.JPEG,100,stream)
        return stream.toByteArray()
    }

    /***/
    private val cameraClickListener=View.OnClickListener{
        //Log.d("CameraActivity","Camera Button Clicked")
        //bounceAnimation.start()
        camera.captureImage { _, capturedImage ->
            val imageName="Image_"
            val storageDir=this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile= File.createTempFile(imageName,".jpg",storageDir)
            try{
                var image=capturedImage
                if(camera.facing==CameraKit.FACING_FRONT)
                    image=flipImage(capturedImage)
                val fileOutputStream= FileOutputStream(imageFile.path)
                fileOutputStream.write(image)
                fileOutputStream.close()
                val imageUri=Uri.fromFile(imageFile)
                ImageClickListener.onClick(this,imageUri)
            }
            catch (e:Error){
                Toast.makeText(this,"Some Error Occurred",Toast.LENGTH_SHORT).show()
            }
        }
    }

    /***/
    private val cameraSwitchClickListener=View.OnClickListener{
        camera.toggleFacing()
    }

    /***/
    private val flashClickListener=View.OnClickListener { v->
        val imageView=v as ImageView
        Log.d("CameraActivity","flash is ${camera.flash}")
        if(camera.flash== CameraKit.FLASH_OFF){
            camera.flash=CameraKit.FLASH_ON
            //imageView.background=ContextCompat.getDrawable(this,R.drawable.ic_flash)
            imageView.background=ContextCompat.getDrawable(this,R.drawable.ic_flash_on_ripple)
        }
        else{
            camera.flash=CameraKit.FLASH_OFF
            imageView.background=ContextCompat.getDrawable(this,R.drawable.ic_flash_off_ripple)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        //galleryBottomSheet=findViewById(R.id.galleryBottomSheet)
        //galleryBottomSheetBehaviour=BottomSheetBehavior.from(galleryBottomSheet)
        //peekRecyclerView=findViewById(R.id.peekRecyclerView)
        //galleryRecyclerView=findViewById(R.id.galleryItemRecyclerView)
        //galleryPeekView=findViewById(R.id.peekView)
        //galleryCollapseView=findViewById(R.id.collapseView)

        camera=findViewById(R.id.camera)
        camera.flash= CameraKit.FLASH_OFF

        if(cameraViewModel.imageUriDateList.isEmpty()) {
            getImages(this, this, cameraViewModel.imageUriDateList)
            cameraViewModel.sortImageUri(cameraViewModel.imageUriDateList.toTypedArray())
        }

        getImages(this,this,cameraViewModel.imageUriDateList)
        //connectWithPeekRecyclerView(cameraViewModel.imageUriDateList.toTypedArray())
        //connectWithRecyclerView(cameraViewModel.imageUriList.toTypedArray())

        /*galleryBottomSheetBehaviour.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                //Log.d("CameraActivity",newState.toString())
                when(newState){
                    BottomSheetBehavior.STATE_HIDDEN->{}
                    BottomSheetBehavior.STATE_EXPANDED->{
                        galleryRecyclerView.suppressLayout(false)
                        galleryBottomSheetBehaviour.isDraggable=false
                    }
                    BottomSheetBehavior.STATE_COLLAPSED->{
                        galleryBottomSheetBehaviour.isDraggable=true
                    }
                    BottomSheetBehavior.STATE_DRAGGING->{
                        galleryRecyclerView.suppressLayout(true)
                    }
                    BottomSheetBehavior.STATE_SETTLING->{}
                    else->{}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                galleryPeekView.alpha=1.0f-slideOffset
                galleryCollapseView.alpha=slideOffset
            }

        })*/

        /*galleryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!galleryRecyclerView.canScrollVertically(-1)) {
                    if (!galleryBottomSheetBehaviour.isDraggable) {
                        galleryBottomSheetBehaviour.isDraggable = true
                    }
                }
                else {
                    if (galleryBottomSheetBehaviour.isDraggable) {
                        galleryBottomSheetBehaviour.isDraggable = false
                    }
                }
            }
        })*/

        /*peekRecyclerView.setOnTouchListener(object: View.OnTouchListener(){
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                v?.parent.requestDisallowInterceptTouchEvent(true)
                v?.onTouchEvent(event)
                return true
            }
        })*/

        val cameraImageView: ImageView =findViewById(R.id.cameraImageView)
        //cameraImageView.bringToFront()
        cameraImageView.setOnClickListener(cameraClickListener)

        val cameraSwitchImageView:ImageView=findViewById(R.id.cameraSwitchImageView)
        cameraSwitchImageView.setOnClickListener(cameraSwitchClickListener)

        val flashImageView:ImageView=findViewById(R.id.flashImageView)
        flashImageView.setOnClickListener(flashClickListener)

        /*cameraImageView.setOnTouchListener { v, event ->
            val view=v as ImageView
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    view.drawable.colorFilter=LightingColorFilter(0x77000000, Color.BLACK)
                    view.invalidate()
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
                    view.drawable.clearColorFilter()
                    view.invalidate()
                }
            }
            false
        }*/

    }

    /*override fun onBackPressed() {
        if(galleryBottomSheetBehaviour.state!=BottomSheetBehavior.STATE_COLLAPSED)
            galleryBottomSheetBehaviour.state=BottomSheetBehavior.STATE_COLLAPSED
        else
            super.onBackPressed()
    }*/

    override fun onStart() {
        super.onStart()
        camera.onStart()
    }

    override fun onResume() {
        super.onResume()
        camera.onResume()
    }

    override fun onPause() {
        super.onPause()
        camera.onPause()
    }

    override fun onStop() {
        super.onStop()
        camera.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.onRequestPermissionsResult(requestCode,permissions, grantResults)
    }
}
*/


class CameraActivity : AppCompatActivity() {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        /** A [Int] specifying the external storage permission code */
        private const val EXTERNAL_STORAGE_PERMISSION_CODE = 23
    }

    /** A [ConstraintLayout] */
    //private lateinit var galleryBottomSheet:ConstraintLayout
    /** A [BottomSheetBehavior]<[ConstraintLayout]> */
    //private lateinit var galleryBottomSheetBehaviour:BottomSheetBehavior<ConstraintLayout>
    /** A [ConstraintLayout] object  */
    //private lateinit var galleryPeekView:ConstraintLayout
    /** A [ConstraintLayout] */
    //private lateinit var galleryCollapseView:ConstraintLayout
    /** A [RecyclerView] object used to display recent images in BottomSheet */
    //private lateinit var peekRecyclerView: RecyclerView
    /** A [RecyclerView] object used to display images in expanded BottomSheet */
    //private lateinit var galleryRecyclerView: RecyclerView

    /** A [CameraViewModel] */
    private val cameraViewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraController:CameraController
    private lateinit var previewView:PreviewView


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     *
     *
     */
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir
        else
            filesDir
    }

    /**
     *
     *
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /***/
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraActivity", "Image Capture Successful, saved at $savedUri")

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "Unable to Capture Image")
                }
            })
    }

    /**
     *
     *
     * @param imageUriDateList [Array]<[Pair]<[Uri],[Long]>>
     */
    /*private fun connectWithPeekRecyclerView(imageUriDateList: Array<Pair<Uri,Long>>){
        peekRecyclerView.layoutManager= LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        peekRecyclerView.adapter=GalleryPeekViewAdapter(this,R.layout.gallery_image_bottom_sheet,imageUriDateList)
    }*/

    /**
     *
     *
     * @param imageUriList [Array]<[Pair]<[String],[Array]<[Uri]>>>
     */
    /*private fun connectWithRecyclerView(imageUriList:Array<Pair<String,Array<Uri>>>){
        galleryRecyclerView.layoutManager=LinearLayoutManager(this)
        galleryRecyclerView.adapter=GalleryAdapter(this, R.layout.gallery_item, imageUriList)
    }*/

    /**
     *
     *
     * @param context A [Context]
     * @param activity A [AppCompatActivity]
     * @param imageUriDateList A [MutableList]<[Pair]<[Uri],[Long]>>
     */
    private fun getImages(
        context: Context,
        activity: AppCompatActivity,
        imageUriDateList: MutableList<Pair<Uri, Long>>
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                EXTERNAL_STORAGE_PERMISSION_CODE
            )

        /***/
        val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)

        /***/
        val orderBy = MediaStore.Images.Media.DATE_TAKEN

        /** A  */
        val contentResolver = context.contentResolver

        val source = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        contentResolver.query(
            source, columns, null, null, "$orderBy DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                do {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getLong(dateColumn)
                    if (date == 0L)
                        continue
                    val uri = Uri.withAppendedPath(source, id.toString())
                    imageUriDateList.add(Pair(uri, date))
                } while (cursor.moveToNext())
            }
        }
    }

    /**
     *
     *
     * @param image A [ByteArray]
     * @return A [ByteArray]
     */
    private fun flipImage(image: ByteArray): ByteArray {
        val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
        val matrix = Matrix()
        matrix.preScale(-1.0f, 1.0f)
        val bmp2 = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        val stream = ByteArrayOutputStream()
        bmp2.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private val cameraClickListener = View.OnClickListener {
        takePhoto()
    }

    private val cameraSwitchClickListener = View.OnClickListener {}

    private val flashClickListener = View.OnClickListener { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        //galleryBottomSheet=findViewById(R.id.galleryBottomSheet)
        //galleryBottomSheetBehaviour=BottomSheetBehavior.from(galleryBottomSheet)
        //peekRecyclerView=findViewById(R.id.peekRecyclerView)
        //galleryRecyclerView=findViewById(R.id.galleryItemRecyclerView)
        //galleryPeekView=findViewById(R.id.peekView)
        //galleryCollapseView=findViewById(R.id.collapseView)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        previewView=findViewById(R.id.previewView)
        //cameraController=

        if (cameraViewModel.imageUriDateList.isEmpty()) {
            getImages(this, this, cameraViewModel.imageUriDateList)
            cameraViewModel.sortImageUri(cameraViewModel.imageUriDateList.toTypedArray())
        }

        getImages(this, this, cameraViewModel.imageUriDateList)
        //connectWithPeekRecyclerView(cameraViewModel.imageUriDateList.toTypedArray())
        //connectWithRecyclerView(cameraViewModel.imageUriList.toTypedArray())

        /*galleryBottomSheetBehaviour.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                //Log.d("CameraActivity",newState.toString())
                when(newState){
                    BottomSheetBehavior.STATE_HIDDEN->{}
                    BottomSheetBehavior.STATE_EXPANDED->{
                        galleryRecyclerView.suppressLayout(false)
                        galleryBottomSheetBehaviour.isDraggable=false
                    }
                    BottomSheetBehavior.STATE_COLLAPSED->{
                        galleryBottomSheetBehaviour.isDraggable=true
                    }
                    BottomSheetBehavior.STATE_DRAGGING->{
                        galleryRecyclerView.suppressLayout(true)
                    }
                    BottomSheetBehavior.STATE_SETTLING->{}
                    else->{}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                galleryPeekView.alpha=1.0f-slideOffset
                galleryCollapseView.alpha=slideOffset
            }

        })*/

        /*galleryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!galleryRecyclerView.canScrollVertically(-1)) {
                    if (!galleryBottomSheetBehaviour.isDraggable) {
                        galleryBottomSheetBehaviour.isDraggable = true
                    }
                }
                else {
                    if (galleryBottomSheetBehaviour.isDraggable) {
                        galleryBottomSheetBehaviour.isDraggable = false
                    }
                }
            }
        })*/

        /*peekRecyclerView.setOnTouchListener(object: View.OnTouchListener(){
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                v?.parent.requestDisallowInterceptTouchEvent(true)
                v?.onTouchEvent(event)
                return true
            }
        })*/

        val cameraImageView: ImageView = findViewById(R.id.cameraImageView)
        cameraImageView.setOnClickListener(cameraClickListener)

        val cameraSwitchImageView: ImageView = findViewById(R.id.cameraSwitchImageView)
        cameraSwitchImageView.setOnClickListener(cameraSwitchClickListener)

        val flashImageView: ImageView = findViewById(R.id.flashImageView)
        flashImageView.setOnClickListener(flashClickListener)

    }

    /*override fun onBackPressed() {
        if(galleryBottomSheetBehaviour.state!=BottomSheetBehavior.STATE_COLLAPSED)
            galleryBottomSheetBehaviour.state=BottomSheetBehavior.STATE_COLLAPSED
        else
            super.onBackPressed()
    }*/

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted())
                startCamera()
            else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
