package uk.vpn.vpnuk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_server_list.*
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.view.ServersAdapter

class ServerListActivity : AppCompatActivity() {

    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)
        repository = Repository.instance(this@ServerListActivity)
        rvList.layoutManager = LinearLayoutManager(this)
        rvList.adapter = ServersAdapter(this) {
            repository.setServerId(it.address!!)
            finish()
        }.apply {
            updateData(repository.getServers())
        }
    }
}
