/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */
package uk.vpn.vpnuk.ui.splash

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
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.utils.asLiveData
import javax.inject.Inject

@HiltViewModel
class SplashScreenVM @Inject constructor(
    private val localRepository: LocalRepository,
    private val serverListApi: ServerListVaultApi,
    private val vpnUkInfoApi: VpnUkInfoApi,
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val _oneShotEvents = MutableSharedFlow<OneShotEvent>()
    val oneShotEvents = _oneShotEvents.asSharedFlow()


    fun onAction(action: UiAction) {

    }

    fun onCreate() = viewModelScope.launch {
        checkVpnVersion()
    }

    private fun checkVpnVersion() = viewModelScope.launch {
        when(val request = serverListApi.getVersions()){
            is NetworkResponse.Success ->{
                if(request.body.ovpn != localRepository.previousOvpnConfigVersion){
                    localRepository.previousOvpnConfigVersion = request.body.ovpn.toString()

                    getNewOvpnConfig()
                }else{
                    _oneShotEvents.tryEmit(OneShotEvent.NavigateToQuickLaunch)
                }
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.tryEmit(OneShotEvent.ErrorToast(request.error.message.toString()))
            }
        }

        _viewState.tryEmit(_viewState.value.copy(loadingTextToDisplay = "Checking for update..."))
    }

    private fun getNewOvpnConfig() = viewModelScope.launch {
        when(val request = serverListApi.getOVPNConfig()){
            is NetworkResponse.Success ->{
                val ovpnTxtConfigString = request.body.string()
                localRepository.newOvpnConfigTxt = ovpnTxtConfigString

                _oneShotEvents.tryEmit(OneShotEvent.NavigateToQuickLaunch)
            }
            is NetworkResponse.Error ->{
                _oneShotEvents.tryEmit(OneShotEvent.ErrorToast(request.error.message.toString()))
            }
        }

        _viewState.tryEmit(_viewState.value.copy(loadingTextToDisplay = "Updating vpn config..."))
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