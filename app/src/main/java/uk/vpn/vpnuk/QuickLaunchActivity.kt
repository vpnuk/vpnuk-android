/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_quick_launch.*
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import io.reactivex.functions.Function3
import kotlinx.android.synthetic.main.dialog_free_trial.view.*
import kotlinx.android.synthetic.main.dialog_subscription_expired.view.*
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*
import java.util.HashSet

class QuickLaunchActivity : BaseActivity(), ConnectionStateListener {

    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector

    val hash = HashSet<String>()
    val purchaseKey = "DED01-2-1F"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_launch)

        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initView()

        showSubscriptionExpiredDialog()

        if (!repository.serversUpdated) {
            repository.updateServers()
                .doOnIoObserveOnMain()
                .addProgressTracking()
                .subscribe({}, { error ->
                    showMessage(getString(R.string.err_unable_to_update_servers))
                })
                .addToDestroySubscriptions()
        }


        //TODO method to buy purchase
        //PurchasingService.purchase(purchaseKey)

        createAmazonIap()
    }

    private fun createAmazonIap() {
        PurchasingService.registerListener(this.applicationContext, object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse?) {
                when (response?.requestStatus) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        val amazonUserID = response.userData.userId


                        Log.d("kek", "onUserDataResponse = success -----  $amazonUserID")
                    }
                }
            }
            override fun onProductDataResponse(response: ProductDataResponse?) {}
            override fun onPurchaseResponse(response: PurchaseResponse?) {
                Log.d("kek", "onPurchaseResponse = ${response?.receipt?.sku}")
                //Купленная только что покупка
                when (response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d("kek", "onPurchaseResponse = success. ReceiptSKU  -----  ${response.receipt.sku}")
                        Log.d("kek", "onPurchaseResponse = success. ReceiptID  -----  ${response.receipt.receiptId}")

                        Log.d("kek", "onPurchaseResponse = success. UserID  -----  ${response.userData.userId}")


                        val amazonReceiptId = response.receipt.receiptId

                        PurchasingService.notifyFulfillment(purchaseKey, FulfillmentResult.FULFILLED)
                    }
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                        Log.d("kek", "onPurchaseResponse = already_purchased")
                    }
                }
            }
            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                Log.d("kek", "onPurchaseUpdatesResponse = ${response?.receipts?.size}")
                //Список купленных юзером покупок
                when (response?.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                        val q = response.receipts
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
                    val login = settings.credentials?.login
                    val password = settings.credentials?.password

                    val socket = settings.socket
                    val port = settings.port

                    vpnConnector.startVpn(
                        login,
                        password,
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
            Toasty.success(this, "RENEW SUBSCRIPTION", Toasty.LENGTH_SHORT).show()
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