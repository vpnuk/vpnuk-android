/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.functions.Consumer
import uk.vpn.vpnuk.databinding.ActivityServerListBinding
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.ServerType
import uk.vpn.vpnuk.utils.doOnIoObserveOnMain
import uk.vpn.vpnuk.utils.setTabListener
import uk.vpn.vpnuk.utils.setTabs
import uk.vpn.vpnuk.ui.ServersAdapter

class ServerListActivity : BaseActivity() {

    private lateinit var bind: ActivityServerListBinding

    private lateinit var repository: Repository
    private lateinit var serversAdapter: ServersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityServerListBinding.inflate(layoutInflater)
        setContentView(bind.root)
        repository = Repository.instance(this@ServerListActivity)
        initViews()
        bind.tabTypes.getTabAt(0)!!.select()
    }

    private fun initViews() {
        bind.tabTypes.setTabs(ServerType.values().map { getString(it.nameRes) })
        bind.tabTypes.setTabListener { _, position ->
            loadServers(ServerType.values()[position])
        }
        serversAdapter = ServersAdapter(this) {
            repository.setServerId(it.address)
                .doOnIoObserveOnMain()
                .subscribe {
                    finish()
                }.addToDestroySubscriptions()
        }
        bind.rvList.layoutManager = LinearLayoutManager(this)
        bind.rvList.adapter = serversAdapter
    }

    private fun loadServers(serverType: ServerType) {
        repository
            .getServersCache()
            .map { servers -> servers.filter { it.type == serverType.value } }
            .doOnIoObserveOnMain()
            .addProgressTracking()
            .subscribe(Consumer {
                serversAdapter.updateData(it)
            })
            .addToDestroySubscriptions()
    }
}
