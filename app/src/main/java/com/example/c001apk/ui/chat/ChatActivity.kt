package com.example.c001apk.ui.chat

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityChatBinding
import com.example.c001apk.logic.model.OSSUploadPrepareModel
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.feed.reply.emoji.EmojiPagerAdapter
import com.example.c001apk.ui.others.WebViewActivity
import com.example.c001apk.ui.user.UserActivity
import com.example.c001apk.util.EmojiUtils
import com.example.c001apk.util.ImageUtil.getImageDimensionsAndMD5
import com.example.c001apk.util.ImageUtil.toHex
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.makeToast
import com.example.c001apk.util.ossUpload
import com.example.c001apk.view.EmojiTextWatcher
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.view.View
import com.example.c001apk.util.DateUtils

@AndroidEntryPoint
class ChatActivity : BaseActivity<ActivityChatBinding>() {

    private val ukey by lazy { intent.getStringExtra("ukey").orEmpty() }
    private val uid by lazy { intent.getStringExtra("uid").orEmpty() }
    private val username by lazy { intent.getStringExtra("username").orEmpty() }

    @Inject
    lateinit var viewModelAssistedFactory: ChatViewModel.Factory
    private val viewModel by viewModels<ChatViewModel> {
        ChatViewModel.provideFactory(viewModelAssistedFactory, ukey)
    }

    private lateinit var chatAdapter: ChatAdapter
    private val layoutManager by lazy { LinearLayoutManager(this).apply { stackFromEnd = true } }
    private val imm by lazy { getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager }

    private val dataList by lazy { EmojiUtils.emojiMap.toList() }
    private val recentList = ArrayList<List<Pair<String, Int>>>()
    private val emojiList = ArrayList<List<Pair<String, Int>>>()
    private val coolBList = ArrayList<List<Pair<String, Int>>>()

    private var pendingUri: Uri? = null
    private var pendingType: String = ""
    private var pendingMd5: ByteArray? = null
    private lateinit var pickMedia: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val displayName = username.ifBlank { "私信" }
        binding.toolBar.title = displayName
        supportActionBar?.title = displayName
        binding.toolBar.setNavigationOnClickListener { finish() }

        initList()
        initEmojiPanel()
        initInput()
        initPhotoPicker()
        initObserve()

        viewModel.fetchData(true)
        viewModel.checkBlocked(uid)
    }

    private var menuBlock: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        menuBlock = menu.findItem(R.id.block)
        viewModel.isBlocked.observe(this) {
            menuBlock?.title = if (it) "移除黑名单" else "屏蔽"
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.check -> {
                IntentUtil.startActivity<UserActivity>(this) {
                    putExtra("id", uid)
                }
            }

            R.id.block -> {
                val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                MaterialAlertDialogBuilder(this).apply {
                    setTitle("确定将 $username ${menuBlock?.title}？")
                    setNegativeButton(android.R.string.cancel, null)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        if (isBlocked) viewModel.deleteUid(uid) else viewModel.saveUid(uid)
                    }
                    show()
                }
            }

            R.id.report -> {
                IntentUtil.startActivity<WebViewActivity>(this) {
                    putExtra("url", "https://m.coolapk.com/mp/do?c=user&m=report&id=$uid")
                }
            }
        }
        return true
    }

    private fun initList() {
        chatAdapter = ChatAdapter(
            onDelete = { id -> viewModel.deleteMessage(id) },
            onResolveImage = { id -> viewModel.resolveImageUrl(id) }
        )
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = chatAdapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() == 0 && !viewModel.isEnd) {
                    viewModel.fetchData(false)
                }
            }
        })
    }

    private fun initInput() {
        binding.inputEdit.addTextChangedListener(
            EmojiTextWatcher(this, binding.inputEdit.textSize) {}
        )

        binding.sendButton.setOnClickListener {
            val text = binding.inputEdit.text.toString()
            if (text.isBlank()) return@setOnClickListener
            viewModel.sendMessage(uid, text)
            binding.inputEdit.setText("")
        }

        binding.emojiButton.setOnClickListener {
            if (binding.emojiPanel.isVisible) {
                hideEmojiPanel()
            } else {
                imm.hideSoftInputFromWindow(binding.inputEdit.windowToken, 0)
                binding.emojiIndicator.isVisible = true
                binding.emojiPanel.isVisible = true
            }
        }

        binding.inputEdit.setOnClickListener { hideEmojiPanel() }
    }

    private fun hideEmojiPanel() {
        binding.emojiIndicator.isVisible = false
        binding.emojiPanel.isVisible = false
    }

    private fun initEmojiPanel() {
        for (i in 0..3) {
            emojiList.add(dataList.subList(i * 27 + 4, (i + 1) * 27 + 4))
        }
        emojiList.add(dataList.subList(155, dataList.size))
        coolBList.add(dataList.subList(112, 139))
        coolBList.add(dataList.subList(139, 155))
        val list = listOf(recentList, emojiList, coolBList)

        listOf("最近", "默认", "酷币").forEach { name ->
            binding.emojiIndicator.addView(
                TextView(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply { weight = 1f }
                    gravity = Gravity.CENTER
                    text = name
                    setOnClickListener {
                        binding.emojiPanel.setCurrentItem(
                            listOf("最近", "默认", "酷币").indexOf(name), false
                        )
                    }
                }
            )
        }

        binding.emojiPanel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in 0 until binding.emojiIndicator.childCount) {
                    (binding.emojiIndicator.getChildAt(i) as? TextView)?.setTextColor(
                        if (i == position)
                            MaterialColors.getColor(
                                this@ChatActivity,
                                com.google.android.material.R.attr.colorPrimary, 0
                            )
                        else getColor(android.R.color.darker_gray)
                    )
                }
            }
        })

        binding.emojiPanel.adapter = EmojiPagerAdapter(
            list,
            onClickEmoji = {
                with(binding.inputEdit) {
                    editableText.replace(selectionStart, selectionEnd, it)
                    viewModel.updateRecentEmoji(it)
                }
            },
            onCountStart = {},
            onCountStop = {}
        )

        viewModel.recentEmojiLiveData.observe(this) {
            recentList.clear()
            recentList.add(
                0,
                it.orEmpty().map { item ->
                    Pair(item.data, EmojiUtils.emojiMap[item.data] ?: R.drawable.ic_logo)
                }
            )
            binding.emojiPanel.adapter?.notifyItemChanged(0)
        }
    }

    private fun initPhotoPicker() {
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            val result = getImageDimensionsAndMD5(contentResolver, uri)
            val type = result.first?.third ?: ""
            pendingUri = uri
            pendingType = type
            pendingMd5 = result.second
            val name = "${UUID.randomUUID().toString().replace("-", "")}.${
                if (type.startsWith("image/")) type.substring(6) else type
            }"
            val width = result.first?.first ?: 0
            val height = result.first?.second ?: 0
            viewModel.onPostOSSUploadPrepare(
                uid,
                listOf(
                    OSSUploadPrepareModel(
                        name = name,
                        resolution = "${width}x${height}",
                        md5 = result.second?.toHex() ?: ""
                    )
                )
            )
        }

        binding.imageButton.setOnClickListener {
            hideEmojiPanel()
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun initObserve() {
        viewModel.chatListData.observe(this) { list ->
            val wasAtBottom = layoutManager.findLastVisibleItemPosition() >= chatAdapter.itemCount - 2
            chatAdapter.submitList(list) {
                if (wasAtBottom && list.isNotEmpty())
                    binding.recyclerView.scrollToPosition(list.size - 1)
            }
        }

        viewModel.toastText.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let { makeToast(it) }
        }

        viewModel.uploadImage.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let { responseData ->
                val uri = pendingUri ?: return@let
                makeToast("正在上传图片")
                lifecycleScope.launch(Dispatchers.IO) {
                    ossUpload(
                        this@ChatActivity, responseData,
                        listOf(uri), listOf(pendingType), listOf(pendingMd5),
                        iOnSuccess = {
                            val pic = "/" + responseData.fileInfo[0].uploadFileName
                            runOnUiThread { viewModel.sendMessage(uid, "", pic) }
                        },
                        iOnFailure = {
                            runOnUiThread { makeToast("图片上传失败") }
                        },
                        closeDialog = {}
                    )
                }
            }
        }
    }
}
