package com.seedbx.leafdisease

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GalleryItemDecoration(parent: RecyclerView):RecyclerView.ItemDecoration() {

    /** A [Pair]<[Int],[RecyclerView.ViewHolder]>? used to hold the current header's position and view holder */
    private var currentHeader:Pair<Int,RecyclerView.ViewHolder>?=null

    /*init {
        parent.adapter?.registerAdapterDataObserver(object :RecyclerView.AdapterDataObserver(){
            override fun onChanged() {
                currentHeader=null
            }
        })

        /*parent.doOnEachNextLayout {
            currentHeader=null
        }*/

        parent.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener(){
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return if(e.action==MotionEvent.ACTION_DOWN)
                    e.y<=currentHeader?.second?.itemView?.bottom?:0
                else
                    false
            }
        })
    }

    private inline fun View.doOnEachNextLayout(crossinline action: (view: View) -> Unit) {
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            action(view)
        }
    }*/

    /**
     * Checks whether an item is a header or not
     *
     * @param position A [Int] specifying the position of the item in [GalleryAdapter.imageUriList]
     * @return A [Boolean] which is true if [GalleryAdapter.imageUriList].at([position]) is a header else false
     */
    private fun isHeader(position: Int):Boolean{
        return position%2==GalleryAdapter.GALLERY_HEADER
    }

    /**
     * Returns the position of corresponding header for the item at [GalleryAdapter.imageUriList].at([itemPosition])
     *
     * @param itemPosition A [Int] specifying the position of the item in [GalleryAdapter.imageUriList]
     * @return A [Int] which is the position of corresponding header for the item at [GalleryAdapter.imageUriList].at([itemPosition])
     */
    private fun getHeaderPositionForItem(itemPosition:Int):Int{
        return if(isHeader(itemPosition)) itemPosition else itemPosition-1
    }

    /**
     *
     *
     * @param position A [Int]
     * @param parent A [RecyclerView]
     * @return A [View]?
     */
    private fun getHeaderViewForItem(position:Int,parent: RecyclerView): View? {
        if(parent.adapter==null)
            return null
        val headerPosition=getHeaderPositionForItem(position)
        //Log.d("GalleryItemDecoration","headerPosition is $headerPosition")
        if(currentHeader?.first==headerPosition)
            return currentHeader?.second?.itemView
        val headerHolder=parent.adapter?.createViewHolder(parent,GalleryAdapter.GALLERY_HEADER)
        if(headerHolder!=null){
            parent.adapter?.bindViewHolder(headerHolder,headerPosition)
            currentHeader=headerPosition to headerHolder
        }
        return headerHolder?.itemView
    }

    /**
     *
     *
     * @param parent A [RecyclerView]
     * @param contactPoint A [Int]
     * @return A [View]?
     */
    private fun getChildInContact(parent: RecyclerView,contactPoint:Int):View?{
        var childInContact:View?=null
        for(i in 0 until parent.childCount){
            val child=parent.getChildAt(i)
            val bounds=Rect()
            parent.getDecoratedBoundsWithMargins(child,bounds)
            if(bounds.bottom>contactPoint){
                childInContact=child
                break
            }
        }
        return childInContact
    }

    /**
     *
     *
     * @param c A [Canvas]
     * @param currentHeader A [View]
     * @param nextHeader A [View]
     * @param paddingTop A [Int]
     */
    private fun moveHeader(c:Canvas,currentHeader:View,nextHeader:View,paddingTop:Int){
        //Log.d("GalleryItemDecoration","moveHeader: $paddingTop")
        c.save()
        c.clipRect(0,paddingTop,c.width,paddingTop+currentHeader.height)
        c.translate(0f,(nextHeader.top-currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    /**
     *
     *
     * @param c A [Canvas]
     * @param header A [View]
     * @param paddingTop A [Int]
     */
    private fun drawHeader(c:Canvas,header:View,paddingTop: Int){
        //Log.d("GalleryItemDecoration","drawHeader: $paddingTop")
        c.save()
        c.translate(0f,paddingTop.toFloat())
        header.draw(c)
        c.restore()
        /*val paint= Paint()
        paint.color=Color.RED
        c.drawLine(0f,0f,100f,100f, paint)*/
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        //val topChild=parent.findChildViewUnder(parent.paddingLeft.toFloat(),parent.paddingTop.toFloat())?:return
        /** A [View] */
        val topChild=parent.getChildAt(0)
        /** A [Int] */
        val topChildPosition=parent.getChildAdapterPosition(topChild)
        if(topChildPosition==RecyclerView.NO_POSITION)
            return
        /** A [View] */
        val headerView=getHeaderViewForItem(topChildPosition,parent)?:return
        /** A [Int] */
        val contactPoint=headerView.bottom+headerView.paddingTop
        /** A [View] */
        val childInContact=getChildInContact(parent,contactPoint)?:return
        if(isHeader(parent.getChildAdapterPosition(childInContact))){
            moveHeader(c,headerView,childInContact,parent.paddingTop)
            //Log.d("GalleryItemDecoration","Moving Header")
            return
        }
        drawHeader(c,headerView,parent.paddingTop)
    }
}