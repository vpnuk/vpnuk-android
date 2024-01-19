/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.api.ServerListVaultApi
import uk.vpn.vpnuk.api.VpnUkInfoApi
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.data.repository.RegisterUserRepository
import uk.vpn.vpnuk.data.repository.VpnAccountRepository
import uk.vpn.vpnuk.model.DnsServer
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel
import uk.vpn.vpnuk.utils.asLiveData
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val vpnUkInfoApi: VpnUkInfoApi,
    private val serverListVaultApi: ServerListVaultApi
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()
    private val _oneShotEvents = MutableSharedFlow<OneShotEvent>()
    val oneShotEvents = _oneShotEvents.asSharedFlow()

    private val _allSubscriptionsLive = MutableLiveData<List<SubscriptionsModel>>()
    val allSubscriptionsLive = _allSubscriptionsLive.asLiveData()

    //Refactor this some day...
    val registerRepo = RegisterUserRepository()
    val isRegisteredFromApp = registerRepo.isUserRegisteredFromApp
    val isSubscriptionExpired = registerRepo.isSubscriptionExpired

    var selectedSubscription = 0


    fun onCreate() {
        initAmazonGoogleViews()
        initAmazonApi()
        getCustomDNS()
    }

    private fun getCustomDNS() = viewModelScope.launch {
        when(val result = serverListVaultApi.getCustomDns()){
            is NetworkResponse.Success -> {
                val servers = result.body.dns.apply {
                        this.add(0, DnsServer(name = "Default"))
                    }

                _viewState.update {
                    it.copy(customDnsServers = servers)
                }
            }
            else -> {}
        }
    }

    private fun initAmazonApi() {
        if(localRepository.isAppDownloadedFromAmazon){
            getServerList()
        }
    }

    private fun initAmazonGoogleViews() {
        if(localRepository.isAppDownloadedFromAmazon){
            _viewState.tryEmit(viewState.value.copy(amazonApiSettingsVisible = true))
        }else{
            _viewState.tryEmit(viewState.value.copy(amazonApiSettingsVisible = false))
        }
    }

    private fun getServerList() = viewModelScope.launch {
        val login = localRepository.initialUserName
        val password = localRepository.initialPassword

        _viewState.tryEmit(viewState.value.copy(serverProgressView = true))

        when(val resultToken = vpnUkInfoApi.getTokenCoroutine("password", login, password)){
            is NetworkResponse.Success ->{
                localRepository.token = resultToken.body.accessToken.toString()
                val token = "Bearer ${localRepository.token}"

                when(val resultSubscriptions = vpnUkInfoApi.getAllSubscriptionsCoroutine(token)){
                    is NetworkResponse.Success ->{
                        val subscriptions = resultSubscriptions.body

                        _allSubscriptionsLive.postValue(subscriptions)
                        _viewState.emit(viewState.value.copy(serverProgressView = false))
                    }
                    is NetworkResponse.Error ->{
                        _oneShotEvents.tryEmit(OneShotEvent.ErrorToast(resultSubscriptions.error.message.toString()))
                    }
                    else -> {}
                }
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.tryEmit(OneShotEvent.ErrorToast(resultToken.error.message.toString()))
            }
            else -> {}
        }
    }

    fun checkIfRegisteredFromApp(initialEmail: String, subscription: Int) {
        registerRepo.checkRegisteredSource(initialEmail, "app")
        selectedSubscription = subscription
    }

    fun getPendingOrders() {
        registerRepo.checkSubscriptionState(selectedSubscription)
    }

    fun renewSubscription(amazonReceiptId: String, amazonUserId: String, pendingOrderId: String) {
        registerRepo.renewSubscription(amazonUserId, amazonReceiptId, pendingOrderId)
    }

    fun onCustomDnsSelected(position: Int) {
        val selectedDns = _viewState.value.customDnsServers[position]

        if(selectedDns.secondary != null && selectedDns.primary != null){
            localRepository.customDns = selectedDns
        }else{
            localRepository.customDns = null
        }
    }


    data class ViewState(
        val amazonApiSettingsVisible: Boolean = false,
        val serverProgressView: Boolean = false,
        val customDnsServers: List<DnsServer> = listOf(),
    )
    sealed class OneShotEvent {
        class ErrorToast(val message: String = "") : OneShotEvent()
    }
}