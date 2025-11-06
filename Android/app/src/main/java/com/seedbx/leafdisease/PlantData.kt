package com.seedbx.leafdisease

import com.google.gson.annotations.SerializedName

data class PlantData(
    @SerializedName("plant_name")
    val plantName:String,
    @SerializedName("disease_category")
    val diseaseCategory:String,
    @SerializedName("disease_name")
    val diseaseName:String?,
)