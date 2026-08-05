package com.example.c001apk.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.constant.Constants
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.AccountManager
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.Event
import com.example.c001apk.util.PrefManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepo: NetworkRepo
) : ViewModel() {

    var lastCheck = System.currentTimeMillis()
    var isInit: Boolean = true
    val setBadge = MutableLiveData<Event<Boolean>>()

    fun fetchAppInfo(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getCheckLoginInfo()
        }
    }

    private fun getCheckLoginInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.checkLoginInfo()
                .collect { result ->
                    val response = result.getOrNull()
                    response?.let {
                        try {
                            val session = response.headers().values("Set-Cookie").firstOrNull()
                            session?.substringBefore(";")?.let { CookieUtil.SESSID = it }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        response.body()?.let {
                            if (response.body()?.data?.token != null) {
                                response.body()?.data?.let { login ->
                                    CookieUtil.badge = login.notifyCount.badge
                                    CookieUtil.atme = login.notifyCount.atme
                                    CookieUtil.atcommentme = login.notifyCount.atcommentme
                                    CookieUtil.feedlike = login.notifyCount.feedlike
                                    CookieUtil.contacts_follow = login.notifyCount.contactsFollow
                                    PrefManager.isLogin = true
                                    PrefManager.uid = login.uid
                                    PrefManager.username =
                                        withContext(Dispatchers.IO) {
                                            URLEncoder.encode(login.username, "UTF-8")
                                        }
                                    PrefManager.token = login.token
                                    PrefManager.userAvatar = login.userAvatar
                                    AccountManager.saveCurrent()
                                }
                            } else if (response.body()?.message == "登录信息有误") {
                                PrefManager.isLogin = false
                                PrefManager.uid = ""
                                PrefManager.username = ""
                                PrefManager.token = ""
                                PrefManager.userAvatar = ""
                            }

                            if (CookieUtil.badge != 0)
                                setBadge.postValue(Event(true))
                        }
                    }
                }
        }
    }

    fun onCheckCount() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.checkCount()
                .collect { result ->
                    val response = result.getOrNull()
                    response?.data?.let {
                        CookieUtil.atme = it.atme
                        CookieUtil.atcommentme = it.atcommentme
                        CookieUtil.feedlike = it.feedlike
                        CookieUtil.contacts_follow = it.contactsFollow
                        CookieUtil.badge = it.badge
                        CookieUtil.notification = it.notification
                        if (CookieUtil.badge != 0)
                            setBadge.postValue(Event(true))
                    }
                }
        }
    }

}
