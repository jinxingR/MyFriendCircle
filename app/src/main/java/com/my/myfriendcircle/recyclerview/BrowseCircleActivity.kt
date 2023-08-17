package com.my.myfriendcircle.recyclerview

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View.OVER_SCROLL_NEVER
import androidx.appcompat.app.AppCompatActivity
import com.drake.statusbar.statusBarHeight
import com.github.jdsjlzx.interfaces.OnLoadMoreListener
import com.github.jdsjlzx.interfaces.OnRefreshListener
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter
import com.github.jdsjlzx.recyclerview.ProgressStyle
import com.github.jdsjlzx.util.WeakHandler
import com.my.myfriendcircle.R
import com.my.myfriendcircle.databinding.ActivityBrowseCircleBinding
import com.my.myfriendcircle.dp
import com.my.myfriendcircle.screenHeight


/**
 * @ClassName BrowseCircleActivity
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/8 17:04
 * @Version 1.0
 **/
class BrowseCircleActivity : AppCompatActivity() {

   private lateinit var binding : ActivityBrowseCircleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseCircleBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private val mHandler: WeakHandler = object : WeakHandler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                -1 -> {
                    binding.rvList.refreshComplete(10)
                }
                else -> {}
            }
        }
    }

    private lateinit var circleAdapter: DataAdapter
    private lateinit var mLRecyclerViewAdapter: LRecyclerViewAdapter
    private var circleRefreshHeader: CircleRefreshHeader? = null

   private  fun initView() {
        circleAdapter = DataAdapter(R.layout.sample_item_text, ArrayList())
        for (i in 0 until 50) {
            circleAdapter.data.add(ItemModel(i.toString()))
        }
        binding.rvList.apply {
            overScrollMode = OVER_SCROLL_NEVER
            initHead()
            circleRefreshHeader = CircleRefreshHeader(this@BrowseCircleActivity)
            setRefreshHeader(circleRefreshHeader)
            mLRecyclerViewAdapter = LRecyclerViewAdapter(circleAdapter)
            adapter = mLRecyclerViewAdapter
            setRefreshProgressStyle(ProgressStyle.LineSpinFadeLoader)
            setLoadingMoreProgressStyle(ProgressStyle.BallSpinFadeLoader)
            setPullRefreshEnabled(true)
            val height = screenHeight()
            circleRefreshHeader?.setRefreshTranslationY(44f.dp + statusBarHeight)
                ?.setCoverImageHeight(height / 3, height * 4 / 5)
                ?.setTriggerCriticalPoint(height / 2)
                ?.toRequestLayout()

        }
        initListener()
        binding.rvList.postDelayed({
            binding.rvList.refresh()
        }, 100)

    }


    private fun initListener() {
        binding.rvList.apply {
            setOnRefreshListener(OnRefreshListener {
                requestData()
            })
            setOnLoadMoreListener(OnLoadMoreListener {
                requestData()
            })
        }

        circleRefreshHeader?.apply {
            setMomentsRefreshHeaderBack(object : CircleRefreshHeader.MomentsRefreshHeaderBack {
                override fun onClick(type: Int) {
                    binding.rvList.changHeadImageHeight()
                }
            })
        }
    }

    /**
     * 模拟请求网络
     */
    private fun requestData() {
        object : Thread() {
            override fun run() {
                super.run()
                try {
                    sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                mHandler.sendEmptyMessage(-1)
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}