package com.example.c001apk.constant

import com.example.c001apk.MyApplication.Companion.context
import com.example.c001apk.util.PrefManager
import rikka.core.util.ResourceUtils

object Constants {
    const val REQUEST_WITH = "XMLHttpRequest"
    const val LOCALE = "zh-CN"
    const val APP_ID = "com.coolapk.market"
    var DARK_MODE =
        if (ResourceUtils.isNightMode(context.resources.configuration)) "1"
        else "0"
    const val CHANNEL = "coolapk"
    const val MODE = "universal"
    const val VERSION_NAME = "16.6.5"
    const val API_VERSION = "16"
    const val VERSION_CODE = "2610162"
    val USER_AGENT =
        "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; ${PrefManager.MODEL} ${PrefManager.BUILDNUMBER}) (#Build; ${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILDNUMBER}; ${PrefManager.ANDROID_VERSION}) +CoolMarket/${PrefManager.VERSION_NAME}-${PrefManager.VERSION_CODE}-${MODE}"

    // "${System.getProperty("http.agent")} (#Build; ${android.os.Build.BRAND}; ${android.os.Build.MODEL}; ${android.os.Build.DISPLAY}; ${android.os.Build.VERSION.RELEASE}) +CoolMarket/${VERSION_NAME}-${VERSION_CODE}-${MODE}"
    const val SZLM_ID = "数字联盟ID不能为空"
    const val LOADING_FAILED = "加载失败"
    const val LOADING_EMPTY = "什么也没有"
    const val LOADING_END = "没有更多了"
}
