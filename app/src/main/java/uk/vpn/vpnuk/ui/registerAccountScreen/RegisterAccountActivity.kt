/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.registerAccountScreen

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import es.dmoral.toasty.Toasty
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.databinding.ActivityRegisterAccountBinding
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.CREATED_SUBSCRIPTION
import uk.vpn.vpnuk.utils.isEmailValid


class RegisterAccountActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var bind: ActivityRegisterAccountBinding

    lateinit var vm: RegisterAccountVM
    lateinit var progressDialog: ProgressDialog

    lateinit var localRepository: LocalRepository

    private var serverList = listOf<String>()
    private var selectedServerCountry = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityRegisterAccountBinding.inflate(layoutInflater)
        setContentView(bind.root)
        vm = ViewModelProvider(this)[RegisterAccountVM::class.java]

        vm.getServerList()

        localRepository = LocalRepository(this)

        //For fireTv
        bind.vRegisterActivitySpinnerServers.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                bind.vRegisterAccountActivityFrameSpinner.background = resources.getDrawable(R.drawable.blue_rounded_stroke)
            }else{
                bind.vRegisterAccountActivityFrameSpinner.background = resources.getDrawable(R.drawable.gray_rounded_stroke)
            }
        }


        initListeners()
        observeLiveData()
    }

    private fun initSpinners() {
        val accountsAdapter = ArrayAdapter(this, R.layout.spinner_custom, serverList)

        bind.vRegisterActivitySpinnerServers.adapter = accountsAdapter

        bind.vRegisterActivitySpinnerServers.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedServerCountry = serverList[position]
            }
        })

        bind.vRegisterActivitySpinnerServers.setSelection(accountsAdapter.getPosition(serverList[0]))
    }

    private fun observeLiveData() {
        vm.servers.observe(this, Observer {
            serverList = it
            initSpinners()
        })
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
        val login = localRepository.initialUserName
        val password = localRepository.initialPassword

        val settings = localRepository.settings
        val credentials = Credentials(login, password)
        localRepository.settings = localRepository.settings?.copy(credentials = credentials)
    }

    private fun initListeners() {
        bind.buttonSignUp.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.button_sign_up ->{
                val userName = bind.editTextName.text.toString().trim()
                val email = bind.editTextEmail.text.toString().trim()
                val password = bind.editTextPassword.text.toString().trim()

                if(checkFields(userName, email, password)){
                    vm.registerUser(userName, email, password, selectedServerCountry)
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