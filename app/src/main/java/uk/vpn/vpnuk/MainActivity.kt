/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import io.reactivex.functions.Function3
import kotlinx.android.synthetic.main.activity_main.*
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.remote.Wrapper
import uk.vpn.vpnuk.utils.*
import uk.vpn.vpnuk.view.RegisterAccountActivity
import java.util.*


class MainActivity : BaseActivity(), ConnectionStateListener {
    private var dialog: AlertDialog? = null
    private lateinit var repository: Repository
    private lateinit var vpnConnector: VpnConnector


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

    override fun showProgress() {
        content.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        content.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.show()
        supportActionBar?.title = ""


        repository = Repository.instance(this)
        vpnConnector = VpnConnector(this)

        initViews()
        applySettings()

        if (!repository.serversUpdated) {
            repository.updateServers()
                .doOnIoObserveOnMain()
                .addProgressTracking()
                .subscribe({}, { error ->
                    showMessage(getString(R.string.err_unable_to_update_servers))
                })
                .addToDestroySubscriptions()
        }

        createAmazonIap()
    }


    private fun createAmazonIap() {
        PurchasingService.registerListener(this.applicationContext, object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse?) {
                when (response?.requestStatus) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        val q = response.userData.marketplace
                        q
                    }
                }
            }

            override fun onProductDataResponse(response: ProductDataResponse?) {
                Log.d("kek", "onProductDataResponse = ${response?.productData?.size}")
                //Список покупок
                when (response?.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                        val q = response.productData
                        q
                    }
                }
            }

            override fun onPurchaseResponse(response: PurchaseResponse?) {
                Log.d("kek", "onPurchaseResponse = ${response?.receipt?.sku}")
                //Купленная только что покупка
                when (response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d("kek", "onPurchaseResponse = success   ${response.receipt.sku}")

                        val q = response.receipt
                        q

                        PurchasingService.notifyFulfillment("DED01-2-1F", FulfillmentResult.FULFILLED)
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
                        q
                    }
                }
            }
        })

        PurchasingService.getPurchaseUpdates(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setView(R.layout.dialog_free_trial)
        alertDialog.setPositiveButton("OK") { dialog, which ->

        }
        alertDialog.show()
    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivity(Intent(this, SettingsActivity::class.java))
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        //tvLinkTrial.movementMethod = LinkMovementMethod.getInstance()
        tvLinkTrial.stripUnderlines()

        tvLinkTrial.setOnClickListener {
            val intent = Intent(this, RegisterAccountActivity::class.java)
            startActivityForResult(intent, 132)
        }

        vSelectAddress.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerListActivity::class.java))
        }

        btConnect.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()
            val credentials: Credentials? = if (cbSaveCredentials.isChecked) Credentials(
                login,
                password
            ) else null
            val address = repository.getSelectedServer()!!.address
            val settings = repository.getSettings()

            val socket = settings.socket
            val port = settings.port

            repository.updateSettings(
                settings.copy(
                    credentials = credentials
                )
            )

            vpnConnector.startVpn(
                login,
                password,
                address,
                socket,
                port,
                settings.mtu ?: DefaultSettings.MTU_DEFAULT
            )
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
//                    tvDns.text = it.dns
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

    private fun applySettings() {
        val settings = repository.getSettings()
        settings.credentials?.let {
            etLogin.setText(it.login)
            etPassword.setText(it.password)
        }
        cbSaveCredentials.isChecked = settings.credentials != null
    }

    override fun onResume() {
        super.onResume()
        vpnConnector.startListen(this)


        val hash = HashSet<String>()
        hash.add("DED01-2-1F")
        PurchasingService.getUserData()
        PurchasingService.getProductData(hash)
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
