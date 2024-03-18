/*
 * Copyright (c) 2023 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.ui.serverListScreen

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.onEach
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.databinding.ActivityServerListBinding
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Server
import uk.vpn.vpnuk.remote.ServerVersion
import uk.vpn.vpnuk.utils.ServerType
import uk.vpn.vpnuk.utils.doOnIoObserveOnMain
import uk.vpn.vpnuk.utils.setTabListener
import uk.vpn.vpnuk.utils.setTabs
import uk.vpn.vpnuk.ui.ServersAdapter
import uk.vpn.vpnuk.utils.launchWhenCreated

class ServerListActivity : BaseActivity() {

    private lateinit var bind: ActivityServerListBinding
    private val vm: ServerListViewModel by viewModels()

    private lateinit var repository: Repository
    private lateinit var serversAdapter: ServersAdapter
    private var serverList = listOf<Server>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityServerListBinding.inflate(layoutInflater)
        setContentView(bind.root)
        repository = Repository.instance(this@ServerListActivity)

        vm.getServerList()

        initViews()
        observeData()

        bind.tabTypes.getTabAt(0)!!.select()
    }

    private fun observeData() {
        vm.viewState.onEach { render(it) }.launchWhenCreated(lifecycleScope)
    }

    private fun render(viewState: ServerListViewModel.ViewState) {
        Log.d("kek", "Render, serverList = ${viewState.serverList}")
        if(viewState.serverList.isNotEmpty()) {
            serverList = viewState.serverList
            loadServers(ServerType.values()[0])
        }
    }

    private fun initViews() {
        Log.d("kek", "serverList - ${localRepository.serversList.map { it.address }}, size - ${localRepository.serversList.size}")


        serversAdapter = ServersAdapter(this) {
            localRepository.currentServer = it

            repository.setServerId(it.address)
                .doOnIoObserveOnMain()
                .subscribe {
                    finish()
                }.addToDestroySubscriptions()
        }
        bind.rvList.layoutManager = LinearLayoutManager(this)
        bind.rvList.adapter = serversAdapter

        loadServers(ServerType.values()[0])

        bind.tabTypes.setTabs(ServerType.values().map { getString(it.nameRes) })
        bind.tabTypes.setTabListener { _, position ->
            loadServers(ServerType.values()[position])
        }
    }

    private fun loadServers(serverType: ServerType) {
        Log.d("kek", "load servers. before filter - ${localRepository.serversList}")
        val servers = serverList.filter { it.type == serverType.value }
        Log.d("kek", "servers after filter - ${servers.size}")
        serversAdapter.updateData(servers)
    }
}
