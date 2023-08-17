package com.my.myfriendcircle.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.jdsjlzx.interfaces.*
import com.github.jdsjlzx.recyclerview.AppBarStateChangeListener
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter
import com.github.jdsjlzx.view.ArrowRefreshHeader
import com.github.jdsjlzx.view.LoadingFooter
import com.google.android.material.appbar.AppBarLayout

/**
 * @ClassName HeaderPullRefreshRecyclerView
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/10 16:40
 * @Version 1.0
 **/
class CircleRecyclerView : RecyclerView {

    companion object {

        /**
         * 拖动速率
         */
        private const val DRAG_RATE = 2.0f

        /**
         * 触发在上下滑动监听器的容差距离
         */
        private const val HIDE_THRESHOLD = 20
    }

    /**
     * 是否允许下拉刷新
     */
    private var mPullRefreshEnabled = false

    private var mLoadMoreEnabled = true

    private var mRefreshing = false //是否正在下拉刷新

    private var mLoadingData = false //是否正在加载数据

    private var mRefreshListener: OnRefreshListener? = null
    private var mLoadMoreListener: OnLoadMoreListener? = null
    private var mLScrollListener: LScrollListener? = null
    private var mRefreshHeader: ICircleRefreshHeader? = null
    private var mLoadMoreFooter: ILoadMoreFooter? = null
    private var mEmptyView: View? = null
    private var mFootView: View? = null

    private val mDataObserver: AdapterDataObserver = DataObserver()
    private var mLastY = -1f
    private var sumOffSet = 0f

    private var mPageSize = 10 //一次网络请求默认数量


    private var mWrapAdapter: LRecyclerViewAdapter? = null
    private var isNoMore = false
    private var mIsVpDragger = false
    private var mTouchSlop = 0
    private var startY = 0f
    private var startX = 0f
    private var isRegisterDataObserver = false


    //<editor-fold desc="滚动的变量">
    /**
     * 最后一个的位置
     */
    private var lastPositions: IntArray? = null

    /**
     * 最后一个可见的item的位置
     */
    private var lastVisibleItemPosition = 0

    /**
     * 当前滑动的状态
     */
    private var currentScrollState = 0


    /**
     * 滑动的距离
     */
    private var mDistance = 0

    /**
     * 是否需要监听控制
     */
    private var mIsScrollDown = true

    /**
     * Y轴移动的实际距离（最顶部为0）
     */
    private var mScrolledYDistance = 0

    /**
     * X轴移动的实际距离（最左侧为0）
     */
    private var mScrolledXDistance = 0

    //</editor-fold>


    private var appbarState: AppBarStateChangeListener.State =
        AppBarStateChangeListener.State.EXPANDED

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    fun initHead() {
        mTouchSlop = ViewConfiguration.get(context.applicationContext).scaledTouchSlop
        if (mPullRefreshEnabled) {
            setRefreshHeader(ArrowCustomRefreshHeader(context.applicationContext))
        }
        if (mLoadMoreEnabled) {
            setLoadMoreFooter(LoadingFooter(context.applicationContext))
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        if ( isRegisterDataObserver) {
            mWrapAdapter?.innerAdapter?.unregisterAdapterDataObserver(mDataObserver)
        }
        mWrapAdapter = adapter as LRecyclerViewAdapter?
        super.setAdapter(mWrapAdapter)
        mDataObserver.apply {
            mWrapAdapter?.innerAdapter?.registerAdapterDataObserver(this)
            onChanged()
        }
        isRegisterDataObserver = true
        mWrapAdapter?.setRefreshHeader(mRefreshHeader)

        //fix bug: https://github.com/jdsjlzx/LRecyclerView/issues/115
        if (mLoadMoreEnabled && mWrapAdapter?.getFooterViewsCount() === 0 && mFootView != null) {
            mWrapAdapter?.addFooterView(mFootView)
        }
    }

    protected override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isRegisterDataObserver) {
            mWrapAdapter?.innerAdapter?.unregisterAdapterDataObserver(mDataObserver)
            isRegisterDataObserver = false
        }
    }

    private inner class DataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            val mAdapter: Adapter<*> = adapter as Adapter<*>
            if (mAdapter is LRecyclerViewAdapter) {
                val lRecyclerViewAdapter: LRecyclerViewAdapter = mAdapter
                val count: Int = lRecyclerViewAdapter.innerAdapter.itemCount
                if (count == 0) {
                    mEmptyView?.visibility = View.VISIBLE
                    this@CircleRecyclerView.visibility = View.GONE
                } else {
                    mEmptyView?.visibility = View.GONE
                    this@CircleRecyclerView.visibility = View.VISIBLE
                }
            } else {
                mAdapter.apply {
                    if (itemCount == 0) {
                        mEmptyView?.visibility = View.VISIBLE
                        this@CircleRecyclerView.visibility = View.GONE
                    } else {
                        mEmptyView?.visibility = View.GONE
                        this@CircleRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
            mWrapAdapter?.notifyDataSetChanged()
            if ((mWrapAdapter?.innerAdapter?.itemCount ?: 0) < mPageSize) {
                mFootView?.visibility = View.GONE
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            mWrapAdapter?.apply {
                notifyItemRangeChanged(positionStart + headerViewsCount + 1, itemCount)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            mWrapAdapter?.apply {
                notifyItemRangeChanged(positionStart + headerViewsCount + 1, itemCount)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            mWrapAdapter?.apply {
                notifyItemRangeRemoved(
                    positionStart + headerViewsCount + 1,
                    itemCount
                )
                if (innerAdapter.itemCount < mPageSize) {
                    mFootView?.visibility = View.GONE
                }
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            mWrapAdapter?.apply {
                val headerViewsCountCount: Int = headerViewsCount
                notifyItemRangeChanged(
                    fromPosition + headerViewsCountCount + 1,
                    toPosition + headerViewsCountCount + 1 + itemCount
                )
            }
        }
    }

    /**
     * 解决嵌套RecyclerView滑动冲突问题
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录手指按下的位置
                startY = ev.y
                startX = ev.x
                // 初始化标记
                mIsVpDragger = false
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果viewpager正在拖拽中，那么不拦截它的事件，直接return false；
                if (mIsVpDragger) {
                    return false
                }

                // 获取当前手指位置
                val endY: Float = ev.y
                val endX: Float = ev.x
                val distanceX = Math.abs(endX - startX)
                val distanceY = Math.abs(endY - startY)
                // 如果X轴位移大于Y轴位移，那么将事件交给viewPager处理。
                if (distanceX > mTouchSlop && distanceX > distanceY) {
                    mIsVpDragger = true
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {  // 初始化标记
                mIsVpDragger = false
            }
        }
        // 如果是Y轴位移大于X轴，事件交给swipeRefreshLayout处理。
        return super.onInterceptTouchEvent(ev)
    }

    private var mActivePointerId = 0
    private var isCoverMax = false
    private var isTouchMove = false


    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (mLastY == -1f) { // 如果adapter设置了setOnItemClickListener点击事件，则RecyclerView的ACTION_DOWN事件被拦截，这里通过这种方式获取起始坐标。
            mLastY = ev.y
            mActivePointerId = ev.getPointerId(0)
            sumOffSet = 0f
            isCoverMax = mRefreshHeader?.isMaxCover() ?: false
            isTouchMove = false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastY = ev.y
                mActivePointerId = ev.getPointerId(0)
                sumOffSet = 0f
                isCoverMax = mRefreshHeader?.isMaxCover() ?: false
                isTouchMove = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mActivePointerId = ev.getPointerId(index)
                mLastY = ev.getY(index)
            }
            MotionEvent.ACTION_MOVE -> {
                var pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex == -1) {
                    pointerIndex = 0
                    mActivePointerId = ev.getPointerId(pointerIndex)
                }
                val moveY = ev.getY(pointerIndex)
                val deltaY = (mLastY - moveY) / DRAG_RATE
                mLastY = moveY
                sumOffSet += deltaY
                if (isOnTop() && mPullRefreshEnabled && !mRefreshing && appbarState === AppBarStateChangeListener.State.EXPANDED) {
                    if (deltaY < 0 && !canScrollVertically(-1) ||
                        deltaY > 0 && !canScrollVertically(1)
                    ) { //判断无法下拉和无法上拉（item过少的情况）
                        overScrollBy(0, deltaY.toInt(), 0, 0, 0, 0, 0, sumOffSet.toInt(), true)
                    }
                } else if (isOnTopAndNoPullRefreshEnabled()
                    && !mPullRefreshEnabled
                    && mRefreshHeader?.isNeedOverScrollBy(deltaY < 0) == true
                    && scrollY == 0
                ) {
                    if (deltaY < 0 && !canScrollVertically(-1) || deltaY > 0) { //判断无法下拉和无法上拉（item过少的情况）
                        headerToMove(deltaY.toInt(), sumOffSet.toInt(), false)
                    }
                }
                isTouchMove = true
            }
            MotionEvent.ACTION_UP -> {
                mLastY = -1f // reset
                mActivePointerId = -1
                if (isOnTop() && mPullRefreshEnabled && !mRefreshing /*&& appbarState == AppBarStateChangeListener.State.EXPANDED*/) {
                    val isOnRefresh = mRefreshHeader?.onRelease() ?: false
                    if (isOnRefresh) {
                        mRefreshing = true
                        mFootView?.visibility = View.GONE
                        mRefreshListener?.onRefresh()
                    }
                } else if (isOnTopAndNoPullRefreshEnabled() && !mPullRefreshEnabled && isTouchMove) {
                    var value = mRefreshHeader?.onUp(isTouchMove)
                    if (value != null) {
                        mPullRefreshEnabled = value
                    }
                    if (isTouchMove) {
                        stopScroll()
                    }
                }
            }
        }
        if (isCoverMax && isTouchMove) {
            return true
        }
        return super.onTouchEvent(ev)
    }

    fun toOverScrollBy(
        deltaX: Int = 0,
        deltaY: Int = 0,
        scrollX: Int = 0,
        scrollY: Int = 0,
        scrollRangeX: Int = 0,
        scrollRangeY: Int = 0,
        maxOverScrollX: Int = 0,
        maxOverScrollY: Int = 0,
        isTouchEvent: Boolean = true
    ): Boolean {
        return overScrollBy(
            deltaX,
            deltaY,
            scrollX,
            scrollY,
            scrollRangeX,
            scrollRangeY,
            maxOverScrollX,
            maxOverScrollY,
            isTouchEvent
        )
    }

    private fun headerToMove(deltaY: Int = 0, maxOverScrollY: Int, isTouchEvent: Boolean = true) {
        if (deltaY != 0) {
            if (mRefreshHeader is ICircleRefreshHeader) {
                (mRefreshHeader as ICircleRefreshHeader).onMove(
                    deltaY.toFloat(),
                    maxOverScrollY.toFloat(),
                    isTouchEvent
                )
            } else {
                mRefreshHeader?.onMove(deltaY.toFloat(), sumOffSet)
            }
        }
    }


    override fun overScrollBy(
        deltaX: Int,
        deltaY: Int,
        scrollX: Int,
        scrollY: Int,
        scrollRangeX: Int,
        scrollRangeY: Int,
        maxOverScrollX: Int,
        maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        headerToMove(deltaY, maxOverScrollY, isTouchEvent)
        return super.overScrollBy(
            deltaX,
            deltaY,
            scrollX,
            scrollY,
            scrollRangeX,
            scrollRangeY,
            maxOverScrollX,
            maxOverScrollY,
            isTouchEvent
        )
    }

    private fun findMax(lastPositions: IntArray): Int {
        var max = lastPositions[0]
        for (value in lastPositions) {
            if (value > max) {
                max = value
            }
        }
        return max
    }

    private fun isOnTop(): Boolean {
        return mPullRefreshEnabled && mRefreshHeader?.headerView?.parent != null
    }

    private fun isOnTopAndNoPullRefreshEnabled(): Boolean {
        return !mPullRefreshEnabled && mRefreshHeader?.headerView?.parent != null
    }

    /**
     * set view when no content item
     *
     * @param emptyView visiable view when items is empty
     */
    fun setEmptyView(emptyView: View?) {
        mEmptyView = emptyView
        mDataObserver.onChanged()
    }

    /**
     * @param pageSize 一页加载的数量
     */
    fun refreshComplete(pageSize: Int) {
        mPageSize = pageSize
        if (mRefreshing) {
            isNoMore = false
            mRefreshing = false
            mRefreshHeader?.refreshComplete()
            if ((mWrapAdapter?.innerAdapter?.itemCount ?: 0) < pageSize) {
                mFootView?.visibility = View.GONE
            }
        } else if (mLoadingData) {
            mLoadingData = false
            mLoadMoreFooter?.onComplete()
        }
    }

    /**
     * 设置是否已加载全部
     */
    fun setNoMore(noMore: Boolean) {
        mLoadingData = false
        isNoMore = noMore
        if (isNoMore) {
            mLoadMoreFooter?.onNoMore()
        } else {
            mLoadMoreFooter?.onComplete()
        }
    }

    /**
     * 设置自定义的RefreshHeader
     * 注意：setRefreshHeader方法必须在setAdapter方法之前调用才能生效
     */
    fun setRefreshHeader(refreshHeader: ICircleRefreshHeader?) {
        if (isRegisterDataObserver) {
            throw RuntimeException("setRefreshHeader must been invoked before setting the adapter.")
        }
        mRefreshHeader = refreshHeader
    }

    fun setRefreshHeaderTranslationY(translation: Int) {
        mRefreshHeader
    }


    /**
     * 设置自定义的footerview
     */
    fun setLoadMoreFooter(loadMoreFooter: ILoadMoreFooter) {
        mLoadMoreFooter = loadMoreFooter
        mFootView = loadMoreFooter.footView
        mFootView?.visibility = View.GONE

        //wxm:mFootView inflate的时候没有以RecyclerView为parent，所以要设置LayoutParams
        val layoutParams = mFootView?.layoutParams
        mFootView?.layoutParams = if (layoutParams != null) {
            LayoutParams(layoutParams)
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
    }

    fun setPullRefreshEnabled(enabled: Boolean) {
        mPullRefreshEnabled = enabled
        mRefreshHeader?.setIsMoveAllowCover(!enabled)
    }

    /**
     * 到底加载是否可用
     */
    fun setLoadMoreEnabled(enabled: Boolean) {
        if (mWrapAdapter == null) {
            throw NullPointerException("LRecyclerViewAdapter cannot be null, please make sure the variable mWrapAdapter have been initialized.")
        }
        mLoadMoreEnabled = enabled
        if (!enabled) {
            mWrapAdapter?.removeFooterView()
        }
    }

    fun setRefreshProgressStyle(style: Int) {
        if (mRefreshHeader is ArrowRefreshHeader) {
            (mRefreshHeader as ArrowRefreshHeader).setProgressStyle(style)
        }
    }

    fun setArrowImageView(resId: Int) {
        if (mRefreshHeader is ArrowRefreshHeader) {
            (mRefreshHeader as ArrowRefreshHeader).setArrowImageView(resId)
        }
    }

    fun setLoadingMoreProgressStyle(style: Int) {
        if (mLoadMoreFooter is LoadingFooter) {
            (mLoadMoreFooter as LoadingFooter).setProgressStyle(style)
        }
    }

    fun setOnRefreshListener(listener: OnRefreshListener?) {
        mRefreshListener = listener
    }

    fun setOnLoadMoreListener(listener: OnLoadMoreListener?) {
        mLoadMoreListener = listener
    }

    fun setOnNetWorkErrorListener(listener: OnNetWorkErrorListener) {
        val loadingFooter = mFootView as LoadingFooter?
        loadingFooter?.state = ILoadMoreFooter.State.NetWorkError
        loadingFooter?.setOnClickListener {
            mLoadMoreFooter?.onLoading()
            listener.reload()
        }
    }

    fun setFooterViewHint(loading: String?, noMore: String?, noNetWork: String?) {
        if (mLoadMoreFooter is LoadingFooter) {
            val loadingFooter = mLoadMoreFooter as LoadingFooter
            loadingFooter.setLoadingHint(loading)
            loadingFooter.setNoMoreHint(noMore)
            loadingFooter.setNoNetWorkHint(noNetWork)
        }
    }

    /**
     * 设置Footer文字颜色
     */
    fun setFooterViewColor(indicatorColor: Int, hintColor: Int, backgroundColor: Int) {
        if (mLoadMoreFooter is LoadingFooter) {
            val loadingFooter = mLoadMoreFooter as LoadingFooter
            loadingFooter.setIndicatorColor(ContextCompat.getColor(context, indicatorColor))
            loadingFooter.setHintTextColor(hintColor)
            loadingFooter.setViewBackgroundColor(backgroundColor)
        }
    }

    /**
     * 设置颜色
     *
     * @param indicatorColor Only call the method setRefreshProgressStyle(int style) to take effect
     */
    fun setHeaderViewColor(indicatorColor: Int, hintColor: Int, backgroundColor: Int) {
        if (mRefreshHeader is ArrowRefreshHeader) {
            val arrowRefreshHeader = mRefreshHeader as ArrowRefreshHeader
            arrowRefreshHeader.setIndicatorColor(
                ContextCompat.getColor(
                    context,
                    indicatorColor
                )
            )
            arrowRefreshHeader.setHintTextColor(hintColor)
            arrowRefreshHeader.setViewBackgroundColor(backgroundColor)
        }
    }

    fun setLScrollListener(listener: LScrollListener?) {
        mLScrollListener = listener
    }

    interface LScrollListener {
        fun onScrollUp() //scroll down to up
        fun onScrollDown() //scroll from up to down
        fun onScrolled(distanceX: Int, distanceY: Int) // moving state,you can get the move distance
        fun onScrollStateChanged(state: Int)
    }

    fun refresh() {
        if ((mRefreshHeader?.visibleHeight
                ?: 0) > 0 || mRefreshing
        ) { // if RefreshHeader is Refreshing, return
            return
        }
        if (mPullRefreshEnabled) {
            mRefreshHeader?.onRefreshing()
            val offSet: Int = mRefreshHeader?.headerView?.measuredHeight ?: 0
            mRefreshHeader?.onMove(offSet.toFloat(), offSet.toFloat())
            mRefreshing = true
            mFootView?.visibility = View.GONE
            mRefreshListener?.onRefresh()
        }
    }

    fun forceToRefresh() {
        if (mLoadingData) {
            return
        }
        refresh()
    }

    /**
     * 当前RecyclerView类型
     */
    protected var layoutManagerType: LayoutManagerType? = null

    override fun onScrolled(dx: Int, dy: Int) {

        super.onScrolled(dx, dy)
        var firstVisibleItemPosition = 0
        val layoutManager: LayoutManager = layoutManager as LayoutManager
        if (layoutManagerType == null) {
            layoutManagerType = if (layoutManager is LinearLayoutManager) {
                LayoutManagerType.LinearLayout
            } else if (layoutManager is GridLayoutManager) {
                LayoutManagerType.GridLayout
            } else if (layoutManager is StaggeredGridLayoutManager) {
                LayoutManagerType.StaggeredGridLayout
            } else {
                throw RuntimeException(
                    "Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager"
                )
            }
        }
        when (layoutManagerType) {
            LayoutManagerType.LinearLayout -> {
                firstVisibleItemPosition =
                    (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                lastVisibleItemPosition =
                    (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            }
            LayoutManagerType.GridLayout -> {
                firstVisibleItemPosition =
                    (layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
                lastVisibleItemPosition =
                    (layoutManager as GridLayoutManager).findLastVisibleItemPosition()
            }
            LayoutManagerType.StaggeredGridLayout -> {
                val staggeredGridLayoutManager: StaggeredGridLayoutManager =
                    layoutManager as StaggeredGridLayoutManager
                if (lastPositions == null) {
                    lastPositions = IntArray(staggeredGridLayoutManager.spanCount)
                }
                staggeredGridLayoutManager.findLastVisibleItemPositions(lastPositions)
                lastVisibleItemPosition = findMax(lastPositions!!)
                staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(lastPositions)
                firstVisibleItemPosition = findMax(lastPositions!!)
            }
            else -> {

            }
        }

        // 根据类型来计算出第一个可见的item的位置，由此判断是否触发到底部的监听器
        // 计算并判断当前是向上滑动还是向下滑动
        calculateScrollUpOrDown(firstVisibleItemPosition, dy)
        // 移动距离超过一定的范围，我们监听就没有啥实际的意义了
        mScrolledXDistance += dx
        mScrolledYDistance += dy
        mScrolledXDistance = if (mScrolledXDistance < 0) 0 else mScrolledXDistance
        mScrolledYDistance = if (mScrolledYDistance < 0) 0 else mScrolledYDistance
        if (mIsScrollDown && dy == 0) {
            mScrolledYDistance = 0
        }
        //Be careful in here
        mLScrollListener?.onScrolled(mScrolledXDistance, mScrolledYDistance)
        if (mLoadMoreListener != null && mLoadMoreEnabled) {
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            if (visibleItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 1 && totalItemCount > visibleItemCount && !isNoMore && !mRefreshing) {
                mFootView?.setVisibility(View.VISIBLE)
                if (!mLoadingData) {
                    mLoadingData = true
                    mLoadMoreFooter?.onLoading()
                    mLoadMoreListener?.onLoadMore()
                }
            }
        }
        if (isOnTop() && mPullRefreshEnabled && !mRefreshing && appbarState === AppBarStateChangeListener.State.EXPANDED) {
            if (dy > 0) {
                mRefreshHeader?.onMove(dy.toFloat(), mScrolledYDistance.toFloat())
            }
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        currentScrollState = state
        mLScrollListener?.onScrollStateChanged(state)
    }

    /**
     * 计算当前是向上滑动还是向下滑动
     */
    private fun calculateScrollUpOrDown(firstVisibleItemPosition: Int, dy: Int) {
        if (mLScrollListener != null) {
            if (firstVisibleItemPosition == 0) {
                if (!mIsScrollDown) {
                    mIsScrollDown = true
                    mLScrollListener?.onScrollDown()
                }
            } else {
                if (mDistance > HIDE_THRESHOLD && mIsScrollDown) {
                    mIsScrollDown = false
                    mLScrollListener?.onScrollUp()
                    mDistance = 0
                } else if (mDistance < -HIDE_THRESHOLD && !mIsScrollDown) {
                    mIsScrollDown = true
                    mLScrollListener?.onScrollDown()
                    mDistance = 0
                }
            }
        }
        if (mIsScrollDown && dy > 0 || !mIsScrollDown && dy < 0) {
            mDistance += dy
        }
    }

    enum class LayoutManagerType {
        LinearLayout, StaggeredGridLayout, GridLayout
    }

    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //解决LRecyclerView与CollapsingToolbarLayout滑动冲突的问题
        var appBarLayout: AppBarLayout? = null
        var p: ViewParent? = parent
        while (p != null) {
            if (p is CoordinatorLayout) {
                break
            }
            p = p.parent
        }
        if (null != p && p is CoordinatorLayout) {
            val coordinatorLayout: CoordinatorLayout = p
            val childCount: Int = coordinatorLayout.childCount
            for (i in childCount - 1 downTo 0) {
                val child: View = coordinatorLayout.getChildAt(i)
                if (child is AppBarLayout) {
                    appBarLayout = child as AppBarLayout
                    break
                }
            }
            appBarLayout?.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
                override fun onStateChanged(p0: AppBarLayout?, state: State) {
                    appbarState = state
                }
            })
        }
    }


    /**
     * 改变head的高度值（放大，还是复原）
     */
    fun changHeadImageHeight() {
        mRefreshHeader?.apply {
            mPullRefreshEnabled = isMaxCover()
            var offset = if (isMaxCover()) {
                getCoverImageMaxHeight() - getCoverImageMinHeight()
            } else {
                var height = if (getHeaderViewHeight() > 0) {
                    getCoverImageMaxHeight() - getCoverViewHeight()
                } else {
                    getCoverImageMaxHeight()
                }
                0 - height
            }
            toOverScrollBy(
                0, offset, 0, 0, 0,
                0, 0, offset, false
            )
        }

    }
}