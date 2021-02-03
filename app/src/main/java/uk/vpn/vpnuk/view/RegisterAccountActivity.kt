/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_register_account.*
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.utils.isEmailValid
import java.util.HashSet


class RegisterAccountActivity : AppCompatActivity(), View.OnClickListener {


    val hash = HashSet<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_account)


        initListeners()
        createAmazonIap()
    }

    private fun initListeners() {
        button_sign_up.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        hash.add("DED01-2-1F")

        PurchasingService.getUserData();
        PurchasingService.getProductData(hash)
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
                Log.d("kek", "onProductDataResponse222 = ${response?.productData?.size}")
                //Список покупок
                when (response?.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                        val q = response.productData
                        q
                    }
                }
            }

            override fun onPurchaseResponse(response: PurchaseResponse?) {
                Log.d("kek", "onPurchaseResponse222 = ${response?.receipt?.sku}")
                //Купленная только что покупка
                when (response?.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                        Log.d("kek", "onPurchaseResponse222 = success   ${response.receipt.sku}")
                        Toasty.success(this@RegisterAccountActivity,"Purchase Success", Toasty.LENGTH_SHORT).show()

                        PurchasingService.notifyFulfillment("DED01-2-1F", FulfillmentResult.FULFILLED)
                    }
                    PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                        Log.d("kek", "onPurchaseResponse222 = already_purchased")

                        Toasty.info(this@RegisterAccountActivity,"Already purchased", Toasty.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse?) {
                Log.d("kek", "onPurchaseUpdatesResponse222 = ${response?.receipts?.size}")
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

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.button_sign_up ->{
                val name = editText_name.text.toString().trim()
                val email = editText_email.text.toString().trim()
                val password = editText_password.text.toString().trim()

                if(checkFields(name, email, password)){
                    PurchasingService.purchase("DED01-2-1F")
                }
            }
        }
    }

    private fun checkFields(
        name: String,
        email: String,
        password: String
    ) : Boolean {
        if(email.isBlank()){
            Toasty.error(this, "Email can't be empty", Toasty.LENGTH_SHORT).show()
            return false
        }else if(!email.isEmailValid()){
            Toasty.error(this, "Invalid email", Toasty.LENGTH_SHORT).show()
            return false
        }

        if(name.isBlank()){
            Toasty.error(this, "Name can't be empty", Toasty.LENGTH_SHORT).show()
            return false
        }

        if(password.isBlank()){
            Toasty.error(this, "Password can't be empty", Toasty.LENGTH_SHORT).show()
            return false
        }else if(password.length < 6){
            Toasty.error(this, "Password must be more than 5 characters", Toasty.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}