/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import uk.vpn.vpnuk.api.RestClient
import de.blinkt.openvpn.core.App
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.vpn.vpnuk.model.*
import uk.vpn.vpnuk.model.serverList.ServerListModel
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel


class RegisterUserRepository() {

    private val errorMutable = MutableLiveData<String>()
    private val successTokenMutable = MutableLiveData<Boolean>()
    private val createdSubscriptionMutable = MutableLiveData<SubscriptionsModel>()
    private val isUserRegisteredFromAppMutable = MutableLiveData<Boolean>()
    private val isSubscriptionExpiredMutable = MutableLiveData<Pair<Boolean, String>>()
    private val serverListMutable = MutableLiveData<List<String>>()

    val error: LiveData<String> = errorMutable
    val successToken: LiveData<Boolean> = successTokenMutable
    val createSubscription: LiveData<SubscriptionsModel> = createdSubscriptionMutable
    val isUserRegisteredFromApp: LiveData<Boolean> = isUserRegisteredFromAppMutable
    val isSubscriptionExpired: LiveData<Pair<Boolean, String>> = isSubscriptionExpiredMutable
    val serverList: LiveData<List<String>> = serverListMutable


    var context: Context = App.instance.applicationContext

    val localRepository = LocalRepository(context)


    fun registerUser(
        userName: String,
        email: String,
        password: String,
        selectedServerCountry: String
    ) {
        val userModel = RegisterModel(userName, password, userName, "firetv", email, "app")

        RestClient.getApi().registerNewCustomer(userModel)
            .enqueue(object : Callback<ErrorModel> {
                override fun onResponse(call: Call<ErrorModel>, response: Response<ErrorModel>) {
                    if (response.isSuccessful) {
                        localRepository.initialUserName = userName
                        localRepository.initialPassword = password
                        localRepository.initialEmail = email

                        localRepository.cachedSelectedCountry = selectedServerCountry

                        getToken()
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message?:"")
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
                override fun onResponse(call: Call<TokenModelResp>, response: Response<TokenModelResp>) {
                    if (response.isSuccessful) {
                        localRepository.token = response.body()?.accessToken.toString()

                        successTokenMutable.postValue(true)
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message?:"")
                    }
                }
                override fun onFailure(call: Call<TokenModelResp>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }


    fun createSubscription(){
        val selectedCountry = localRepository.cachedSelectedCountry

        val subModel = CreateSubscriptionModel("6633", "vpnuk", selectedCountry)
        val token = "Bearer ${localRepository.token}"

        RestClient.getApi().createSubscription(token, subModel)
            .enqueue(object : Callback<CreatedSubscriptionResp> {
                override fun onResponse(call: Call<CreatedSubscriptionResp>, response: Response<CreatedSubscriptionResp>) {
                    if (response.isSuccessful) {
                        if (response.body() != null && response.body()?.id != null) {
                            localRepository.purchasedSubId = response.body()?.id!!

                            getSubscriptionInfo(localRepository.purchasedSubId)
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message?:"")
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
                override fun onResponse(call: Call<SubscriptionsModel>, response: Response<SubscriptionsModel>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            createdSubscriptionMutable.postValue(body!!)

                            val serverName = body.vpnaccounts?.get(0)?.server?.description.toString().split("Server:")[1].split("<")[0]

                            localRepository.vpnUsername = body.vpnaccounts?.get(0)?.username.toString()
                            localRepository.vpnPassword = body.vpnaccounts?.get(0)?.password.toString()
                            localRepository.vpnIp = body.vpnaccounts?.get(0)?.server?.ip.toString()
                            localRepository.vpnDescription = body.vpnaccounts?.get(0)?.server?.description.toString()
                            localRepository.vpnServerName = serverName
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message?:"")
                    }
                }
                override fun onFailure(call: Call<SubscriptionsModel>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    fun renewSubscription(amazonUserId: String, receiptId: String, pendingOrderId: String){
        val token = localRepository.token
        val body = RenewSubscriptionRequestModel(
            amazonUserId,
            receiptId
        )

        RestClient.getApi().renewSubscription(token, body, pendingOrderId)
            .enqueue(object : Callback<ResponseBody>{
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if(response.isSuccessful){
                        Log.d("kek", "Renewing!!!   Success - ${response.code()}")
                    }else{
                        Log.d("kek", "Renewing!!!   No Success - ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("kek", "Renewing!!!   Failure - ${t.message}")
                }
            })
    }

    fun checkRegisteredSource(email: String, source: String){
        RestClient.getApi().checkIfUserRegisteredFromSource(email, source)
            .enqueue(object : Callback<ResponseBody>{
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if(response.isSuccessful){
                        isUserRegisteredFromAppMutable.postValue(true)
                    }else{
                        isUserRegisteredFromAppMutable.postValue(false)
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isUserRegisteredFromAppMutable.postValue(false)
                }
            })
    }

    fun checkSubscriptionState(productId: Int) {
        val token = "Bearer ${localRepository.token}"

        RestClient.getApi().getSubInfo(token, productId.toString())
            .enqueue(object : Callback<SubscriptionsModel> {
                override fun onResponse(call: Call<SubscriptionsModel>, response: Response<SubscriptionsModel>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            if(body.status == "on-hold"){
                                val pendingOrderId = body.pending_orders?.get(0)?.orderId.toString()

                                if(pendingOrderId != ""){
                                    isSubscriptionExpiredMutable.postValue(Pair(true, pendingOrderId))
                                }
                            }else{
                                isSubscriptionExpiredMutable.postValue(Pair(false, ""))
                            }
                        }
                    } else {
                        val error = Gson().fromJson<ErrorModel>(response.errorBody()!!.string(), ErrorModel::class.java)
                        errorMutable.postValue(error.message?:"")
                    }
                }
                override fun onFailure(call: Call<SubscriptionsModel>, t: Throwable) {
                    errorMutable.postValue(t.message)
                }
            })
    }

    fun getServerList() {
        RestClient.getApi("https://www.serverlistvault.com/")
            .getServerList()
            .enqueue(object : Callback<ServerListModel>{
                override fun onResponse(call: Call<ServerListModel>, response: Response<ServerListModel>) {
                    if(response.isSuccessful){
                        val serverListModel = response.body()
                        val servers = serverListModel?.country?.map { it.location.toString() }

                        serverListMutable.postValue(servers!!)
                    }
                }
                override fun onFailure(call: Call<ServerListModel>, t: Throwable) {
                }
            })
    }
}