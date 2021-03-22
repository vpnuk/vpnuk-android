/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.registerAccountScreen

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_register_account.*
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.CREATED_SUBSCRIPTION
import uk.vpn.vpnuk.utils.doOnIoObserveOnMain
import uk.vpn.vpnuk.utils.isEmailValid
import java.util.HashSet


class RegisterAccountActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var vm: RegisterAccountVM
    lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_account)
        vm = ViewModelProvider(this)[RegisterAccountVM::class.java]


        initListeners()
        observeLiveData()
    }

    private fun observeLiveData() {
        vm.successToken.observe(this, Observer {
            vm.createSubscription()
        })
        vm.createdSubscription.observe(this, Observer {
            saveNewCredentialsAsDefault(it)

            hideProgress()

            val intent = Intent()
            intent.putExtra(CREATED_SUBSCRIPTION, it)
            setResult(RESULT_OK, intent)
            finish()
        })
        vm.error.observe(this, Observer {
            Toasty.error(this, it, Toasty.LENGTH_SHORT).show()
            hideProgress()
        })
    }

    private fun saveNewCredentialsAsDefault(it: SubscriptionsModel?) {
        val login = it?.vpnaccounts?.get(0)?.username.toString()
        val password = it?.vpnaccounts?.get(0)?.password.toString()

        val repository = Repository.instance(this)
        val settings = repository.getSettings()
        val credentials = Credentials(login, password)
        repository.updateSettings(settings.copy(credentials = credentials))
    }

    private fun initListeners() {
        button_sign_up.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.button_sign_up ->{
                val userName = editText_name.text.toString().trim()
                val email = editText_email.text.toString().trim()
                val password = editText_password.text.toString().trim()

                if(checkFields(userName, email, password)){
                    vm.registerUser(userName, email, password)
                    showProgress()
                }else{
                    Toasty.error(this, "Wrong data", Toasty.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showProgress() {
        progressDialog = ProgressDialog.show(this, "Sign Up in process", "Please wait")
    }

    private fun hideProgress(){
        progressDialog.hide()
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