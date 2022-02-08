/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import uk.vpn.vpnuk.api.RestClient
import de.blinkt.openvpn.core.App
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.vpn.vpnuk.model.ErrorModel
import uk.vpn.vpnuk.model.TokenModelResp
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount

class VpnAccountRepository {

    private val errorMutable = MutableLiveData<String>()
    private val vpnAccountsListMutable = MutableLiveData<List<Vpnaccount>>()
    private val allSubscriptionsMutable = MutableLiveData<List<SubscriptionsModel>>()

    val error: LiveData<String> = errorMutable
    val vpnAccountsList: LiveData<List<Vpnaccount>> = vpnAccountsListMutable
    val allSubscriptions: LiveData<List<SubscriptionsModel>> = allSubscriptionsMutable


    var context: Context = App.instance.applicationContext
    val localRepository = LocalRepository(context)


    fun getVpnAccountsFirstLoginAttempt(login: String, password: String) {
        RestClient.getApi().getToken("password", login, password)
            .enqueue(object : Callback<TokenModelResp> {
                override fun onResponse(call: Call<TokenModelResp>, response: Response<TokenModelResp>) {
                    if (response.isSuccessful) {
                        localRepository.token = response.body()?.accessToken.toString()
                        localRepository.initialUserName = login
                        localRepository.initialPassword = password

                        getSubscriptions()
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }
                override fun onFailure(call: Call<TokenModelResp>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    private fun getSubscriptions() {
        //todo - set all tokens to retrofit builder
        val token = "Bearer ${localRepository.token}"

        RestClient.getApi().getAllSubscriptions(token)
            .enqueue(object : Callback<List<SubscriptionsModel>> {
                override fun onResponse(call: Call<List<SubscriptionsModel>>, response: Response<List<SubscriptionsModel>>) {
                    if (response.isSuccessful) {
                        val activeSubsList = response.body()?.filter { it.status == "active" }?:listOf()
                        val vpnAccounts = mutableListOf<Vpnaccount>()

                        if(activeSubsList.isNotEmpty()){
                            activeSubsList.forEach { subscription ->
                                subscription.vpnaccounts?.forEach { vpnAccount ->
                                    vpnAccount.subscriptionId = subscription.id ?: 0
                                    vpnAccounts.add(vpnAccount)
                                }
                            }

                            vpnAccountsListMutable.postValue(vpnAccounts)
                        }else{
                            errorMutable.postValue("You don't have any active subscription")
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }
                override fun onFailure(call: Call<List<SubscriptionsModel>>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    fun getAllVpnAccounts() {
        val login = localRepository.initialUserName
        val password = localRepository.initialPassword

        RestClient.getApi().getToken("password", login, password)
            .enqueue(object : Callback<TokenModelResp> {
                override fun onResponse(call: Call<TokenModelResp>, response: Response<TokenModelResp>) {
                    if (response.isSuccessful) {
                        localRepository.token = response.body()?.accessToken.toString()

                        val token = "Bearer ${localRepository.token}"
                        RestClient.getApi().getAllSubscriptions(token)
                            .enqueue(object : Callback<List<SubscriptionsModel>> {
                                override fun onResponse(call: Call<List<SubscriptionsModel>>, response: Response<List<SubscriptionsModel>>) {
                                    if (response.isSuccessful) {
                                        allSubscriptionsMutable.postValue(response.body())
                                    }
                                }
                                override fun onFailure(call: Call<List<SubscriptionsModel>>, t: Throwable) {
                                    errorMutable.postValue(t.message)
                                }
                            })
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }
                override fun onFailure(call: Call<TokenModelResp>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }
}