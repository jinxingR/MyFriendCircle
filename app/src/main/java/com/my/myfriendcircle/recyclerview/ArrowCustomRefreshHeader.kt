package com.my.myfriendcircle.recyclerview

import android.content.Context
import android.util.AttributeSet
import com.github.jdsjlzx.view.ArrowRefreshHeader

/**
 * @ClassName ArrowCustomRefreshHeader
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/14 14:51
 * @Version 1.0
 **/
class ArrowCustomRefreshHeader : ArrowRefreshHeader, ICircleRefreshHeader {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun onMove(offSet: Float, sumOffSet: Float, isTouchEvent: Boolean) {
        onMove(offSet, sumOffSet)
    }

    override fun isNeedOverScrollBy(isDown: Boolean?): Boolean {
        return false
    }

    override fun onUp(isMove: Boolean): Boolean? {
        return null
    }

    override fun setIsMoveAllowCover(isMove: Boolean): ICircleRefreshHeader {
        return this
    }

    override fun getCoverImageMinHeight(): Int {
        return 0
    }

    override fun getHeaderViewHeight(): Int {
        return 0
    }

    override fun getCoverImageMaxHeight(): Int {
        return 0
    }

    override fun getCoverViewHeight(): Int {
        return 0
    }

    override fun isMaxCover(): Boolean {
        return false
    }
}