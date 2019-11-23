package uk.vpn.vpnuk

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_server_list.*
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.ServerType
import uk.vpn.vpnuk.utils.doOnIoSubscribeOnMain
import uk.vpn.vpnuk.utils.setTabListener
import uk.vpn.vpnuk.utils.setTabs
import uk.vpn.vpnuk.view.ServersAdapter

class ServerListActivity : BaseActivity() {

    private lateinit var repository: Repository

    private lateinit var serversAdapter: ServersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)
        repository = Repository.instance(this@ServerListActivity)
        initViews()
        tabTypes.getTabAt(0)!!.select()
    }

    private fun initViews() {
        tabTypes.setTabs(ServerType.values().map { it.value })
        tabTypes.setTabListener {
            loadServers(ServerType.byValue(it)!!)
        }
        serversAdapter = ServersAdapter(this) {
            repository.setServerId(it.address)
                .doOnIoSubscribeOnMain()
                .subscribe {
                    finish()
                }
        }
        rvList.layoutManager = LinearLayoutManager(this)
        rvList.adapter = serversAdapter
    }

    private fun loadServers(serverType: ServerType) {
        repository
            .getServersCache()
            .map { servers -> servers.filter { it.type == serverType.value } }
            .doOnIoSubscribeOnMain()
            .addProgressTracking()
            .subscribe(Consumer {
                serversAdapter.updateData(it)
            })
            .addToDestroySubscriptions()
    }
}
