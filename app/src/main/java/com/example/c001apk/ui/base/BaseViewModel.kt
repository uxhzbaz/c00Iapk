package com.example.c001apk.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.c001apk.adapter.LoadingState

private val tagRe = Regex("<[^>]*>")
fun String?.hit(k: String) = this?.replace(tagRe, "")?.contains(k, true) == true

abstract class BaseViewModel : ViewModel() {

    var lastItem: String? = null
    var isInit: Boolean = true
    var listSize: Int = -1
    var isRefreshing: Boolean = false
    var isLoadMore: Boolean = false
    var isEnd: Boolean = false
    var page: Int = 1
    var kw: String = ""

    val activityState = MutableLiveData<LoadingState>()
    val loadingState = MutableLiveData<LoadingState>()

    abstract fun fetchData()

}
