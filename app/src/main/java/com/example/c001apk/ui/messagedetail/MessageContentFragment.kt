package com.example.c001apk.ui.messagedetail

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import com.example.c001apk.adapter.FooterAdapter
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.HeaderAdapter
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.ui.base.BaseViewFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.c001apk.logic.model.MessageResponse
import com.example.c001apk.ui.base.Searchable
import com.example.c001apk.ui.base.hit

@AndroidEntryPoint
class MessageContentFragment : BaseViewFragment<MessageViewModel>(), Searchable {

    @Inject
    lateinit var viewModelAssistedFactory: MessageViewModel.Factory
    override val viewModel by viewModels<MessageViewModel> {
        MessageViewModel.provideFactory(
            viewModelAssistedFactory,
            arguments?.getString("type").orEmpty()
        )
    }
    private lateinit var messAdapter: MessageContentAdapter
    private lateinit var footerAdapter: FooterAdapter

    companion object {
        @JvmStatic
        fun newInstance(type: String) =
            MessageContentFragment().apply {
                arguments = Bundle().apply {
                    putString("type", type)
                }
            }
    }

    override fun initAdapter() {
        messAdapter = MessageContentAdapter(viewModel.type, object : ItemListener {})
        footerAdapter = FooterAdapter(ReloadListener())
        mAdapter = ConcatAdapter(HeaderAdapter(), messAdapter, footerAdapter)
    }

    inner class ReloadListener : FooterAdapter.FooterListener {
        override fun onReLoad() {
            viewModel.isEnd = false
            loadMore()
        }
    }

    override fun initObserve() {
        super.initObserve()
        viewModel.messageListData.observe(viewLifecycleOwner) { showList(it) }

        viewModel.footerState.observe(viewLifecycleOwner) {
            footerAdapter.setLoadState(it)
            if (it !is FooterState.Loading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    private fun showList(all: List<MessageResponse.Data>) {
        val k = viewModel.kw
        val l = if (k.isEmpty()) all else all.filter {
            it.note.hit(k) || it.message.hit(k) || it.fromusername.hit(k)
                    || it.username.hit(k) || it.title.hit(k) || it.infoHtml.hit(k)
        }
        viewModel.listSize = all.size
        messAdapter.submitList(l)
        autoMore(l.size)
    }

    override fun search(k: String) {
        viewModel.kw = k
        if (view != null && !viewModel.isInit) showList(viewModel.messageListData.value.orEmpty())
    }}
