/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.quickLaunch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_quick_launch.*
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import kotlinx.android.synthetic.main.dialog_subscription_expired.view.*
import uk.vpn.vpnuk.*
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.utils.*
import uk.vpn.vpnuk.view.mainScreen.MainActivity
import java.util.HashSet

class QuickLaunchActivity : BaseActivity(), ConnectionStateListener {

    private lateinit var vm: QuickLaunchVM
    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val hash = HashSet<String>()
    val purchaseKey = "DED01-FR"

    var pendingOrderId = ""

    private lateinit var localRepository: LocalRepository



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_launch)
        vm = ViewModelProvider(this)[QuickLaunchVM::class.java]

        localRepository = LocalRepository(this)
        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)


        if(LocalRepository(this).initialEmail != ""){
            vm.updateToken()
        }

        createAmazonIap()
        observeLivaData()
        initView()

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

    private fun observeLivaData() {
        vm.tokenSuccess.observe(this, Observer {
            vm.checkRegisteredSource(LocalRepository(this).initialEmail, "app")
        })
        vm.isUserRegisteredFromApp.observe(this, Observer {
            if(it) vm.checkSubscriptionState(localRepository.purchasedSubId)
        })
        vm.isSubscriptionExpired.observe(this, Observer {
            if(it.first){
                pendingOrderId = it.second
                showSubscriptionExpiredDialog()
            }
        })
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




    private fun initView() {
        supportActionBar?.hide()

        switch_connect.setOnCheckedChangeListener { view, isChecked ->
            if(isChecked){
                if(checkData()){
                    val address = repository.getSelectedServer()?.address
                    val settings = repository.getSettings()

                    val socket = settings.socket
                    val port = settings.port

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
            }else{
                vpnConnector.stopVpn()
            }
        }
        imageView_connection_configure.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkData() : Boolean {
        val settings = repository.getSettings()

        if(repository.getSelectedServer() == null){
            Toasty.error(this, "You have not selected a server", Toasty.LENGTH_SHORT).show()
            return false
        }else if(settings.credentials == null){
            Toasty.error(this, "You have not entered credentials", Toasty.LENGTH_SHORT).show()
            return false
        }else{
            return true
        }
    }

    override fun onStateChanged(state: ConnectionState) {

        when (state) {
            ConnectionState.LEVEL_NOTCONNECTED -> {
                switch_connect.isChecked = false
                textView_state.text = "Disconnected"
            }
            ConnectionState.LEVEL_START ->{
                textView_state.text = "Connecting"
            }
            ConnectionState.LEVEL_CONNECTED -> {
                switch_connect.isChecked = true
                textView_state.text = "Connected"
            }
        }
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

    override fun showProgress() {
        switch_connect.visibility = View.GONE
        progressBarQuick.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        switch_connect.visibility = View.VISIBLE
        progressBarQuick.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)

        hash.add(purchaseKey)
        PurchasingService.getUserData()
        PurchasingService.getProductData(hash)
    }

    override fun onPause() {
        vpnConnector.removeListener()
        super.onPause()
    }
}