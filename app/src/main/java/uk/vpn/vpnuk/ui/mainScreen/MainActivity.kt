/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import io.reactivex.functions.Function3
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_choose_vpnaccount.view.*
import kotlinx.android.synthetic.main.dialog_free_trial.view.*
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.ui.adapter.vpnAccountAdapter.VpnAccountAdapter
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*
import uk.vpn.vpnuk.ui.registerAccountScreen.RegisterAccountActivity
import uk.vpn.vpnuk.ui.settingsScreen.SettingsActivity


class MainActivity : BaseActivity(), ConnectionStateListener {
    private var dialog: AlertDialog? = null
    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    private lateinit var vm: MainVM


    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.show()
        supportActionBar?.title = ""
        vm = ViewModelProvider(this)[MainVM::class.java]

        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initViews()
        applySettings()
        stFreeTrialAccountVisibility()
        observeLiveData()

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

    private fun observeLiveData() {
        vm.vpnAccounts.observe(this, Observer {
            hideProgressBar()
            if(it.isEmpty()){
                Toasty.error(this, getString(R.string.error_you_dont_have_active_vpn_account))
            }else if(it.size == 1){
                localRepository.vpnUsername = it[0].username.toString()
                localRepository.vpnPassword = it[0].password.toString()
                localRepository.vpnServerName = it[0].server?.description.toString().split("Server:")[1].split("<")[0]
                localRepository.vpnIp = it[0].server?.ip.toString()
                localRepository.vpnDescription = it[0].server?.description.toString()
                localRepository.purchasedSubId = it[0].subscriptionId

                selectNewServer()
                connectToVpn()

                showExplainingDialog()
            }else if(it.size > 1){
                showChooseVpnAccountDialog(it)
            }
        })
    }

    private fun showChooseVpnAccountDialog(list: List<Vpnaccount>){
        val alertDialog = AlertDialog.Builder(this).create()
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_choose_vpnaccount, null)
        alertDialog.setView(customLayout)

        val adapter = VpnAccountAdapter(this, list){
            localRepository.vpnUsername = it.username.toString()
            localRepository.vpnPassword = it.password.toString()
            localRepository.vpnServerName = it.server?.description.toString().split("Server:")[1].split("<")[0]
            localRepository.vpnIp = it.server?.ip.toString()
            localRepository.vpnDescription = it.server?.description.toString()
            localRepository.purchasedSubId = it.subscriptionId

            selectNewServer()
            connectToVpn()

            alertDialog.dismiss()

            showExplainingDialog()
        }
        customLayout.vChooseVpnAccountDialogRecycler.layoutManager = LinearLayoutManager(this)
        customLayout.vChooseVpnAccountDialogRecycler.adapter = adapter

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
            etLogin.setText(it.login)
            etPassword.setText(it.password)
        }
        cbSaveCredentials.isChecked = settings.credentials != null
    }

    private fun initViews() {
        //temp//////////////////////////////   CONNECT CLICK   ////////////////////////////////////
        btConnect.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if(isFirstLoginAttempt()){
                vm.findActiveVpnAccount(login, password)
                showProgressBar()
            }else if(login != localRepository.initialUserName){
                vm.findActiveVpnAccount(login, password)
                showProgressBar()
            }else{
                connectToVpn()
            }
        }
        //temp//////////////////////////////   CONNECT CLICK   ////////////////////////////////////



        tvLinkTrial.stripUnderlines()
        tvLinkTrial.setOnClickListener {
            val intent = Intent(this, RegisterAccountActivity::class.java)
            startActivityForResult(intent, 132)
        }
        vSelectAddress.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerListActivity::class.java))
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

    private fun isFirstLoginAttempt(): Boolean {
        return localRepository.vpnUsername == ""
    }

    private fun stFreeTrialAccountVisibility() {
        if(localRepository.vpnIp != ""){
            tvLinkTrial.visibility = View.GONE
        }else{
            tvLinkTrial.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_action_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivityForResult(Intent(this, SettingsActivity::class.java), 3332)
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 3332 && resultCode == RESULT_OK){
            vpnConnector.stopVpn()

            applySettings()
            initViews()
            selectNewServer()

        }else{
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

    private fun connectToVpn(){
        val login = etLogin.text.toString()
        val password = etPassword.text.toString()

        //Check if checkbox checked...Removed because amazon iap flow
        val credentials: Credentials? = Credentials(
            login,
            password
        )

        val address = repository.getSelectedServer()!!.address
        val settings = repository.getSettings()

        val socket = settings.socket
        val port = settings.port

        repository.updateSettings(settings.copy(credentials = credentials))

        val vpnLogin = localRepository.vpnUsername
        val vpnPassword = localRepository.vpnPassword

        vpnConnector.startVpn(
            vpnLogin,
            vpnPassword,
            address,
            socket,
            port,
            settings.mtu ?: DefaultSettings.MTU_DEFAULT
        )
    }

    private fun showSubscriptionInfoDialog(subscriptionModel: SubscriptionsModel?) {
        val alertDialog = AlertDialog.Builder(this)
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_free_trial, null)
        alertDialog.setView(customLayout)
        customLayout.vFreeTrialCreatedDialogUsername.text = subscriptionModel?.vpnaccounts?.get(0)?.username.toString()
        customLayout.vFreeTrialCreatedDialogPassword.text = subscriptionModel?.vpnaccounts?.get(0)?.password.toString()
        customLayout.vFreeTrialCreatedDialogIP.text = subscriptionModel?.vpnaccounts?.get(0)?.ip.toString()
        customLayout.vFreeTrialCreatedDialogServer.text = LocalRepository(this).vpnServerName
        alertDialog.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

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

    private fun showProgressBar(){
        progressBar.visibility = View.VISIBLE
        progressBackground.visibility = View.VISIBLE
    }
    private fun hideProgressBar(){
        progressBar.visibility = View.GONE
        progressBackground.visibility = View.GONE
    }

    override fun showProgress() {
        content.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        content.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
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
