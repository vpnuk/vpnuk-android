/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.quickLaunch

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.RuntimePermissions
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.databinding.ActivityQuickLaunchBinding
import uk.vpn.vpnuk.databinding.DialogSubscriptionExpiredBinding
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.ui.dialog.NotificationExplanationDialog
import uk.vpn.vpnuk.ui.mainScreen.amazonVersion.AmazonMainActivity
import uk.vpn.vpnuk.ui.mainScreen.googleVersion.GoogleMainActivity
import uk.vpn.vpnuk.utils.*


@RuntimePermissions
class QuickLaunchActivity : BaseActivity(), ConnectionStateListener {

    lateinit var bind: ActivityQuickLaunchBinding

    private lateinit var vm: QuickLaunchVM
    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val hash = HashSet<String>()
    val purchaseKey = "DED01-FR"
    var pendingOrderId = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityQuickLaunchBinding.inflate(layoutInflater)
        setContentView(bind.root)
        vm = ViewModelProvider(this)[QuickLaunchVM::class.java]

        vpnConnector = VpnConnector(this)


        if(localRepository.isAppDownloadedFromAmazon){ initAmazonServices() }


        observeLivaData()
        initView()
        initListeners()

        requestPostNotificationPermissionWithPermissionCheck()
    }

    private fun initAmazonServices() {
        if(localRepository.isAppDownloadedFromAmazon){
            if(LocalRepository(this).initialEmail != ""){
                vm.updateToken()
            }

            createAmazonIap()
        }
    }

    private fun observeLivaData() {
        vm.tokenSuccess.observe(this) {
            vm.checkRegisteredSource(LocalRepository(this).initialEmail, "app")
        }
        vm.isUserRegisteredFromApp.observe(this) {
            if (it) vm.checkSubscriptionState(localRepository.purchasedSubId)
        }
        vm.isSubscriptionExpired.observe(this) {
            if (it.first) {
                pendingOrderId = it.second
                showSubscriptionExpiredDialog()
            }
        }
    }

    private fun initListeners() {
        bind.switchConnect.setOnCheckedChangeListener { view, isChecked ->
            if(isChecked){
                if(PermissionUtils.hasSelfPermissions(this, Manifest.permission.POST_NOTIFICATIONS)){
                    startVpn()
                }else{
                    requestPostNotificationPermissionWithPermissionCheck()
                }
            }else{
                stopVpn()
            }
        }
        bind.imageViewConnectionConfigure.setOnClickListener {
            if(localRepository.isAppDownloadedFromAmazon){
                val intent = Intent(this, AmazonMainActivity::class.java)
                startActivity(intent)
            }else{
                val intent = Intent(this, GoogleMainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun createAmazonIap() {
        PurchasingService.registerListener(this.applicationContext, object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse?) {}
            override fun onProductDataResponse(response: ProductDataResponse?) {}
            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {}
            override fun onPurchaseResponse(response: PurchaseResponse?) {
                when (response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        val amazonReceiptId = response.receipt.receiptId
                        val amazonUserId = response.userData.userId

                        vm.renewSubscription(amazonReceiptId, amazonUserId, pendingOrderId)

                        PurchasingService.notifyFulfillment(purchaseKey, FulfillmentResult.FULFILLED)
                    }
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {}
                    else -> {}
                }
            }
        })

        PurchasingService.getPurchaseUpdates(false)
    }

    private fun initView() {
        supportActionBar?.hide()
    }

    private fun stopVpn() {
        vpnConnector.stopVpn()
    }

    private fun startVpn() {
        if(checkIfDataFilled()){
            val address = localRepository.currentServer?.address
            val settings = localRepository.settings

            val socket = settings?.socket
            val port = settings?.port

            val isLoggedByCreds = localRepository.isLoginByUserCreds
            val vpnLogin = if(isLoggedByCreds) localRepository.vpnUsername else localRepository.settings?.credentials?.login
            val vpnPassword = if(isLoggedByCreds) localRepository.vpnPassword else localRepository.settings?.credentials?.password

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
    }

    @NeedsPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun requestPostNotificationPermission(){
        Log.d("kek", "REQUESTING PERMISSIONS")
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun requestWriteFilesPermission(){
        Log.d("kek", "REQUESTING PERMISSIONS")
    }

    @OnNeverAskAgain(Manifest.permission.POST_NOTIFICATIONS)
    fun onNotificationsNeverAskAgain() {
        val dialog = NotificationExplanationDialog()
        dialog.show(supportFragmentManager, "dummy")
    }


    private fun checkIfDataFilled() : Boolean {
        val settings = localRepository.settings

        if(localRepository.currentServer == null){
            Toasty.error(this, "You have not selected a server", Toasty.LENGTH_SHORT).show()
            return false
        }else if(settings?.credentials == null){
            Toasty.error(this, "You have not entered credentials", Toasty.LENGTH_SHORT).show()
            return false
        }else{
            return true
        }
    }

    override fun onStateChanged(state: ConnectionState) {
        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                bind.switchConnect.isChecked = false
                bind.textViewState.text = "Disconnected"
            }
            ConnectionState.LEVEL_START ->{
                bind.textViewState.text = "Connecting"
            }
            ConnectionState.LEVEL_CONNECTED -> {
                bind.switchConnect.isChecked = true
                bind.textViewState.text = "Connected"
            }
            else -> {}
        }
    }

    private fun showSubscriptionExpiredDialog(){
        val alertDialog = AlertDialog.Builder(this).create()
        val clBind = DialogSubscriptionExpiredBinding.inflate(layoutInflater, null, false)
        alertDialog.setView(clBind.root)
        alertDialog.show()

        clBind.vSubscriptionExpiredDialogRenew.setOnClickListener {
            PurchasingService.purchase(purchaseKey)
        }
        clBind.vSubscriptionExpiredDialogCancel.setOnClickListener { alertDialog.dismiss() }
    }

    private fun resumeAmazonService() {
        if(localRepository.isAppDownloadedFromAmazon){
            hash.add(purchaseKey)
            PurchasingService.getUserData()
            PurchasingService.getProductData(hash)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun showProgress() {
        bind.switchConnect.visibility = View.GONE
        bind.progressBarQuick.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        bind.switchConnect.visibility = View.VISIBLE
        bind.progressBarQuick.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)

        resumeAmazonService()
    }

    override fun onPause() {
        vpnConnector.removeListener()
        super.onPause()
    }
}