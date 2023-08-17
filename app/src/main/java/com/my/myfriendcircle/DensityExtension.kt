package com.my.myfriendcircle

import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics

/**
 * @ClassName DensityExtension
 * @Description TODO
 * @Author mwj
 * @Date 2023/8/17 15:15
 * @Version 1.0
 **/

val Number.dp: Int
    get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

/**
 * 屏幕高
 * @receiver Activity
 * @return Int
 */
fun Activity.screenHeight(): Int {
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metrics)
    return metrics.heightPixels
}