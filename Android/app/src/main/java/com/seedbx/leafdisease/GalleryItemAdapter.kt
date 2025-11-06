package com.seedbx.leafdisease

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryItemAdapter(
    private val context: Context,
    private val resource: Int,
    private val imageUriList: Array<Uri>
) : RecyclerView.Adapter<GalleryItemAdapter.GalleryItemImageViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    class GalleryItemImageViewHolder(v: View): RecyclerView.ViewHolder(v){
        val imageView: ImageView =v.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryItemImageViewHolder {
        val view=inflater.inflate(resource,parent,false)
        return GalleryItemImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryItemImageViewHolder, position: Int) {
        val imageUri=imageUriList[position]
        Glide.with(context).load(imageUri).into(holder.imageView)

        /** Handles item click, i.e. it starts SubmitActivity with the given imageUri */
        holder.itemView.setOnClickListener{
            ImageClickListener.onClick(context,imageUri)
        }

    }

    override fun getItemCount(): Int {
        return imageUriList.size
    }

}