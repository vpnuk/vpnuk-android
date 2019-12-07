/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.crashlytics.android.Crashlytics
import io.reactivex.Observable
import io.reactivex.functions.Function3
import kotlinx.android.synthetic.main.activity_main.*
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

class MainActivity : BaseActivity(), ConnectionStateListener {
    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    override fun onStateChanged(state: ConnectionState) {
        tvStatus.setText(state.nameId)
        tvStatus.setTextColor(state.color(this))
        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                btConnect.visibility = View.VISIBLE
                btDisconnect.visibility = View.GONE
                vSelectAddress.isEnabled = true
            }
            else -> {
                btConnect.visibility = View.GONE
                btDisconnect.visibility = View.VISIBLE
                vSelectAddress.isEnabled = false
            }
        }
    }

    override fun showProgress() {
        content.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        content.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)
        initViews()
        applySettings()
        if (!repository.serversUpdated) {
            repository.updateServers()
                .doOnIoObserveOnMain()
                .addProgressTracking()
                .subscribe({}, {
                    showMessage(getString(R.string.err_unable_to_update_servers))
                })
                .addToDestroySubscriptions()
        }
    }

    private fun applySettings() {
        val settings = repository.getSettings()

        val socketType = SocketType.byValue(settings.socket)!!
        val portIndex = socketType.ports.indexOf(settings.port)
        val socketIndex = SocketType.values().indexOf(socketType)
        tabsSocketType.select(socketIndex)
        tabsPort.select(portIndex)
        settings.credentials?.let {
            etLogin.setText(it.login)
            etPassword.setText(it.password)
        }
        cbSaveCredentials.isChecked = settings.credentials != null
        cbReconnect.isChecked = settings.reconnect
    }

    private fun initViews() {
        tabsSocketType.setTabs(SocketType.values().map { it.value })
        tabsSocketType.setTabListener { text, _ ->
            tabsPort.setTabs(SocketType.byValue(text)!!.ports)
        }
        vSelectAddress.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerListActivity::class.java))
        }

        btConnect.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()
            val credentials: Credentials? =
                if (cbSaveCredentials.isChecked) Credentials(login, password) else null
            val socket = tabsSocketType.selectedTab().text.toString()
            val port = tabsPort.selectedTab().text.toString()
            val reconnect = cbReconnect.isChecked

            val address = repository.getSelectedServer()!!.address
            repository.updateSettings(Settings(socket, port, reconnect, credentials))
            vpnConnector.startVpn(
                login,
                password,
                address,
                socket,
                port
            )
        }
        btDisconnect.setOnClickListener {
            vpnConnector.stopVpn()
        }

        repository.getCurrentServerObservable()
            .observeOnMain()
            .subscribe { server ->
                Logger.e("subscribe", "$server")
                server.server?.let {
                    tvAddress.text = it.dns
                    tvAddress.visibility = View.VISIBLE
//                    tvDns.text = it.dns
                    tvCity.text = it.location.city
                    ivCountry.setImageResource(it.getIconResourceName(this))
                }?:run {
                    tvAddress.visibility = View.GONE
                    ivCountry.setImageResource(R.drawable.ic_country)
                    tvCity.setText(R.string.select_city)
                }
            }.addToDestroySubscriptions()

        Observable.combineLatest(
            etPassword.textEmpty(),
            etLogin.textEmpty(),
            repository.getCurrentServerObservable(),
            Function3<Boolean, Boolean, Wrapper, Boolean> { passwordEmpty, loginEmpty, server ->
                Logger.e("subscribe", "enabled $server")
                !passwordEmpty && !loginEmpty && server.server != null
            })
            .observeOnMain()
            .subscribe {
                btConnect.isEnabled = it
            }.addToDestroySubscriptions()
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)
    }

    override fun onPause() {
        vpnConnector.removeListener()
        super.onPause()
    }
}
