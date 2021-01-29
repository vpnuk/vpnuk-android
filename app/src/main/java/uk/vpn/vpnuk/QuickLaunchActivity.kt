/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_quick_launch.*
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import io.reactivex.functions.Function3
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*

class QuickLaunchActivity : BaseActivity(), ConnectionStateListener {

    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_launch)

        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initView()

        if (!repository.serversUpdated) {
            repository.updateServers()
                .doOnIoObserveOnMain()
                .addProgressTracking()
                .subscribe({}, { error ->
                    showMessage(getString(R.string.err_unable_to_update_servers))
                })
                .addToDestroySubscriptions()
        }
    }

    private fun initView() {
        supportActionBar?.hide()

        switch_connect.setOnCheckedChangeListener { view, isChecked ->
            if(isChecked){
                if(checkData()){
                    val address = repository.getSelectedServer()?.address
                    val settings = repository.getSettings()
                    val login = settings.credentials?.login
                    val password = settings.credentials?.password

                    val socket = settings.socket
                    val port = settings.port

                    vpnConnector.startVpn(
                        login,
                        password,
                        address,
                        socket,
                        port,
                        settings.mtu ?: DefaultSettings.MTU_DEFAULT
                    )
                }
            }else{
                vpnConnector.stopVpn()
            }
        }
        imageView_connection_configure.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkData() : Boolean {
        val settings = repository.getSettings()

        if(repository.getSelectedServer() == null){
            Toasty.error(this, "You have not selected a server", Toasty.LENGTH_SHORT).show()
            return false
        }else if(settings.credentials == null){
            Toasty.error(this, "You have not entered credentials", Toasty.LENGTH_SHORT).show()
            return false
        }else{
            return true
        }
    }

    override fun onStateChanged(state: ConnectionState) {

        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                switch_connect.isChecked = false
                textView_state.text = "Disconnected"
            }
            ConnectionState.LEVEL_START ->{
                textView_state.text = "Connecting"
            }
            ConnectionState.LEVEL_CONNECTED -> {
                switch_connect.isChecked = true
                textView_state.text = "Connected"
            }
        }
    }

    override fun showProgress() {
        switch_connect.visibility = View.GONE
        progressBarQuick.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        switch_connect.visibility = View.VISIBLE
        progressBarQuick.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)
    }

    override fun onPause() {
        vpnConnector.removeListener()
        super.onPause()
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}