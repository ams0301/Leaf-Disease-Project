package com.seedbx.leafdisease

import android.view.View

interface StickyHeaderInterface {
    fun getHeaderPosition(position: Int): Int
    fun getHeaderLayout(position: Int): Int
    fun bindHeaderData(header: View, headerPosition: Int)
    fun isHeader(position: Int): Boolean
    //fun getHeader
}