/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.googleVersion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import io.reactivex.Observable
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.ui.settingsScreen.SettingsActivity
import uk.vpn.vpnuk.utils.*
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.RuntimePermissions
import uk.vpn.vpnuk.databinding.ActivityGoogleMainBinding
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.ui.dialog.NotificationExplanationDialog
import uk.vpn.vpnuk.ui.serverListScreen.ServerListActivity


@RuntimePermissions
class GoogleMainActivity : BaseActivity(), ConnectionStateListener {

    lateinit var bind: ActivityGoogleMainBinding

    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val vm: GoogleMainVM by viewModels()

    private val SETTINGS_SCREEN_CODE = 3332

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityGoogleMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        supportActionBar?.show()
        supportActionBar?.title = ""

        vpnConnector = VpnConnector(this)

        initViews()
        applySettings()
        observeLiveData()

        localRepository.isLoginByUserCreds = false

        vm.updateServers()

        requestPostNotificationPermissionWithPermissionCheck()
    }

    private fun observeLiveData() {

    }

    private fun selectNewServer() {
        val ip = LocalRepository(this).vpnIp
    }

    private fun applySettings() {
        val settings = localRepository.settings
        settings?.credentials?.let {
            bind.vGoogleMainActivityLogin.setText(it.login)
            bind.vGoogleMainActivityPassword.setText(it.password)
        }
        bind.vGoogleMainActivityCheckSaveCredentials.isChecked = settings?.credentials != null
    }

    private fun initViews() {
        bind.vGoogleMainActivityButtonConnect.setOnClickListener {
            if(PermissionUtils.hasSelfPermissions(this, Manifest.permission.POST_NOTIFICATIONS)){
                startVpn()
            }else{
                requestPostNotificationPermissionWithPermissionCheck()
            }
        }
        bind.vGoogleMainActivityLinkTrial.stripUnderlines()
        bind.vGoogleMainActivityLinkTrial.setOnClickListener {
            val url = "https://www.vpnuk.net/product-category/free-trial/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        bind.vGoogleMainActivityButtonSelectAddress.setOnClickListener {
            startActivity(Intent(this, ServerListActivity::class.java))
        }
        bind.vGoogleMainActivityButtonDisconnect.setOnClickListener {
            vpnConnector.stopVpn()
        }


        //TODO _____________________________________________________
        val server = localRepository.currentServer
        Log.d("kek", "GoogleMainActivity InitView server = ${server}")
        if(server != null){
            bind.vGoogleMainActivityTextAddress.text = server.dns
            bind.vGoogleMainActivityTextAddress.visibility = View.VISIBLE
            bind.vGoogleMainActivityTextCity.text = server.location?.city
            bind.vGoogleMainActivityImageViewCountry.setImageDrawable(server.getIsoDrawable(this))
        }else{
            bind.vGoogleMainActivityTextAddress.visibility = View.GONE
            bind.vGoogleMainActivityImageViewCountry.setImageResource(R.drawable.ic_country)
            bind.vGoogleMainActivityTextCity.setText(R.string.select_city)
        }
        if(!bind.vGoogleMainActivityPassword.text.isNullOrEmpty() && !bind.vGoogleMainActivityLogin.text.isNullOrEmpty() && server != null){
            bind.vGoogleMainActivityButtonConnect.isEnabled = true
        }
        //TODO ________________________________________________________

        //repository.getCurrentServerObservable()
        //    .observeOnMain()
        //    .subscribe { server ->
        //        Logger.e("subscribe", "$server")
        //        server.server?.let {
        //            bind.vGoogleMainActivityTextAddress.text = it.dns
        //            bind.vGoogleMainActivityTextAddress.visibility = View.VISIBLE
        //            bind.vGoogleMainActivityTextCity.text = it.location?.city
        //            bind.vGoogleMainActivityImageViewCountry.setImageDrawable(it.getIsoDrawable(this))
        //        } ?: run {
        //            bind.vGoogleMainActivityTextAddress.visibility = View.GONE
        //            bind.vGoogleMainActivityImageViewCountry.setImageResource(R.drawable.ic_country)
        //            bind.vGoogleMainActivityTextCity.setText(R.string.select_city)
        //        }
        //    }.addToDestroySubscriptions()


        //Observable.combineLatest(
        //    bind.vGoogleMainActivityPassword.textEmpty(),
        //    bind.vGoogleMainActivityLogin.textEmpty(),
        //    repository.getCurrentServerObservable()
        //) { passwordEmpty, loginEmpty, server ->
        //    Logger.e("subscribe", "enabled $server")
        //    !passwordEmpty && !loginEmpty && server.server != null
        //}
        //    .observeOnMain()
        //    .subscribe {
        //        bind.vGoogleMainActivityButtonConnect.isEnabled = it
        //    }.addToDestroySubscriptions()
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
        val login = bind.vGoogleMainActivityLogin.text.toString()
        val password = bind.vGoogleMainActivityPassword.text.toString()

        val credentials = Credentials(login, password)
        val address = localRepository.currentServer?.address
        val settings = localRepository.settings
        val socket = settings?.socket
        val port = settings?.port

        localRepository.settings = localRepository.settings?.copy(credentials = credentials)

        val vpnLogin = localRepository.settings?.credentials?.login
        val vpnPassword = localRepository.settings?.credentials?.password

        vpnConnector.startVpn(
            vpnLogin,
            vpnPassword,
            address,
            socket,
            port,
            settings?.mtu ?: DefaultSettings.MTU_DEFAULT,
            localRepository.customDns,
            localRepository.excludedApps,
            localRepository.excludedWebsites,
            localRepository.useObfuscation
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun requestPostNotificationPermission(){
        Log.d("kek", "REQUESTING PERMISSIONS")
    }

    @OnNeverAskAgain(Manifest.permission.POST_NOTIFICATIONS)
    fun onNotificationsNeverAskAgain() {
        val dialog = NotificationExplanationDialog()
        dialog.show(supportFragmentManager, "dummy")
    }

    override fun onStateChanged(state: ConnectionState) {
        bind.vGoogleMainActivityTextStatus.setText(state.nameId)
        bind.vGoogleMainActivityTextStatus.setTextColor(state.color(this))

        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                bind.vGoogleMainActivityButtonConnect.visibility = View.VISIBLE
                bind.vGoogleMainActivityButtonDisconnect.visibility = View.GONE
                bind.vGoogleMainActivityButtonSelectAddress.isEnabled = true
            }
            else -> {
                bind.vGoogleMainActivityButtonConnect.visibility = View.GONE
                bind.vGoogleMainActivityButtonDisconnect.visibility = View.VISIBLE
                bind.vGoogleMainActivityButtonSelectAddress.isEnabled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)
        initViews()
    }
    override fun onPause() {
        vpnConnector.removeListener()
        super.onPause()
    }
}