package com.example.c001apk.logic.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val message: String?,
    val data: List<Data>?
) {
    data class Data(
        val id: String,
        val fromuid: String,
        val fromusername: String,
        val fromUserAvatar: String,
        val message: String?,
        @SerializedName("message_pic") val messagePic: String?,
        val dateline: Long
    )
}
