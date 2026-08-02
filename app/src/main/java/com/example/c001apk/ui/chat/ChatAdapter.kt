package com.example.c001apk.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.c001apk.constant.Constants.USER_AGENT
import com.example.c001apk.databinding.ItemChatBinding
import com.example.c001apk.logic.model.ChatResponse
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.mikaelzero.mojito.Mojito

class ChatAdapter(
    private val onDelete: (String) -> Unit,
    private val onResolveImage: (String) -> Unit
) : ListAdapter<ChatResponse.Data, ChatAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        val isMine = data.fromuid == PrefManager.uid
        holder.binding.apply {
            leftGroup.isVisible = !isMine
            rightGroup.isVisible = isMine
            val avatar = if (isMine) rightAvatar else leftAvatar
            val bubbleText = if (isMine) rightText else leftText
            val bubbleImage = if (isMine) rightImage else leftImage
            ImageUtil.showIMG(avatar, data.fromUserAvatar)

            bubbleImage.isVisible = !data.messagePic.isNullOrEmpty()
            if (!data.messagePic.isNullOrEmpty()) {
                if (data.messagePic.startsWith("http")) {
                    val glideUrl = GlideUrl(
                        data.messagePic,
                        LazyHeaders.Builder().addHeader("User-Agent", USER_AGENT).build()
                    )
                    Glide.with(bubbleImage).load(glideUrl).into(bubbleImage)
                    bubbleImage.setOnClickListener {
                        Mojito.start(root.context) {
                            urls(listOf(data.messagePic), listOf(data.messagePic))
                        }
                    }
                } else {
                    bubbleImage.setImageDrawable(null)
                    bubbleImage.setOnClickListener(null)
                    onResolveImage(data.id)
                }
            }

            root.setOnLongClickListener {
                MaterialAlertDialogBuilder(root.context)
                    .setItems(arrayOf("删除")) { _, _ -> onDelete(data.id) }
                    .show()
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatResponse.Data>() {
        override fun areItemsTheSame(old: ChatResponse.Data, new: ChatResponse.Data) =
            old.id == new.id

        override fun areContentsTheSame(old: ChatResponse.Data, new: ChatResponse.Data) =
            old == new
    }
}
