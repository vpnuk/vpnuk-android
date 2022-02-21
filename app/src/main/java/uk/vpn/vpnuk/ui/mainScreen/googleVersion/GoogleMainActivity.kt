/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.googleVersion

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_google_main.*
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.ui.settingsScreen.SettingsActivity
import uk.vpn.vpnuk.utils.*
import android.net.Uri


class GoogleMainActivity : BaseActivity(), ConnectionStateListener {

    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val vm: GoogleMainVM by viewModels()

    private val SETTINGS_SCREEN_CODE = 3332

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_main)
        supportActionBar?.show()
        supportActionBar?.title = ""

        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initViews()
        applySettings()
        observeLiveData()
        updateVpnServers()
    }

    private fun observeLiveData() {

    }

    private fun updateVpnServers() {
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

    private fun selectNewServer() {
        val ip = LocalRepository(this).vpnIp
        repository.setServerId(ip)
            .doOnIoObserveOnMain()
            .subscribe {}.addToDestroySubscriptions()
    }

    private fun applySettings() {
        val settings = repository.getSettings()
        settings.credentials?.let {
            vGoogleMainActivityLogin.setText(it.login)
            vGoogleMainActivityPassword.setText(it.password)
        }
        vGoogleMainActivityCheckSaveCredentials.isChecked = settings.credentials != null
    }

    private fun initViews() {
        vGoogleMainActivityButtonConnect.setOnClickListener {
            startVpn()
        }
        vGoogleMainActivityLinkTrial.stripUnderlines()
        vGoogleMainActivityLinkTrial.setOnClickListener {
            val url = "https://www.vpnuk.net/product-category/free-trial/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        vGoogleMainActivityButtonSelectAddress.setOnClickListener {
            startActivity(Intent(this, ServerListActivity::class.java))
        }
        vGoogleMainActivityButtonDisconnect.setOnClickListener {
            vpnConnector.stopVpn()
        }

        repository.getCurrentServerObservable()
            .observeOnMain()
            .subscribe { server ->
                Logger.e("subscribe", "$server")
                server.server?.let {
                    vGoogleMainActivityTextAddress.text = it.dns
                    vGoogleMainActivityTextAddress.visibility = View.VISIBLE
                    vGoogleMainActivityTextCity.text = it.location.city
                    vGoogleMainActivityImageViewCountry.setImageResource(it.getIconResourceName(this))
                } ?: run {
                    vGoogleMainActivityTextAddress.visibility = View.GONE
                    vGoogleMainActivityImageViewCountry.setImageResource(R.drawable.ic_country)
                    vGoogleMainActivityTextCity.setText(R.string.select_city)
                }
            }.addToDestroySubscriptions()

        Observable.combineLatest(
            vGoogleMainActivityPassword.textEmpty(),
            vGoogleMainActivityLogin.textEmpty(),
            repository.getCurrentServerObservable(),
            { passwordEmpty, loginEmpty, server ->
                Logger.e("subscribe", "enabled $server")
                !passwordEmpty && !loginEmpty && server.server != null
            })
            .observeOnMain()
            .subscribe {
                vGoogleMainActivityButtonConnect.isEnabled = it
            }.addToDestroySubscriptions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_action_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_SCREEN_CODE)
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SETTINGS_SCREEN_CODE){
            if(resultCode == RESULT_OK){
                vpnConnector.stopVpn()

                applySettings()
                initViews()
                selectNewServer()
            }
        }
    }

    private fun startVpn(){
        val login = vGoogleMainActivityLogin.text.toString()
        val password = vGoogleMainActivityPassword.text.toString()

        val credentials = Credentials(login, password)
        val address = repository.getSelectedServer()!!.address
        val settings = repository.getSettings()
        val socket = settings.socket
        val port = settings.port

        repository.updateSettings(settings.copy(credentials = credentials))

        val vpnLogin = repository.getSettings().credentials?.login
        val vpnPassword = repository.getSettings().credentials?.password

        vpnConnector.startVpn(
            vpnLogin,
            vpnPassword,
            address,
            socket,
            port,
            settings.mtu ?: DefaultSettings.MTU_DEFAULT
        )
    }

    override fun onStateChanged(state: ConnectionState) {
        vGoogleMainActivityTextStatus.setText(state.nameId)
        vGoogleMainActivityTextStatus.setTextColor(state.color(this))

        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                vGoogleMainActivityButtonConnect.visibility = View.VISIBLE
                vGoogleMainActivityButtonDisconnect.visibility = View.GONE
                vGoogleMainActivityButtonSelectAddress.isEnabled = true
            }
            else -> {
                vGoogleMainActivityButtonConnect.visibility = View.GONE
                vGoogleMainActivityButtonDisconnect.visibility = View.VISIBLE
                vGoogleMainActivityButtonSelectAddress.isEnabled = false
            }
        }
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