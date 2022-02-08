/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.dialog_subscription_expired.view.*
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.*
import java.util.HashSet


class SettingsActivity : BaseActivity() {

    private var dialog: AlertDialog? = null
    private lateinit var repository: Repository
    private lateinit var settings: Settings
    private lateinit var vm: SettingsViewModel

    private var vpnAccountsList = mutableListOf<Vpnaccount>()
    private var subscriptionsList = mutableListOf<SubscriptionsModel>()


    val hash = HashSet<String>()
    val purchaseKey = "DED01-FR"

    var pendingOrderId = ""

    lateinit var selectedVpnAccountToRenew: Vpnaccount



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = getString(R.string.Settings)
        vm = ViewModelProvider(this)[SettingsViewModel::class.java]


        repository = Repository(this)


        vm.getServerList()
        showServerProgress()


        initViews()
        applySettings()
        initObservers()
        createAmazonIap()
    }

    private fun initObservers() {
        vm.allSubscriptions.observe(this, Observer {
            vpnAccountsList = mutableListOf()
            subscriptionsList = it as MutableList<SubscriptionsModel>

            it.forEach { subscription ->
                subscription.vpnaccounts?.forEach { vpnAccount ->
                    vpnAccount.subscriptionId = subscription.id ?: 0
                    vpnAccountsList.add(vpnAccount)
                }
            }

            hideServerProgress()
            initSpinners()
        })
        vm.isRegisteredFromApp.observe(this, Observer {
            if(it){
                vm.getPendingOrders()
            }else{
                Toasty.info(this, "Please, renew your subscription at vpnuk.net/my-account/", Toasty.LENGTH_LONG).show()
            }
        })
        vm.isSubscriptionExpired.observe(this, Observer {
            if(it.first){
                pendingOrderId = it.second
                showSubscriptionExpiredDialog()
            }
        })
    }

    private fun initSpinners() {
        val vpnAccountStrings = vpnAccountsList.map { it.username }
        val accountsAdapter = ArrayAdapter(this, R.layout.spinner_custom, vpnAccountStrings)

        vSettingsActivitySpinner.adapter = accountsAdapter

        vSettingsActivitySpinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(vpnAccountsList[position].username != localRepository.vpnUsername){
                    subscriptionsList.forEach { subscription ->
                        subscription.vpnaccounts?.forEach { vpnAccount ->
                            if(vpnAccount.username == vpnAccountsList[position].username){
                                if(subscription.status != "active"){

                                    selectedVpnAccountToRenew = vpnAccount
                                    val selectedSubscription = subscription.id
                                    vm.checkIfRegisteredFromApp(localRepository.initialEmail, selectedSubscription?:0)

                                }else{
                                    localRepository.vpnUsername = vpnAccount.username.toString()
                                    localRepository.vpnPassword = vpnAccount.password.toString()
                                    localRepository.vpnServerName = vpnAccount.server?.description.toString().split("Server:")[1].split("<")[0]
                                    localRepository.vpnIp = vpnAccount.server?.ip.toString()
                                    localRepository.vpnDescription = vpnAccount.server?.description.toString()
                                    localRepository.purchasedSubId = vpnAccount.subscriptionId

                                    selectNewServer()

                                    setResult(RESULT_OK)
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        })

        vSettingsActivitySpinner.setSelection(accountsAdapter.getPosition(localRepository.vpnUsername))
    }

    private fun createAmazonIap() {
        PurchasingService.registerListener(this.applicationContext, object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse?) {}
            override fun onProductDataResponse(response: ProductDataResponse?) {}
            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {}
            override fun onPurchaseResponse(response: PurchaseResponse?) {
                when (response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d("kek", "onPurchaseResponse = success. ReceiptID  -----  ${response.receipt.receiptId}")
                        Log.d("kek", "onPurchaseResponse = success. UserID  -----  ${response.userData.userId}")

                        val amazonReceiptId = response.receipt.receiptId
                        val amazonUserId = response.userData.userId


                        vm.renewSubscription(amazonReceiptId, amazonUserId, pendingOrderId)

                        localRepository.vpnUsername = selectedVpnAccountToRenew.username.toString()
                        localRepository.vpnPassword = selectedVpnAccountToRenew.password.toString()
                        localRepository.vpnServerName = selectedVpnAccountToRenew.server?.description.toString().split("Server:")[1].split("<")[0]
                        localRepository.vpnIp = selectedVpnAccountToRenew.server?.ip.toString()
                        localRepository.vpnDescription = selectedVpnAccountToRenew.server?.description.toString()
                        localRepository.purchasedSubId = selectedVpnAccountToRenew.subscriptionId

                        selectNewServer()

                        setResult(RESULT_OK)
                        finish()

                        PurchasingService.notifyFulfillment(purchaseKey, FulfillmentResult.FULFILLED)
                    }
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                        Log.d("kek", "onPurchaseResponse = already_purchased")
                    }
                }
            }
        })

        PurchasingService.getPurchaseUpdates(false)
    }


    private fun selectNewServer() {
        val ip = localRepository.vpnIp
        repository.setServerId(ip)
            .doOnIoObserveOnMain()
            .subscribe {}.addToDestroySubscriptions()
    }

    private fun initViews() {
        //For fireTv
        vSettingsActivitySpinner.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                vSettingsActivityFrameChooseServer.background = resources.getDrawable(R.drawable.blue_rounded_stroke)
            }else{
                vSettingsActivityFrameChooseServer.background = resources.getDrawable(R.drawable.gray_rounded_stroke)
            }
        }


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

    override fun showProgress() {
        vSettingsActivityProgressView.visibility = View.VISIBLE
    }
    override fun hideProgress() {
        vSettingsActivityProgressView.visibility = View.GONE
    }

    private fun showServerProgress(){
        vSettingsActivityServersProgressBackground.visibility = View.VISIBLE
        vSettingsActivityServersProgressView.visibility = View.VISIBLE
    }
    private fun hideServerProgress(){
        vSettingsActivityServersProgressBackground.visibility = View.GONE
        vSettingsActivityServersProgressView.visibility = View.GONE
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

    private fun showSubscriptionExpiredDialog(){
        val alertDialog = AlertDialog.Builder(this).create()
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_subscription_expired, null)
        alertDialog.setView(customLayout)
        alertDialog.show()

        customLayout.vSubscriptionExpiredDialogRenew.setOnClickListener {
            PurchasingService.purchase(purchaseKey)
        }
        customLayout.vSubscriptionExpiredDialogCancel.setOnClickListener { alertDialog.dismiss() }
    }

    override fun onResume() {
        super.onResume()

        hash.add(purchaseKey)
        PurchasingService.getUserData()
        PurchasingService.getProductData(hash)
    }
}
