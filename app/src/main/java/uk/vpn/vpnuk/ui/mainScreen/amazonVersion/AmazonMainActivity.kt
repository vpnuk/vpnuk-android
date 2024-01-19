/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.amazonVersion

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import io.reactivex.functions.Function3
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.ui.adapter.vpnAccountAdapter.VpnAccountAdapter
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.databinding.ActivityAmazonMainBinding
import uk.vpn.vpnuk.databinding.DialogChooseVpnaccountBinding
import uk.vpn.vpnuk.databinding.DialogFreeTrialBinding
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*
import uk.vpn.vpnuk.ui.registerAccountScreen.RegisterAccountActivity
import uk.vpn.vpnuk.ui.serverListScreen.ServerListActivity
import uk.vpn.vpnuk.ui.settingsScreen.SettingsActivity


class AmazonMainActivity : BaseActivity(), ConnectionStateListener {

    private lateinit var bind: ActivityAmazonMainBinding

    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val vm: AmazonMainVM by viewModels()

    val SETTINGS_SCREEN_CODE = 3332
    val REGISTER_AMAZON_ACCOUNT_SCREEN_CODE = 123


    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityAmazonMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        supportActionBar?.show()
        supportActionBar?.title = ""

        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initViews()
        applySettings()
        stFreeTrialAccountVisibility()
        observeLiveData()

        vm.updateServers()
    }

    private fun observeLiveData() {
        vm.vpnAccounts.observe(this) {
            Log.d("kek", "vpnAccounts - observe")
            hideProgressBar()

            if (it.isEmpty()) {
                Toasty.error(this, getString(R.string.error_you_dont_have_active_vpn_account))
                    .show()
            } else if (it.size == 1) {
                Log.d("kek", "vpnAccounts size = 1%%% ${it[0].username.toString()}")
                localRepository.vpnUsername = it[0].username.toString()
                localRepository.vpnPassword = it[0].password.toString()
                localRepository.vpnServerName =
                    it[0].server?.description.toString().split("Server:")[1].split("<")[0]
                localRepository.vpnIp = it[0].server?.ip.toString()
                localRepository.vpnDescription = it[0].server?.description.toString()
                localRepository.purchasedSubId = it[0].subscriptionId

                selectNewServer()
                startVpn()

                showExplainingDialog()
            } else if (it.size > 1) {
                showChooseVpnAccountDialog(it)
            }
        }
        vm.error.observe(this){
            hideProgressBar()
        }
    }

    private fun showChooseVpnAccountDialog(list: List<Vpnaccount>){
        Log.d("kek", "ALERTDIALOG - showChooseVpnAccountDialog")

        val alertDialog = AlertDialog.Builder(this).create()
        //val customLayout: View = layoutInflater.inflate(R.layout.dialog_choose_vpnaccount, null)
        val dialogBind = DialogChooseVpnaccountBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(dialogBind.root)

        val adapter = VpnAccountAdapter(this, list){
            localRepository.vpnUsername = it.username.toString()
            localRepository.vpnPassword = it.password.toString()
            localRepository.vpnServerName = it.server?.description.toString().split("Server:")[1].split("<")[0]
            localRepository.vpnIp = it.server?.ip.toString()
            localRepository.vpnDescription = it.server?.description.toString()
            localRepository.purchasedSubId = it.subscriptionId

            selectNewServer()
            startVpn()

            alertDialog.dismiss()

            showExplainingDialog()
        }
        dialogBind.vChooseVpnAccountDialogRecycler.layoutManager = LinearLayoutManager(this)
        dialogBind.vChooseVpnAccountDialogRecycler.adapter = adapter

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showExplainingDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.server_selected))
        alertDialog.setMessage("Your server has been selected automatically!\n\n${Html.fromHtml(localRepository.vpnDescription)}")
        alertDialog.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
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
            bind.etLogin.setText(it.login)
            bind.etPassword.setText(it.password)
        }
        bind.cbSaveCredentials.isChecked = settings.credentials != null
    }

    private fun onUserCredsClick() {
        localRepository.isLoginByUserCreds = true
        bind.etLoginBox.hint = "User account"
        bind.etPasswordBox.hint = "User password"
    }

    private fun onVpnCredsClick() {
        localRepository.isLoginByUserCreds = false
        bind.etLoginBox.hint = "Vpn username"
        bind.etPasswordBox.hint = "Vpn password"
    }

    private fun initViews() {
        //temp//////////////////////////////   CONNECT CLICK   ////////////////////////////////////
        bind.btConnect.setOnClickListener {
            val login = bind.etLogin.text.toString()
            val password = bind.etPassword.text.toString()

            val isLoginByUserCreds = localRepository.isLoginByUserCreds

            if(!isLoginByUserCreds){ //Login by VPN
                startVpn()
            }else{                  //Login by User Creds
                if(localRepository.vpnUsername == ""){
                    vm.findActiveVpnAccount(login, password)
                    showProgressBar()
                }else if(login != localRepository.initialUserName){
                    vm.findActiveVpnAccount(login, password)
                    showProgressBar()
                }else{
                    startVpn()
                }
            }
        }
        //temp//////////////////////////////   CONNECT CLICK   ////////////////////////////////////

        bind.tbUserCreds.setOnClickListener { onUserCredsClick() }
        bind.tbVpnCreds.setOnClickListener { onVpnCredsClick() }
        if(localRepository.isLoginByUserCreds) {
            bind.tgCredentialsType.check(R.id.tbUserCreds)
            onUserCredsClick()
        } else{
            bind.tgCredentialsType.check(R.id.tbVpnCreds)
            onVpnCredsClick()
        }


        bind.tvLinkTrial.stripUnderlines()
        bind.tvLinkTrial.setOnClickListener {
            val intent = Intent(this, RegisterAccountActivity::class.java)
            startActivityForResult(intent, REGISTER_AMAZON_ACCOUNT_SCREEN_CODE)
        }
        bind.vSelectAddress.setOnClickListener {
            startActivity(Intent(this@AmazonMainActivity, ServerListActivity::class.java))
        }
        bind.btDisconnect.setOnClickListener {
            vpnConnector.stopVpn()
        }

        repository.getCurrentServerObservable()
            .observeOnMain()
            .subscribe { server ->
                Logger.e("subscribe", "$server")
                server.server?.let {
                    bind.tvAddress.text = it.dns
                    bind.tvAddress.visibility = View.VISIBLE
                    bind.tvCity.text = it.location?.city
                    bind.ivCountry.setImageDrawable(it.getIsoDrawable(this))
                } ?: run {
                    bind.tvAddress.visibility = View.GONE
                    bind.ivCountry.setImageResource(R.drawable.ic_country)
                    bind.tvCity.setText(R.string.select_city)
                }
            }.addToDestroySubscriptions()

        Observable.combineLatest(
            bind.etPassword.textEmpty(),
            bind.etLogin.textEmpty(),
            repository.getCurrentServerObservable(),
            Function3<Boolean, Boolean, Wrapper, Boolean> { passwordEmpty, loginEmpty, server ->
                Logger.e("subscribe", "enabled $server")
                !passwordEmpty && !loginEmpty && server.server != null
            })
            .observeOnMain()
            .subscribe {
                bind.btConnect.isEnabled = it
            }.addToDestroySubscriptions()
    }

    private fun isFirstLoginAttempt(): Boolean {
        return localRepository.vpnUsername == ""
    }

    private fun stFreeTrialAccountVisibility() {
        if(localRepository.vpnIp != ""){
            bind.tvLinkTrial.visibility = View.GONE
        }else{
            bind.tvLinkTrial.visibility = View.VISIBLE
        }
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
        }else if(requestCode == REGISTER_AMAZON_ACCOUNT_SCREEN_CODE){
            if(resultCode == RESULT_OK && data != null){
                val subscriptionModel = data.getParcelableExtra<SubscriptionsModel>(CREATED_SUBSCRIPTION)

                applySettings()
                initViews()
                selectNewServer()
                stFreeTrialAccountVisibility()

                showSubscriptionInfoDialog(subscriptionModel)
            }
        }
    }

    private fun startVpn() {
        val login = bind.etLogin.text.toString()
        val password = bind.etPassword.text.toString()

        //Check if checkbox checked...Removed because amazon iap flow
        val credentials = Credentials(login, password)
        val address = repository.getSelectedServer()!!.address
        val settings = repository.getSettings()
        val socket = settings.socket
        val port = settings.port

        repository.updateSettings(settings.copy(credentials = credentials))

        val isLoginByUserCreds = localRepository.isLoginByUserCreds
        val vpnLogin = if(isLoginByUserCreds) localRepository.vpnUsername else login
        val vpnPassword = if(isLoginByUserCreds) localRepository.vpnPassword else password

        vpnConnector.startVpn(
            vpnLogin,
            vpnPassword,
            address,
            socket,
            port,
            settings.mtu ?: DefaultSettings.MTU_DEFAULT,
            localRepository.customDns
        )
    }

    private fun showSubscriptionInfoDialog(subscriptionModel: SubscriptionsModel?) {
        val alertDialog = AlertDialog.Builder(this)
        //val customLayout: View = layoutInflater.inflate(R.layout.dialog_free_trial, null)
        val clBind = DialogFreeTrialBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(clBind.root)
        clBind.vFreeTrialCreatedDialogUsername.text = subscriptionModel?.vpnaccounts?.get(0)?.username.toString()
        clBind.vFreeTrialCreatedDialogPassword.text = subscriptionModel?.vpnaccounts?.get(0)?.password.toString()
        clBind.vFreeTrialCreatedDialogIP.text = subscriptionModel?.vpnaccounts?.get(0)?.ip.toString()
        clBind.vFreeTrialCreatedDialogServer.text = LocalRepository(this).vpnServerName
        alertDialog.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    override fun onStateChanged(state: ConnectionState) {
        bind.tvStatus.setText(state.nameId)
        bind.tvStatus.setTextColor(state.color(this))

        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                bind.btConnect.visibility = View.VISIBLE
                bind.btDisconnect.visibility = View.GONE
                bind.vSelectAddress.isEnabled = true
            }
            else -> {
                bind.btConnect.visibility = View.GONE
                bind.btDisconnect.visibility = View.VISIBLE
                bind.vSelectAddress.isEnabled = false
            }
        }
    }

    private fun showProgressBar(){
        bind.progressBar.visibility = View.VISIBLE
        bind.progressBackground.visibility = View.VISIBLE
    }
    private fun hideProgressBar(){
        bind.progressBar.visibility = View.GONE
        bind.progressBackground.visibility = View.GONE
    }
    override fun showProgress() {
        bind.content.visibility = View.GONE
        bind.progressBar.visibility = View.VISIBLE
    }
    override fun hideProgress() {
        bind.content.visibility = View.VISIBLE
        bind.progressBar.visibility = View.GONE
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
