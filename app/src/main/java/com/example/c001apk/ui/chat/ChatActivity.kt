package com.example.c001apk.ui.chat

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.databinding.ActivityChatBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.makeToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolBar.title = username
        binding.toolBar.setNavigationOnClickListener { finish() }

        chatAdapter = ChatAdapter { id -> viewModel.deleteMessage(id) }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = chatAdapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() == 0 && !viewModel.isEnd) {
                    viewModel.fetchData(false)
                }
            }
        })

        binding.sendButton.setOnClickListener {
            val text = binding.inputEdit.text.toString()
            viewModel.sendMessage(uid, text)
            binding.inputEdit.setText("")
        }

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

        viewModel.fetchData(true)
    }
}
