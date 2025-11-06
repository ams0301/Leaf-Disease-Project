package com.seedbx.leafdisease

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {

    private val galleryViewModel:GalleryViewModel by viewModels()

    private val EXTERNAL_STORAGE_PERMISSION_CODE = 23
    private lateinit var recyclerView: RecyclerView

    private fun getImages(context: Context,activity: AppCompatActivity,imageUriDateList: MutableList<Pair<Uri, Long>>) {
        if(ActivityCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), EXTERNAL_STORAGE_PERMISSION_CODE)

        val columns = arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DATE_TAKEN)

        val orderBy = MediaStore.Images.Media.DATE_TAKEN

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

    private fun connectWithRecyclerView(imageUriList:Array<Any>){
        val galleryAdapter=GalleryAdapter(this, R.layout.gallery_header, R.layout.gallery_item, imageUriList)
        recyclerView=findViewById(R.id.galleryItemRecyclerView)
        recyclerView.layoutManager=LinearLayoutManager(this)
        recyclerView.adapter=galleryAdapter
        val galleryItemDecoration=GalleryItemDecoration(recyclerView)
        recyclerView.addItemDecoration(galleryItemDecoration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        if(galleryViewModel.imageUriDateList.isEmpty()) {
            getImages(this, this, galleryViewModel.imageUriDateList)
            galleryViewModel.sortImageUri(galleryViewModel.imageUriDateList.toTypedArray())
        }

        connectWithRecyclerView(galleryViewModel.imageUriList.toTypedArray())
    }
}