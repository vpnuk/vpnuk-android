/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */
package uk.vpn.vpnuk.ui.splash

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.api.ServerListVaultApi
import uk.vpn.vpnuk.api.VpnUkInfoApi
import uk.vpn.vpnuk.data.repository.ApplicationsInfoRepository
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.local.Credentials
import uk.vpn.vpnuk.local.DefaultSettings
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.utils.asLiveData
import javax.inject.Inject

@HiltViewModel
class SplashScreenVM @Inject constructor(
    private val localRepository: LocalRepository,
    private val serverListApi: ServerListVaultApi,
    private val vpnUkInfoApi: VpnUkInfoApi,
    private val applicationsInfoRepository: ApplicationsInfoRepository
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val _oneShotEvents = MutableSharedFlow<OneShotEvent>()
    val oneShotEvents = _oneShotEvents.asSharedFlow()


    fun onCreate() = viewModelScope.launch {
        createFirstSettings()
        checkVpnVersion()
        updateServersList()
        cacheInstalledApplications()
    }

    private fun createFirstSettings() {
        val settings =localRepository.settings
        if(
            settings == null ||
            settings.credentials == null ||
            settings.port == null ||
            settings.socket == null ||
            settings.mtu == null ||
            settings.port == "" ||
            settings.socket == "" ||
            settings.mtu == ""
            ) {
            localRepository.settings = Settings("udp", "1194", false, DefaultSettings.MTU_DEFAULT, Credentials("", ""))
        }
    }

    private fun cacheInstalledApplications() = viewModelScope.launch {
        applicationsInfoRepository.saveAllAppsLocally()
    }

    private fun updateServersList() = viewModelScope.launch {
        when(val request = serverListApi.getServerList()){
            is NetworkResponse.Success ->{
                localRepository.serversList = request.body.servers  ?: listOf()
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.emit(OneShotEvent.ErrorToast(request.error.message.toString()))
            }
            else -> {}
        }

        _viewState.emit(_viewState.value.copy(loadingTextToDisplay = "Checking server list..."))
    }

    private fun checkVpnVersion() = viewModelScope.launch {
        when(val request = serverListApi.getVersions()){
            is NetworkResponse.Success ->{
                Log.d("kek", "checkVpnVersion = ${request}")
                if(request.body.ovpn != localRepository.previousOvpnConfigVersion){
                    localRepository.previousOvpnConfigVersion = request.body.ovpn.toString()

                    getNewOvpnConfig()
                }else{
                    _oneShotEvents.emit(OneShotEvent.NavigateToQuickLaunch)
                }
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.emit(OneShotEvent.ErrorToast(request.error.message.toString()))
            }
            else -> {}
        }

        _viewState.emit(_viewState.value.copy(loadingTextToDisplay = "Checking for update..."))
    }

    private fun getNewOvpnConfig() = viewModelScope.launch {
        when(val request = serverListApi.getOVPNConfig()){
            is NetworkResponse.Success ->{
                val ovpnTxtConfigString = request.body.string()
                localRepository.newOvpnConfigTxt = ovpnTxtConfigString
                Log.d("kek", "getNewOvpnConfig = $request")
                _oneShotEvents.emit(OneShotEvent.NavigateToQuickLaunch)
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.emit(OneShotEvent.ErrorToast(request.error.message.toString()))
            }
            else -> {}
        }

        _viewState.emit(_viewState.value.copy(loadingTextToDisplay = "Updating vpn config..."))
    }


    data class ViewState(
        val loadingTextToDisplay: String = "Checking for update...",
    )
    sealed class OneShotEvent {
        object NavigateToQuickLaunch : OneShotEvent()
        class ErrorToast(val message: String = "") : OneShotEvent()
    }
    sealed class UiAction {
        class Nothing() : UiAction()
    }
}