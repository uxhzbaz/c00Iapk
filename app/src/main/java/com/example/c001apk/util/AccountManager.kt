package com.example.c001apk.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AccountManager {

    data class Account(
        val uid: String,
        val username: String,
        val token: String,
        val userAvatar: String
    )

    private val gson = Gson()

    var accounts: List<Account>
        get() = runCatching {
            gson.fromJson<List<Account>>(
                PrefManager.accountsJson,
                object : TypeToken<List<Account>>() {}.type
            )
        }.getOrNull().orEmpty()
        private set(value) {
            PrefManager.accountsJson = gson.toJson(value)
        }

    fun saveCurrent() {
        if (!PrefManager.isLogin || PrefManager.uid.isEmpty()) return
        val current =
            Account(PrefManager.uid, PrefManager.username, PrefManager.token, PrefManager.userAvatar)
        accounts = accounts.filterNot { it.uid == current.uid } + current
    }

    fun switchTo(account: Account) {
        PrefManager.uid = account.uid
        PrefManager.username = account.username
        PrefManager.token = account.token
        PrefManager.userAvatar = account.userAvatar
        PrefManager.isLogin = true
    }
}
