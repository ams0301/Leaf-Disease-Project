package com.seedbx.leafdisease

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

class DetailPopupWindow {

    private fun getPopupView(view: View, resource:Int): View {
        val inflater = LayoutInflater.from(view.context)
        return inflater.inflate(resource, null)
    }

    private fun getPopupWindow( popupView: View): PopupWindow {
        val width = ViewGroup.LayoutParams.WRAP_CONTENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        val focusable = true
        return PopupWindow(popupView, width, height, focusable)
    }

    @SuppressLint("ClickableViewAccessibility")
    public fun showDiseasePopupWindow(view: View,plantName:String,diseaseCategory: String,diseaseName:String?){
        val popupView=getPopupView(view,R.layout.popup_detail_disease)
        val popupWindow=getPopupWindow(popupView)

        popupWindow.showAtLocation(view, Gravity.CENTER,0,0)

        val plantNameValue:TextView=popupView.findViewById(R.id.plantNameValue)
        plantNameValue.text=plantName
        val diseaseCategoryValue:TextView=popupView.findViewById(R.id.diseaseCategoryValue)
        diseaseCategoryValue.text=diseaseCategory
        val diseaseNameValue:TextView=popupView.findViewById(R.id.diseaseNameValue)
        diseaseNameValue.text=diseaseName?:"-"

        popupView.setOnTouchListener { _, _ ->
            view.background = ColorDrawable(Color.TRANSPARENT)
            popupWindow.dismiss()
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public fun showHealthyPopupWindow(view:View,plantName: String){
        val popupView=getPopupView(view,R.layout.popup_detail_healthy)
        val popupWindow=getPopupWindow(popupView)

        popupWindow.showAtLocation(view, Gravity.CENTER,0,0)

        val plantNameValue:TextView=popupView.findViewById(R.id.plantNameValue)
        plantNameValue.text=plantName

        popupView.setOnTouchListener { _, _ ->
            view.background = ColorDrawable(Color.TRANSPARENT)
            popupWindow.dismiss()
            true
        }

    }

}