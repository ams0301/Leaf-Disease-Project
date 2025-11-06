package com.seedbx.leafdisease

import android.net.Uri
import androidx.lifecycle.ViewModel
import java.util.*

class CameraViewModel:ViewModel() {

    /**
     * Returns the index of the largest element in list after list.at(startIndex) which is less than or equal to value
     *
     * @param list A [Array]<[Pair]<[Uri],[Long]>> containing (uri, time of capture of images) sorted in descending order by time
     * @param startIndex A [Int] specifying the index after which to start searching
     * @param value A [Long] specifying the time limit
     * @return A [Int] which is the index
     */
    private fun getLast(list:Array<Pair<Uri,Long>>, startIndex:Int, value:Long):Int{
        if(list[startIndex].second<value)
            return startIndex-1
        var left=startIndex
        var right=list.size-1
        while(left<=right){
            val mid=(left+right)/2
            if(list[mid].second==value){
                return mid
            }
            if(list[mid].second>value){
                if(mid==list.size-1)
                    return mid
                if(list[mid+1].second<value)
                    return mid
                left=mid+1
            }
            else
                right=mid-1
        }
        return -1
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getThisWeek(calendar: Calendar):Long{
        calendar.set(Calendar.HOUR_OF_DAY,0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)

        calendar.set(Calendar.DAY_OF_WEEK,calendar.firstDayOfWeek)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getLastWeek(calendar: Calendar):Long{
        calendar.add(Calendar.WEEK_OF_YEAR,-1)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getCurrentMonth(calendar: Calendar):Long{
        calendar.set(Calendar.DAY_OF_MONTH,1)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getCurrentYear(calendar: Calendar):Long{
        calendar.set(Calendar.DAY_OF_YEAR,1)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getPreviousMonth(calendar: Calendar):Long{
        calendar.add(Calendar.MONTH,-1)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param calendar A [Calendar]
     * @return A [Long]
     */
    private fun getPreviousYear(calendar: Calendar):Long{
        calendar.add(Calendar.YEAR,-1)
        return calendar.timeInMillis
    }

    /**
     *
     *
     * @param label A [String]
     * @param uriList A [Array]<[Uri]>
     */
    private fun addToImageUriList(label:String,uriList:Array<Uri>){
        if(uriList.isEmpty())
            return
        imageUriList.add(Pair(label,uriList))
    }

    /**
     *
     *
     * @param imageUriDateList A [Array]<[Pair]<[Uri],[Long]>>
     * @param start A [Int]
     * @param calendar A [Calendar]
     */
    private fun addMonths(imageUriDateList: Array<Pair<Uri,Long>>, start: Int, calendar: Calendar):Int{
        var startIdx=start
        var endIdx: Int
        var ctr=0
        var time=getCurrentMonth(calendar)
        while(ctr<12){
            val currentMonth=month[calendar.get(Calendar.MONTH)]
            endIdx=getLast(imageUriDateList,startIdx,time)
            val monthList= mutableListOf<Uri>()
            for(i in startIdx..endIdx)
                monthList.add(imageUriDateList[i].first)
            addToImageUriList(currentMonth,monthList.toTypedArray())
            startIdx=endIdx+1
            if(startIdx==imageUriDateList.size)
                return startIdx
            time=getPreviousMonth(calendar)
            ctr++
        }
        calendar.add(Calendar.MONTH,1)
        return startIdx
    }


    /**
     *
     *
     * @param imageUriDateList A [Array]<[Pair]<[Uri],[Long]>>
     * @param start A [Int]
     * @param calendar A [Calendar]
     */
    private fun addYears(imageUriDateList: Array<Pair<Uri,Long>>, start: Int, calendar: Calendar){

        var startIdx=start
        var endIdx:Int
        var time=getCurrentYear(calendar)
        while(startIdx<imageUriDateList.size){
            val currentYear=calendar.get(Calendar.YEAR)
            endIdx=getLast(imageUriDateList,startIdx,time)

            val yearList= mutableListOf<Uri>()
            for(i in startIdx..endIdx)
                yearList.add(imageUriDateList[i].first)
            addToImageUriList(currentYear.toString(),yearList.toTypedArray())

            startIdx=endIdx+1
            time=getPreviousYear(calendar)
        }
    }


    /**
     *
     *
     * @param imageUriDateList A [Array]<[Pair]<[Uri],[Long]>>
     */
    fun sortImageUri(imageUriDateList: Array<Pair<Uri,Long>>){

        val thisWeek=getThisWeek(calendar)
        var startIdx=0
        var endIdx=getLast(imageUriDateList,startIdx,thisWeek)

        val recentWeek= mutableListOf<Uri>()
        for(i in startIdx..endIdx)
            recentWeek.add(imageUriDateList[i].first)
        addToImageUriList("RECENT",recentWeek.toTypedArray())

        startIdx=endIdx+1
        if(startIdx==imageUriDateList.size)
            return

        val lastWeek=getLastWeek(calendar)
        endIdx=getLast(imageUriDateList,startIdx,lastWeek)
        val previousWeek= mutableListOf<Uri>()
        for(i in startIdx..endIdx)
            previousWeek.add(imageUriDateList[i].first)
        addToImageUriList("LAST WEEK",previousWeek.toTypedArray())

        startIdx=endIdx+1
        if(startIdx==imageUriDateList.size)
            return

        startIdx=addMonths(imageUriDateList,startIdx,calendar)

        addYears(imageUriDateList,startIdx,calendar)
    }

    /** A [List]<[String]> used to map index to month name */
    private val month=listOf("JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER")

    /** A [Calendar] object  */
    private val calendar= Calendar.getInstance()

    /** A [MutableList]<[Pair]<[String],[Array]<[Uri]>>> */
    val imageUriList= mutableListOf<Pair<String,Array<Uri>>>()

    /** A [MutableList]<[Pair]<[Uri],[Long]>> */
    val imageUriDateList= mutableListOf<Pair<Uri,Long>>()

}