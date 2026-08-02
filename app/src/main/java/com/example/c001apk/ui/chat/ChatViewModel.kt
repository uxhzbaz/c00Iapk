package com.example.c001apk.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.ChatResponse
import com.example.c001apk.logic.model.OSSUploadPrepareModel
import com.example.c001apk.logic.model.OSSUploadPrepareResponse
import com.example.c001apk.logic.model.StringEntity
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.logic.repository.RecentEmojiRepo
import com.example.c001apk.util.Event
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel @AssistedInject constructor(
    @Assisted val ukey: String,
    private val recentEmojiRepo: RecentEmojiRepo,
    private val networkRepo: NetworkRepo,
    private val blackListRepo: com.example.c001apk.logic.repository.BlackListRepo
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
                        val messages = feed.data.filter { it.entityType == "message" }
                        lastItem = feed.data.lastOrNull()?.id
                        if (feed.data.isEmpty()) isEnd = true else page++
                        chatListData.postValue(old + messages)
                    } else {
                        toastText.postValue(Event(feed?.message ?: "加载失败"))
                    }
                }
        }
    }

    fun sendMessage(uid: String, text: String, pic: String? = null) {
        if (text.isBlank() && pic.isNullOrEmpty()) return
        val data = hashMapOf("message" to text)
        if (!pic.isNullOrEmpty()) data["message_pic"] = pic
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.sendMessage(data, uid)
                .collect { result ->
                    val response = result.getOrNull()
                    if (!response?.message.isNullOrEmpty()) {
                        toastText.postValue(Event(response!!.message!!))
                    } else if (response?.data != null) {
                        chatListData.postValue(chatListData.value.orEmpty() + response.data)
                    }
                }
        }
    }

    val isBlocked = MutableLiveData<Boolean>()
    fun checkBlocked(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isBlocked.postValue(blackListRepo.checkUid(uid))
        }
    }

    fun saveUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)
            isBlocked.postValue(true)
        }
    }

    fun deleteUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.deleteUid(uid)
            isBlocked.postValue(false)
        }
    }

    private var resolvingImage = HashSet<String>()
    fun resolveImageUrl(id: String) {
        if (!resolvingImage.add(id)) return
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getImageUrl(id)
                .collect { result ->
                    val url = result.getOrNull()
                    if (!url.isNullOrEmpty()) {
                        chatListData.postValue(
                            chatListData.value.orEmpty().map {
                                if (it.id == id) it.copy(messagePic = url) else it
                            }
                        )
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

    val recentEmojiLiveData: LiveData<List<StringEntity>> = recentEmojiRepo.loadAllListLive()

    fun updateRecentEmoji(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (recentEmojiRepo.checkEmoji(data)) {
                recentEmojiRepo.updateEmoji(data, System.currentTimeMillis())
            } else {
                if (recentEmojiLiveData.value?.size == 27)
                    recentEmojiLiveData.value?.last()?.data?.let {
                        recentEmojiRepo.updateEmoji(it, data, System.currentTimeMillis())
                    }
                else
                    recentEmojiRepo.insertEmoji(StringEntity(data))
            }
        }
    }

    val uploadImage = MutableLiveData<Event<OSSUploadPrepareResponse.Data>>()
    fun onPostOSSUploadPrepare(uid: String, imageList: List<OSSUploadPrepareModel>) {
        val ossUploadPrepareData = hashMapOf(
            "uploadBucket" to "message",
            "uploadDir" to "message",
            "is_anonymous" to "0",
            "uploadFileList" to Gson().toJson(imageList),
            "toUid" to uid
        )
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postOSSUploadPrepare(ossUploadPrepareData)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data?.message != null) {
                        toastText.postValue(Event(data.message))
                    } else if (data?.data != null) {
                        uploadImage.postValue(Event(data.data))
                    }
                }
        }
    }
}
