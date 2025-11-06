package com.seedbx.leafdisease

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class GalleryAdapter(
    private val context: Context,
    private val galleryHeaderResource: Int,
    private val galleryItemResource: Int,
    private val imageUriList: Array<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    companion object {
        const val GALLERY_HEADER = 0
        const val GALLERY_ITEM = 1
    }

    private fun connectWithRecyclerView(
        context: Context,
        recyclerView: RecyclerView,
        uriList: Array<Uri>
    ) {
        val galleryAdapter = GalleryItemAdapter(context, R.layout.gallery_image, uriList)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.adapter = galleryAdapter
    }

    private inner class GalleryHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val header: TextView = v.findViewById(R.id.galleryHeaderTextView)
        fun bind(position: Int) {
            //Log.d("GalleryItemDecoration","the text is ${imageUriList[position]}")
            header.text = imageUriList[position] as String
        }
    }

    private inner class GalleryItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val item: RecyclerView = v.findViewById(R.id.galleryItemRecyclerView)
        fun bind(position: Int) {
            connectWithRecyclerView(context, item, imageUriList[position] as Array<Uri>)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == GALLERY_HEADER) {
            val view = inflater.inflate(galleryHeaderResource, parent, false)
            GalleryHeaderViewHolder(view)
        } else {
            val view = inflater.inflate(galleryItemResource, parent, false)
            GalleryItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position % 2 == GALLERY_HEADER) {
            (holder as GalleryHeaderViewHolder).bind(position)
        } else
            (holder as GalleryItemViewHolder).bind(position)
    }

    override fun getItemCount(): Int {
        return imageUriList.size
    }

    override fun getItemViewType(position: Int): Int {
        return position % 2
    }
}

/*class GalleryAdapter(
    private val context: Context,
    private val resource: Int,
    private val imageUriList: Array<Pair<String,Array<Uri>>>
) : RecyclerView.Adapter<GalleryAdapter.GalleryImageViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    class GalleryImageViewHolder(v: View):RecyclerView.ViewHolder(v){
        val titleTextView: TextView =v.findViewById(R.id.titleTextView)
        val galleryRecyclerView: RecyclerView =v.findViewById(R.id.galleryRecyclerView)
    }

    private fun connectWithRecyclerView(context:Context,recyclerView: RecyclerView,uriList:Array<Uri>){
        val galleryAdapter=GalleryItemAdapter(context, R.layout.gallery_image, uriList)
        recyclerView.layoutManager= GridLayoutManager(context,4)
        recyclerView.adapter=galleryAdapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryImageViewHolder {
        val view=inflater.inflate(resource,parent,false)
        return GalleryImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryImageViewHolder, position: Int) {
        val imageUri=imageUriList[position]
        holder.titleTextView.text=imageUri.first
        connectWithRecyclerView(context,holder.galleryRecyclerView, imageUri.second)
    }

    override fun getItemCount(): Int {
        return imageUriList.size
    }

}*/