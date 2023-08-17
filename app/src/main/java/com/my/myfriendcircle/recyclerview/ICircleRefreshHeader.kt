package com.my.myfriendcircle.recyclerview

import com.github.jdsjlzx.interfaces.IRefreshHeader

/**
 * @ClassName ICircleRefreshHeader
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/12 14:20
 * @Version 1.0
 **/
interface ICircleRefreshHeader: IRefreshHeader {

    fun onMove(offSet: Float, sumOffSet: Float,isTouchEvent: Boolean)

    /**
     * 是否需要继续过度滚动效果
     * @param isDown Boolean true是下滑，false上滑 ,null 则无法判断方向
     * @return Boolean
     */
    fun isNeedOverScrollBy(isDown: Boolean?):Boolean


    /**
     * up事件回调
     */
    fun onUp(isMove:Boolean):Boolean?

    fun setIsMoveAllowCover(isMove: Boolean): ICircleRefreshHeader


    fun getCoverImageMinHeight(): Int

    fun getHeaderViewHeight():Int

    fun getCoverImageMaxHeight():Int

    fun getCoverViewHeight():Int

    fun isMaxCover():Boolean

}