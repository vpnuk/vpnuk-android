/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.reactivex.Observable
import io.reactivex.functions.Function3
import kotlinx.android.synthetic.main.activity_main.*
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*


class MainActivity : BaseActivity(), ConnectionStateListener {
    private var dialog: AlertDialog? = null
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
        cbMtu.isChecked = settings.mtu?.let { it != DefaultSettings.MTU_DEFAULT } ?: false
    }

    private fun initViews() {
        tvLinkTrial.movementMethod = LinkMovementMethod.getInstance()
        tvLinkTrial.stripUnderlines()
        tabsSocketType.setTabs(SocketType.values().map { it.value })
        tabsSocketType.setTabListener { text, _ ->
            tabsPort.setTabs(SocketType.byValue(text)!!.ports)
        }
        vSelectAddress.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerListActivity::class.java))
        }
        cbMtu.post {
            cbMtu.setOnCheckedChangeListener { button, checked ->
                if (checked) {
                    dialog = AlertDialog.Builder(this)
                        .setItems(
                            DefaultSettings.MTU_LIST
                        ) { _, i ->
                            repository.updateSettings(
                                repository.getSettings().copy(
                                    mtu = DefaultSettings.MTU_LIST[i]
                                )
                            )
                        }
                        .setTitle(getString(R.string.custom_mtu))
                        .setOnCancelListener {
                            cbMtu.isChecked = false
                        }
                        .create().apply {
                            show()
                        }
                } else {
                    removeMtu()
                }
            }
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
            val settings = repository.getSettings()
            repository.updateSettings(
                settings.copy(
                    socket = socket,
                    port = port,
                    reconnect = reconnect,
                    credentials = credentials
                )
            )
            vpnConnector.startVpn(
                login,
                password,
                address,
                socket,
                port,
                settings.mtu ?: DefaultSettings.MTU_DEFAULT
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
                } ?: run {
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

    private fun removeMtu() {
        repository.updateSettings(
            repository.getSettings().copy(
                mtu = null
            )
        )
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
        dialog?.dismiss()
        super.onDestroy()
    }
}
