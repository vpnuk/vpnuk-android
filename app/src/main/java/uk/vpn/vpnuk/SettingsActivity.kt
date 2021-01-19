/*
 * Copyright (c) 2020 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_settings.*
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.*
import java.io.BufferedReader
import java.io.InputStreamReader


class SettingsActivity : AppCompatActivity() {

    private var dialog: AlertDialog? = null
    private lateinit var repository: Repository
    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = getString(R.string.Settings)

        repository = Repository(this)

        initViews()
        applySettings()
    }


    private fun initViews() {
        settings = repository.getSettings()

        tabsSocketType.setTabs(SocketType.values().map { it.value })
        tabsPort.setTabListener { _, _ ->
            repository.updateSettings(settings.copy(
                socket = tabsSocketType.selectedTab().text.toString(),
                port = tabsPort.selectedTab().text.toString()
            ))
        }
        tabsSocketType.setTabListener { text, _ ->
            tabsPort.setTabs(SocketType.byValue(text)!!.ports)
            repository.updateSettings(settings.copy(
                socket = tabsSocketType.selectedTab().text.toString(),
                port = tabsPort.selectedTab().text.toString()
            ))
        }

        cbMtu.post {
            cbMtu.setOnCheckedChangeListener { _, checked ->
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

        cbReconnect.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                repository.updateSettings(settings.copy(reconnect = true))
            } else {
                repository.updateSettings(settings.copy(reconnect = false))
            }
        }

        if(Logger.vpnLogs.size > 300){
            Logger.vpnLogs.clear()
        }
        for(i in Logger.vpnLogs.indices){
            textView_logs.append(Logger.vpnLogs[i] + "\n")
        }
    }

    private fun applySettings() {
        settings = repository.getSettings()

        val socketType = SocketType.byValue(settings.socket)!!
        val portIndex = socketType.ports.indexOf(settings.port)
        val socketIndex = SocketType.values().indexOf(socketType)
        tabsSocketType.select(socketIndex)
        tabsPort.select(portIndex)
        cbReconnect.isChecked = settings.reconnect
        cbMtu.isChecked = settings.mtu?.let { it != DefaultSettings.MTU_DEFAULT } ?: false
    }

    private fun removeMtu() {
        repository.updateSettings(
            repository.getSettings().copy(
                mtu = null
            )
        )
    }
}
