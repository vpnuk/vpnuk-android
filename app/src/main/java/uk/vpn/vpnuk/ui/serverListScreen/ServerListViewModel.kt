/*
 * Copyright (c) 2023 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.serverListScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.api.ServerListVaultApi
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.remote.Server
import uk.vpn.vpnuk.ui.splash.SplashScreenVM
import javax.inject.Inject

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val serverListApi: ServerListVaultApi,
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState()
    )
    val viewState = _viewState.asStateFlow()

    private val _oneShotEvents = MutableSharedFlow<OneShotEvent>()
    val oneShotEvents = _oneShotEvents.asSharedFlow()


    fun getServerList() = viewModelScope.launch {
        when(val request = serverListApi.getServerList()){
            is NetworkResponse.Success ->{
                localRepository.serversList = request.body.servers  ?: listOf()
                _viewState.emit(viewState.value.copy(serverList = request.body.servers  ?: listOf()))
            }
            is NetworkResponse.Error ->{}
            else -> {}
        }
    }


    data class ViewState(
        val serverList: List<Server> = listOf()
    )
    sealed class OneShotEvent {
        object NavigateToQuickLaunch : OneShotEvent()
        class ErrorToast(val message: String = "") : OneShotEvent()
    }
    sealed class UiAction {
        class Nothing() : UiAction()
    }
}