package com.seedbx.leafdisease

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryPeekViewAdapter(
    private val context: Context,
    private val resource: Int,
    private val imageUriList: Array<Pair<Uri,Long>>
) : RecyclerView.Adapter<GalleryPeekViewAdapter.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    class ViewHolder(v: View): RecyclerView.ViewHolder(v){
        val imageView: ImageView =v.findViewById(R.id.bottomSheetImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v=inflater.inflate(resource,parent,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUri=imageUriList[position].first
        Glide.with(context).load(imageUri).into(holder.imageView)

        /** Handles item click, i.e. it starts SubmitActivity with the given imageUri */
        holder.itemView.setOnClickListener{
            ImageClickListener.onClick(context,imageUri)
        }

    }

    override fun getItemCount(): Int {
        return imageUriList.count()
    }

}