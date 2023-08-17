package com.my.myfriendcircle.recyclerview

import android.animation.ValueAnimator
import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.jdsjlzx.interfaces.IRefreshHeader
import com.github.jdsjlzx.util.WeakHandler
import com.my.myfriendcircle.databinding.LyCoverHeaderBinding
import com.my.myfriendcircle.dp

/**
 * @ClassName MomentsRefreshHeader
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/11 10:11
 * @Version 1.0
 **/
class CircleRefreshHeader : FrameLayout, ICircleRefreshHeader {

    companion object{
        const val TAG = "CircleRefreshHeader"
    }

    private lateinit var binding: LyCoverHeaderBinding

    private val mHandler = WeakHandler()

    //<editor-fold desc="变量">
    /**
     * 头部item高度
     */
    private var mHeaderItemViewHeight = 0

    /**
     * 封面图片的最小高度
     */
    private var mCoverImageMinHeight: Int = 0

    /**
     * 封面图片的最大高度
     */
    private var mCoverImageMaxHeight: Int = 0

    /**
     * 刷新图片上移的最大距离
     */
    private var mRefreshHideTranslationY = 0f

    /**
     * 刷新图片下拉的最大移动距离
     */
    private var mRefreshShowTranslationY = 0f

    /**
     * 旋转角度
     */
    private var mRotateAngle = 0f

    /**
     * 触发临界点
     */
    private var triggerCriticalPoint = 0

    private var mState = IRefreshHeader.STATE_NORMAL

    /**
     * 是否允许封面放大
     */
    var isMoveAllowCover = false
        private set

    /**
     * 是否放大封面
     */
    private var isMaxCoverHeight = false

    //</editor-fold>


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, 0)
        this.layoutParams = lp
        this.setPadding(0, 0, 0, 0)
        binding = LyCoverHeaderBinding.inflate(LayoutInflater.from(context), this, true)
        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mHeaderItemViewHeight = measuredHeight
        mRefreshHideTranslationY = (0 - binding.ivRefresh.measuredHeight - 20).toFloat()
        setRefreshTranslationY(binding.ivRefresh.measuredHeight)
        binding.ivRefresh.visibility = GONE
        binding.ivCover.setOnClickListener {
            mMomentsRefreshHeaderBack?.onClick(MomentsRefreshHeaderBack.CLICK_HEAD)
        }
    }

    //<editor-fold desc="初始化与继承">


    fun toRequestLayout() {
        val lp: LayoutParams = binding.ivCover.layoutParams as LayoutParams
        lp.height = mCoverImageMinHeight
        lp.topMargin = if (isMoveAllowCover) 0 else (-20f).dp
        binding.ivCover.layoutParams = lp
        requestLayout()
        invalidate()
        mHeaderItemViewHeight = lp.height + lp.bottomMargin
    }

    fun setRefreshTranslationY(translation: Int): CircleRefreshHeader {
        mRefreshShowTranslationY = translation.toFloat()
        return this
    }

    fun setCoverImageHeight(minHeight: Int, maxHeight: Int): CircleRefreshHeader {
        this.mCoverImageMinHeight = minHeight
        this.mCoverImageMaxHeight = maxHeight
        return this
    }

    fun setTriggerCriticalPoint(criticalPoint: Int): CircleRefreshHeader {
        this.triggerCriticalPoint = criticalPoint
        return this
    }

    override fun setIsMoveAllowCover(isMove: Boolean): CircleRefreshHeader {
        this.isMoveAllowCover = isMove
        return this
    }

    override fun isMaxCover(): Boolean {
        return isMaxCoverHeight
    }

    override fun getHeaderView(): View? {
        return this
    }

    override fun getVisibleHeight(): Int {
        return height - mHeaderItemViewHeight
    }

    override fun getHeaderViewHeight(): Int {
        return layoutParams.height
    }

    override fun getCoverViewHeight(): Int {
        return binding.ivCover.layoutParams.height
    }

    fun isCoverToMax(): Boolean {
        return getCoverViewHeight() >= mCoverImageMaxHeight
    }

    //垂直滑动时该方法不实现
    override fun getVisibleWidth(): Int {
        return 0
    }

    override fun getType(): Int {
        return IRefreshHeader.TYPE_HEADER_NORMAL
    }

    override fun onReset() {
        setState(IRefreshHeader.STATE_NORMAL)
    }

    override fun onPrepare() {
        setState(IRefreshHeader.STATE_RELEASE_TO_REFRESH)
    }

    override fun onRefreshing() {
        setState(IRefreshHeader.STATE_REFRESHING)
    }

    override fun getCoverImageMinHeight(): Int {
        return mCoverImageMinHeight - 20f.dp
    }

    override fun getCoverImageMaxHeight(): Int {
        return mCoverImageMaxHeight
    }
    //</editor-fold>

    //<editor-fold desc="滚动业务">

    override fun onRelease(): Boolean {
        var isOnRefresh = false
        val currentHeight: Int =
            layoutParams.height // 使用 mHeaderView.getLayoutParams().height 可以防止快速快速下拉的时候图片不回弹
        if (currentHeight > mHeaderItemViewHeight) {
            if (currentHeight > triggerCriticalPoint && mState < IRefreshHeader.STATE_REFRESHING) {
                setState(IRefreshHeader.STATE_REFRESHING)
                isOnRefresh = true
            }
            headerRest()
        }
        if (!isOnRefresh) {
            hideRefresh()
        }
        return isOnRefresh
    }


    override fun onMove(offSet: Float, sumOffSet: Float, isTouchEvent: Boolean) {
        val top: Int = top // 相对父容器recyclerview的顶部位置 负数表示向上划出父容器的距离
        val currentHeaderHeight = getHeaderViewHeight()
        val currentCoverHeight = getCoverViewHeight()
        var targetCoverHeight = if (!isTouchEvent && currentCoverHeight < 0) {
            0 - offSet.toInt()
        } else {
            currentCoverHeight - offSet.toInt()
        }

        var targetHeaderHeight = if (!isTouchEvent && currentHeaderHeight < 0) {
            0 - offSet.toInt()
        } else {
            currentHeaderHeight - offSet.toInt()
        }

        if (offSet < 0 && top == 0) {
            //第二个参数：针对非触摸事件，封面一下子放缩造成的缺陷弥补
            setHeaderViewHeight(
                targetHeaderHeight,
                if (isTouchEvent) targetHeaderHeight else targetCoverHeight,
                isTouchEvent,
                offSet < 0
            )
            if (isTouchEvent) {
                refreshTranslation(currentHeaderHeight, offSet)
            }

        } else if ((offSet > 0 && currentHeaderHeight > mHeaderItemViewHeight) || !isTouchEvent) {
            layout(left, 0, right, targetHeaderHeight) //重新布局让header显示在顶端，直到不再缩小图片
            setHeaderViewHeight(
                targetHeaderHeight,
                if (isTouchEvent) targetHeaderHeight else targetCoverHeight,
                isTouchEvent,
                offSet < 0
            )
            if (isTouchEvent) {
                refreshTranslation(currentHeaderHeight, offSet)
            }
            //forceStopRecyclerViewScroll((RecyclerView) getParent());// 停止recyclerview的滑动，防止快速上划松手后动画产生抖动
        }
    }

    override fun onMove(offSet: Float, sumOffSet: Float) {
        onMove(offSet, sumOffSet, true)
    }

    override fun isNeedOverScrollBy(isDown: Boolean?): Boolean {
        if (isDown == null) return false
        val coverHeight = getCoverViewHeight()
        return if (isDown) {
            coverHeight < mCoverImageMaxHeight
        } else {
            coverHeight > mCoverImageMinHeight
        }
    }

    override fun onUp(isMove: Boolean): Boolean? {
        var currentIsMaxCover = isMaxCover()
        val coverHeight = getCoverViewHeight()
        val point =
            if (currentIsMaxCover) (mCoverImageMaxHeight + triggerCriticalPoint) / 2 else triggerCriticalPoint
        var isOverPoint = coverHeight >= point
        var start = coverHeight
        var end = if (isOverPoint) {
            mCoverImageMaxHeight
        } else {
            mCoverImageMinHeight
        }
        val newMaxCover =
            if (start == end && !isMoveAllowCover) !currentIsMaxCover else if (currentIsMaxCover) coverHeight <= point else coverHeight >= point
        headerToAnimator(start, end)
        if (!isMoveAllowCover) {
            return newMaxCover
        }
        return null
    }

    //强制停止RecyclerView滑动方法
    fun forceStopRecyclerViewScroll(recyclerView: RecyclerView) {
        recyclerView.dispatchTouchEvent(
            MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_CANCEL,
                0f,
                0f,
                0
            )
        )
    }


    //</editor-fold>

    //<editor-fold desc="刷新控件业务">

    override fun refreshComplete() {
        mHandler.postDelayed({ setState(IRefreshHeader.STATE_DONE) }, 200)
    }

    /**
     * refreshView在刷新区间内相对位移并跟随位移速度旋转
     */
    private fun refreshTranslation(currentHeight: Int, offSet: Float) {
        binding.ivRefresh.apply {
            if ((currentHeight - mHeaderItemViewHeight) / 2 < mRefreshShowTranslationY - mRefreshHideTranslationY) { // 判断是否在非刷新区间
                val startTranslationY =
                    if (translationY == mRefreshHideTranslationY) height / 2 else 0
                //布局高度增加offset 相当于距离上边距offSet * 2
                var newTranslationY: Float = startTranslationY + translationY - offSet * 2
                if (newTranslationY > mRefreshShowTranslationY) {
                    newTranslationY = mRefreshShowTranslationY
                } else if (newTranslationY < mRefreshHideTranslationY) {
                    newTranslationY = mRefreshHideTranslationY
                }
                if (Math.abs(newTranslationY) != translationY) {
                    translationY = newTranslationY
                }
            }
        }
        //旋转，角度大小跟随偏移量
        mRotateAngle -= offSet
        binding.ivRefresh.rotation = mRotateAngle
    }

    private fun showRefresh() {
        binding.ivRefresh.apply {
            if (translationY == mRefreshShowTranslationY) {
                return@apply
            }
            toValueAnimatorOfTranslationY(this, translationY, mRefreshShowTranslationY, 50)
        }
    }

    fun reset() {
        hideRefresh()
        mHandler.postDelayed({
            setState(IRefreshHeader.STATE_NORMAL)
        }, 500)
    }

    private fun refreshing() {
        mHandler.postDelayed(object : Runnable {
            override fun run() {
                if (mState == IRefreshHeader.STATE_REFRESHING) {
                    binding.ivRefresh.rotation = 8.let { mRotateAngle += it; mRotateAngle }
                    mHandler.post(this)
                }
            }
        }, 50)
    }

    private fun hideRefresh() {
        binding.ivRefresh.apply {
            toValueAnimatorOfTranslationY(this, translationY, mRefreshHideTranslationY)
        }
    }

    private fun toValueAnimatorOfTranslationY(
        view: View,
        start: Float,
        end: Float,
        duration: Long = 300,
        startDelay: Long = 60
    ) {
        val animator = ValueAnimator.ofFloat(start, end)
        animator.startDelay = startDelay
        animator.setDuration(duration).start()
        animator.addUpdateListener { animation ->
            if (view.translationY == end) {
                animation.cancel()
            } else {
                view.translationY = (animation.animatedValue as Float)
            }
        }
    }


    //</editor-fold>

    //<editor-fold desc="封面的业务">
    private fun setHeaderViewHeight(
        dHeight: Int,
        cHeight: Int,
        isTouchEvent: Boolean = true,
        isDown: Boolean = false
    ) {
        var height = dHeight
        if (height < mHeaderItemViewHeight) {
            height = mHeaderItemViewHeight
        }
        if (isMoveAllowCover || !isTouchEvent || isMaxCoverHeight) {
            var coverHeight = cHeight
            if (isDown && isCoverToMax()) {//规避下滑已经最大值了，但滚动过程传递小于最大值的小偏差的情况
                return
            }
            if (coverHeight <= mCoverImageMinHeight || height == mHeaderItemViewHeight) {
                coverHeight = mCoverImageMinHeight
                isMaxCoverHeight = false
            }
            if (coverHeight >= mCoverImageMaxHeight) {
                coverHeight = mCoverImageMaxHeight
                isMaxCoverHeight = true
                val lp: LayoutParams = binding.ivCover.layoutParams as LayoutParams
                if (coverHeight == mCoverImageMaxHeight && height < mCoverImageMaxHeight + lp.bottomMargin) { //防止封面已经是最大值了，但header没有的高度没有跟上
                    height = mCoverImageMaxHeight + lp.bottomMargin
                }
            }
            updateViewHeight(binding.ivCover, coverHeight)
        }
        updateViewHeight(this, height)
    }

    /**
     * 更新view的高度
     * @param view View
     * @param height Int
     */
    private fun updateViewHeight(view: View, height: Int) {
        view.layoutParams.height = height
        view.requestLayout()
    }

    /**
     * 重置恢复头部逻辑
     */
    private fun headerRest() {
        headerToAnimator(layoutParams.height, mHeaderItemViewHeight)
    }

    private fun headerToAnimator(start: Int, end: Int, duration: Long = 300L) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.setDuration(duration).start()
        animator.addUpdateListener { animation ->
            if (layoutParams.height == mHeaderItemViewHeight) { // 停止动画，防止快速上划松手后动画产生抖动
                animation.cancel()
            } else {
                setHeaderViewHeight(
                    animation.animatedValue as Int,
                    animation.animatedValue as Int
                )
            }
        }
    }


    //</editor-fold>

    fun setState(state: Int) {
        if (state == mState) return
        if (state == IRefreshHeader.STATE_REFRESHING) {  // 显示进度
            binding.ivRefresh.apply {
                if (visibility != VISIBLE) {
                    visibility = VISIBLE
                }
                showRefresh()
            }
            refreshing()
        } else if (state == IRefreshHeader.STATE_DONE) {
            reset()
        }
        mState = state
    }

    private var mMomentsRefreshHeaderBack: MomentsRefreshHeaderBack? = null

    fun setMomentsRefreshHeaderBack(back: MomentsRefreshHeaderBack?) {
        this.mMomentsRefreshHeaderBack = back
    }


    interface MomentsRefreshHeaderBack {
        companion object {
            const val CLICK_HEAD = 1

            const val CLICK_COVER = 2

            const val CLICK_AVATAR = 3
        }

        fun onClick(type: Int)
    }


}