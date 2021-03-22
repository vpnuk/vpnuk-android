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
import com.syject.scout.api.RestClient
import de.blinkt.openvpn.core.App
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.vpn.vpnuk.model.*
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel


class RegisterUserRepository() {

    private val errorMutable = MutableLiveData<String>()
    private val successTokenMutable = MutableLiveData<Boolean>()
    private val createdSubscriptionMutable = MutableLiveData<SubscriptionsModel>()

    val error: LiveData<String> = errorMutable
    val successToken: LiveData<Boolean> = successTokenMutable
    val createSubscription: LiveData<SubscriptionsModel> = createdSubscriptionMutable


    var context: Context = App.instance.applicationContext

    val localRepository = LocalRepository(context)


    fun registerUser(userName: String, email: String, password: String) {
        val userModel = RegisterModel(userName, password, "null", "null", email)
        val body = Gson().toJson(userModel)

        RestClient.getApi().registerNewCustomer(userModel)
            .enqueue(object : Callback<ErrorModel> {
                override fun onResponse(call: Call<ErrorModel>, response: Response<ErrorModel>) {
                    if (response.isSuccessful) {
                        localRepository.initialUserName = userName
                        localRepository.initialPassword = password
                        localRepository.initialEmail = email

                        getToken()
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }

                override fun onFailure(call: Call<ErrorModel>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    fun getToken(){
        val userName = localRepository.initialUserName
        val password = localRepository.initialPassword

        RestClient.getApi().getToken("password", userName, password)
            .enqueue(object : Callback<TokenModelResp> {
                override fun onResponse(
                    call: Call<TokenModelResp>,
                    response: Response<TokenModelResp>
                ) {
                    if (response.isSuccessful) {
                        localRepository.token = response.body()?.accessToken.toString()

                        successTokenMutable.postValue(true)
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


    fun createSubscription(){
        val subModel = CreateSubscriptionModel(
            "6633",
            "vpnuk",
            "United Kingdom"
        )
        val token = "Bearer ${localRepository.token}"

        RestClient.getApi().createSubscription(token, subModel)
            .enqueue(object : Callback<CreatedSubscriptionResp> {
                override fun onResponse(
                    call: Call<CreatedSubscriptionResp>,
                    response: Response<CreatedSubscriptionResp>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null && response.body()?.id != null) {
                            localRepository.purchasedSubId = response.body()?.id!!

                            getSubscriptionInfo(localRepository.purchasedSubId)
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }

                override fun onFailure(call: Call<CreatedSubscriptionResp>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    fun getSubscriptionInfo(subId: Int) {
        val token = "Bearer ${localRepository.token}"
        val productId = subId.toString()

        RestClient.getApi().getSubInfo(token, productId)
            .enqueue(object : Callback<SubscriptionsModel> {
                override fun onResponse(
                    call: Call<SubscriptionsModel>,
                    response: Response<SubscriptionsModel>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            createdSubscriptionMutable.postValue(body)

                            val serverName = body.vpnaccounts?.get(0)?.server?.description.toString().split("Server:")[1].split("<")[0]

                            localRepository.vpnUsername = body.vpnaccounts?.get(0)?.username.toString()
                            localRepository.vpnPassword = body.vpnaccounts?.get(0)?.password.toString()
                            localRepository.vpnIp = body.vpnaccounts?.get(0)?.server?.ip.toString()
                            localRepository.vpnDescription = body.vpnaccounts?.get(0)?.server?.description.toString()
                            localRepository.vpnServerName = serverName
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message)
                    }
                }

                override fun onFailure(call: Call<SubscriptionsModel>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }
}