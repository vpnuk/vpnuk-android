/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageWebsites

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.databinding.ActivityManageWebsitesBinding
import uk.vpn.vpnuk.utils.observeFlow
import uk.vpn.vpnuk.utils.setText


class ManageWebsitesActivity : BaseActivity() {

    private lateinit var bind: ActivityManageWebsitesBinding
    private val vm: ManageWebsitesViewModel by viewModels()

    private lateinit var domainAdapter: WebsitesRecyclerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityManageWebsitesBinding.inflate(layoutInflater)
        setContentView(bind.root)
        supportActionBar?.title = "VPN connections for websites"

        vm.getExcludedWebsites()

        initView()
        observeData()
    }

    private fun observeData() {
        observeFlow(vm.viewState, ::render)
    }

    private fun render(viewState: ManageWebsitesViewModel.ManageWebsitesViewState) {
        initRecycler(viewState.excludedWebsitesList)

        bind.etDomain.setText(viewState.domainText)
    }

    private fun initRecycler(excludedWebsitesList: List<String>) {
        domainAdapter = WebsitesRecyclerAdapter(excludedWebsitesList.toMutableList()) { domain, position ->
            vm.onDeleteWebsiteClick(domain)

            domainAdapter.items.remove(domain)
            domainAdapter.notifyItemRemoved(position)
            //Got fake "position" from adapter. After animation notify about new items
            lifecycleScope.launch {
                delay(800)
                domainAdapter.notifyDataSetChanged()
            }
        }

        bind.rvDomainsList.adapter = domainAdapter
    }

    private fun initView() {
        bind.buttonAddWebsite.setOnClickListener { vm.onAddWebsiteClick() }

        bind.etDomain.editText?.doOnTextChanged { text, _, _, _ -> vm.onDomainTextChanged(text) }
    }
}