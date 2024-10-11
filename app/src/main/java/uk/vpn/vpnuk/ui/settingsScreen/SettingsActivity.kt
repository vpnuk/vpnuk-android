/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import com.google.android.material.tabs.TabLayout.GRAVITY_FILL
import com.google.android.material.tabs.TabLayout.MODE_FIXED
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.databinding.ActivitySettingsBinding
import uk.vpn.vpnuk.databinding.DialogAccountDeletePromptBinding
import uk.vpn.vpnuk.databinding.DialogSubscriptionExpiredBinding
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.model.DnsServer
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.ui.settingsScreen.manageApps.ManageAppsActivity
import uk.vpn.vpnuk.ui.settingsScreen.manageWebsites.ManageWebsitesActivity
import uk.vpn.vpnuk.utils.*
import javax.mail.internet.InternetAddress


class SettingsActivity : BaseActivity() {

    private lateinit var bind: ActivitySettingsBinding

    private var dialog: AlertDialog? = null
    private lateinit var repository: Repository
    private lateinit var settings: Settings
    val vm: SettingsViewModel by viewModels()

    private var vpnAccountsList = mutableListOf<Vpnaccount>()
    private var subscriptionsList = mutableListOf<SubscriptionsModel>()


    val hash = HashSet<String>()
    val purchaseKey = "DED01-FR"

    var pendingOrderId = ""

    lateinit var selectedVpnAccountToRenew: Vpnaccount


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(bind.root)
        supportActionBar?.title = getString(R.string.Settings)

        repository = Repository(this)

        vm.onCreate()
        if(localRepository.isAppDownloadedFromAmazon){
            createAmazonIap()
        }

        initViews()
        applySettings()
        observeLiveData()
    }

    private fun observeLiveData() {
        vm.oneShotEvents.onEach {
            when(it){
                is SettingsViewModel.OneShotEvent.ErrorToast ->{
                    Toasty.error(this, it.message, Toasty.LENGTH_SHORT).show()
                }
            }
        }.launchWhenCreated(lifecycleScope)
        vm.viewState.onEach {
            render(it)
        }.launchWhenCreated(lifecycleScope)

        vm.allSubscriptionsLive.observe(this) {
            vpnAccountsList = mutableListOf()
            subscriptionsList = it as MutableList<SubscriptionsModel>

            it.forEach { subscription ->
                subscription.vpnaccounts?.forEach { vpnAccount ->
                    vpnAccount.subscriptionId = subscription.id ?: 0
                    vpnAccountsList.add(vpnAccount)
                }
            }
            initSubscriptionsSpinner()
        }
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

    private fun render(viewState: SettingsViewModel.ViewState) {
        if(viewState.amazonApiSettingsVisible){
            bind.frameServers.visible()
        }else{
            bind.frameServers.gone()
        }

        if(viewState.serverProgressView){
            showServerProgress()
        }else{
            hideServerProgress()
        }

        initDnsSpinner(viewState.customDnsServers)
    }

    private fun initDnsSpinner(customDnsServers: List<DnsServer>) {
        val dnsAdapter = ArrayAdapter(this, R.layout.spinner_custom, customDnsServers.map { it.name })

        bind.vSettingsActivitySpinnerDNS.adapter = dnsAdapter

        bind.vSettingsActivitySpinnerDNS.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                vm.onCustomDnsSelected(position)
            }
        })

        bind.vSettingsActivitySpinnerDNS.setSelection(dnsAdapter.getPosition(localRepository.customDns?.name))
    }

    private fun initSubscriptionsSpinner() {
        val vpnAccountStrings = vpnAccountsList.map { it.username }
        val accountsAdapter = ArrayAdapter(this, R.layout.spinner_custom, vpnAccountStrings)

        bind.vSettingsActivitySpinner.adapter = accountsAdapter

        bind.vSettingsActivitySpinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener{
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

        bind.vSettingsActivitySpinner.setSelection(accountsAdapter.getPosition(localRepository.vpnUsername))
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
                    else -> {}
                }
            }
        })

        PurchasingService.getPurchaseUpdates(false)
    }


    private fun selectNewServer() {
        val ip = localRepository.vpnIp
        //repository.setServerId(ip)
        //    .doOnIoObserveOnMain()
        //    .subscribe {}.addToDestroySubscriptions()
    }

    private fun initViews() {
        //For FireTv
        bind.vSettingsActivitySpinner.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                bind.vSettingsActivityFrameChooseServer.background = resources.getDrawable(R.drawable.dropdown_gray_focused)
            }else{
                bind.vSettingsActivityFrameChooseServer.background = resources.getDrawable(R.drawable.dropdown_gray)
            }
        }
        bind.vSettingsActivitySpinnerDNS.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                bind.vSettingsActivityFrameChooseDNS.background = resources.getDrawable(R.drawable.dropdown_gray_focused)
            }else{
                bind.vSettingsActivityFrameChooseDNS.background = resources.getDrawable(R.drawable.dropdown_gray)
            }
        }
        settings = localRepository.settings!!


        bind.buttonManageApps.setOnClickListener { startActivity(Intent(this, ManageAppsActivity::class.java)) }
        bind.buttonManageWebsites.setOnClickListener { startActivity(Intent(this, ManageWebsitesActivity::class.java)) }



        bind.switchEnableMTU.post {
            bind.switchEnableMTU.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    dialog = AlertDialog.Builder(this).setItems(DefaultSettings.MTU_LIST) { _, i ->
                        localRepository.settings = localRepository.settings?.copy(mtu = DefaultSettings.MTU_LIST[i])
                    }
                        .setTitle(getString(R.string.custom_mtu))
                        .setOnCancelListener {
                            bind.switchEnableMTU.isChecked = false
                        }
                        .create().apply {
                            show()
                        }
                } else {
                    removeMtu()
                }
            }
        }

        bind.switchKillSwitch.setOnCheckedChangeListener { _, checked ->
            localRepository.settings = localRepository.settings?.copy(reconnect = checked)
        }

        bind.switchUseObfuscation.isChecked = localRepository.useObfuscation
        bind.switchUseObfuscation.setOnCheckedChangeListener { _, isChecked ->
            localRepository.useObfuscation = isChecked
            applySettings()
        }

        bind.buttonDeleteAccount.visibility = if(localRepository.isAppDownloadedFromAmazon) View.GONE else View.VISIBLE
        bind.buttonDeleteAccount.setOnClickListener {
            val login = localRepository.settings?.credentials?.login
            if(login != null && login != ""){
                showDeletionDialog()
            }else{
                Toasty.error(this, "You are not logged in", Toasty.LENGTH_LONG).show()
            }
        }

        if(Logger.vpnLogs.size > 300){
            Logger.vpnLogs.clear()
        }

        var colorChangeFlag = false
        for(i in Logger.vpnLogs.indices){
            val word = SpannableString(Logger.vpnLogs[i])
            var textColor = if(!colorChangeFlag) {
                colorChangeFlag = true
                Color.GRAY
            } else{
                colorChangeFlag = false
                Color.BLACK
            }
            word.setSpan(ForegroundColorSpan(textColor), 0, word.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            bind.textViewLogs.append(word)
        }

        bind.buttonShareLogs.setOnClickListener {
            val intent2 = Intent()
            intent2.setAction(Intent.ACTION_SEND)
            intent2.setType("text/plain")
            intent2.putExtra(Intent.EXTRA_TEXT, Logger.vpnLogs.toString())
            startActivity(Intent.createChooser(intent2, "Share via"))
        }
    }

    @SuppressLint("NewApi")
    private fun sendEmail(message: String) {
        try{
            val auth = EmailService.UserPassAuthenticator("vpnuk.droid.mail@gmail.com", "dgis fvhb uyea txju")
            val to = listOf(InternetAddress("deletion@vpnuk.info"))
            val from = InternetAddress("vpnuk.droid.mail@gmail.com")
            val email = EmailService.Email(auth, to, from, "Account deletion", message)
            val emailService = EmailService("smtp.gmail.com", 587)

            GlobalScope.launch {
                emailService.send(email)
            }
        }catch (_: Exception){}
    }

    private fun showServerProgress(){
        bind.vSettingsActivityServersProgressBackground.visibility = View.VISIBLE
        bind.vSettingsActivityServersProgressView.visibility = View.VISIBLE
    }
    private fun hideServerProgress(){
        bind.vSettingsActivityServersProgressBackground.visibility = View.GONE
        bind.vSettingsActivityServersProgressView.visibility = View.GONE
    }

    private fun applySettings() {
        settings = localRepository.settings!!

        val socketType = SocketType.byValue(settings.socket)!!
        val portIndex = socketType.ports.indexOf(settings.port)
        val socketIndex = SocketType.values().indexOf(socketType)

        if(!localRepository.useObfuscation){
            bind.tabsPort.setSelectedTabIndicatorColor(resources.getColor(R.color.light_blue))
            bind.tabsSocketType.setSelectedTabIndicatorColor(resources.getColor(R.color.light_blue))
            bind.tabsPort.setTabTextColors(resources.getColor(R.color.light_blue), resources.getColor(R.color.light_blue))
            bind.tabsSocketType.setTabTextColors(resources.getColor(R.color.light_blue), resources.getColor(R.color.light_blue))

            bind.tabsSocketType.setTabs(SocketType.values().map { it.value })
            bind.tabsPort.setTabs(SocketType.byValue(settings.socket)!!.ports)

            bind.tabsPort.setTabListener { _, _ ->
                if (bind.tabsPort.selectedTab().text.toString() != "443" && !localRepository.useObfuscation)
                    localRepository.settings = localRepository.settings?.copy(
                        socket = bind.tabsSocketType.selectedTab().text.toString(),
                        port = bind.tabsPort.selectedTab().text.toString()
                    )
            }
            bind.tabsSocketType.setTabListener { text, _ ->
                bind.tabsPort.setTabs(SocketType.byValue(text)!!.ports)
                localRepository.settings = localRepository.settings?.copy(
                    socket = bind.tabsSocketType.selectedTab().text.toString(),
                    port = bind.tabsPort.selectedTab().text.toString()
                )
            }

            bind.tabsSocketType.select(socketIndex)
            bind.tabsPort.select(portIndex)
            bind.switchKillSwitch.isChecked = settings.reconnect
            bind.switchEnableMTU.isChecked = settings.mtu?.let { it != DefaultSettings.MTU_DEFAULT } ?: false
        }


        if(localRepository.useObfuscation){
            bind.tabsPort.tabMode =  MODE_FIXED
            bind.tabsSocketType.tabMode =  MODE_FIXED
            bind.tabsPort.tabGravity = GRAVITY_FILL
            bind.tabsSocketType.tabGravity = GRAVITY_FILL

            bind.tabsSocketType.clearOnTabSelectedListeners()
            bind.tabsPort.setTabs(listOf("443"))
            bind.tabsSocketType.setTabs(listOf("UDP"))

            bind.tabsSocketType.select(0)
            bind.tabsPort.select(0)


            bind.tabsPort.setSelectedTabIndicatorColor(Color.GRAY)
            bind.tabsSocketType.setSelectedTabIndicatorColor(Color.GRAY)
            bind.tabsPort.setTabTextColors(Color.GRAY, Color.GRAY)
            bind.tabsSocketType.setTabTextColors(Color.GRAY, Color.GRAY)
            bind.tabsPort.isEnabled = false
            bind.tabsSocketType.isEnabled = false
        }
    }

    private fun removeMtu() {
        localRepository.settings = localRepository.settings?.copy(mtu = null)
    }

    private fun showDeletionDialog(){
        val alertDialog = AlertDialog.Builder(this).create()
        val clBind = DialogAccountDeletePromptBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(clBind.root)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        clBind.buttonYes.setOnClickListener {
            sendEmail(
                ("Username: " + localRepository.settings?.credentials?.login)
            )
            alertDialog.dismiss()
        }
        clBind.buttonNo.setOnClickListener { alertDialog.dismiss() }
    }

    private fun showSubscriptionExpiredDialog(){
        val alertDialog = AlertDialog.Builder(this).create()
        //val customLayout: View = layoutInflater.inflate(R.layout.dialog_subscription_expired, null)
        val clBind = DialogSubscriptionExpiredBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(clBind.root)
        alertDialog.show()

        clBind.vSubscriptionExpiredDialogRenew.setOnClickListener {
            PurchasingService.purchase(purchaseKey)
        }
        clBind.vSubscriptionExpiredDialogCancel.setOnClickListener { alertDialog.dismiss() }
    }

    override fun onResume() {
        super.onResume()

        if(localRepository.isAppDownloadedFromAmazon){
            hash.add(purchaseKey)
            PurchasingService.getUserData()
            PurchasingService.getProductData(hash)
        }
    }
}
