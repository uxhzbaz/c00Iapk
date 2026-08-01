package com.example.c001apk.ui.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.logic.model.ChatResponse
import com.example.c001apk.util.Event
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel @AssistedInject constructor(
    @Assisted val ukey: String,
    private val networkRepo: NetworkRepo
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(ukey: String): ChatViewModel
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun provideFactory(assistedFactory: Factory, ukey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    assistedFactory.create(ukey) as T
            }
    }

    var page = 1
    var lastItem: String? = null
    var isEnd = false
    private var isLoading = false

    val chatListData = MutableLiveData<List<ChatResponse.Data>>()
    val toastText = MutableLiveData<Event<String>>()

    fun fetchData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true
        if (isRefresh) {
            page = 1
            lastItem = null
            isEnd = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getChatMessage(ukey, page, lastItem)
                .collect { result ->
                    isLoading = false
                    val feed = result.getOrNull()
                    if (feed?.data != null) {
                        val old = if (isRefresh) emptyList() else chatListData.value.orEmpty()
                        lastItem = feed.data.lastOrNull()?.id
                        if (feed.data.isEmpty()) isEnd = true else page++
                        chatListData.postValue(old + feed.data)
                    } else {
                        toastText.postValue(Event(feed?.message ?: "加载失败"))
                    }
                }
        }
    }

    fun sendMessage(uid: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.sendMessage(uid, text)
                .collect { result ->
                    val data = result.getOrNull()
                    if (!data?.message.isNullOrEmpty()) {
                        toastText.postValue(Event(data!!.message!!))
                    } else if (data?.data != null) {
                        chatListData.postValue(chatListData.value.orEmpty() + data.data)
                    }
                }
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete("/v6/message/delete", id).collect {
                chatListData.postValue(chatListData.value.orEmpty().filter { it.id != id })
            }
        }
    }
}
