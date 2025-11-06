package com.seedbx.leafdisease

import android.content.Context
import android.content.Intent
import android.net.Uri

class ImageClickListener {

    companion object {
        private fun startNewActivity(context: Context, uri: Uri) {
            val intent = Intent(context, SubmitActivity::class.java)
            intent.putExtra(context.getString(R.string.imageUriIntent), uri)
            context.startActivity(intent)
        }

        fun onClick(context: Context, uri: Uri) {
            startNewActivity(context, uri)
        }
    }
}