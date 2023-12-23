/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.amazonVersion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uk.vpn.vpnuk.api.ServerListVaultApi
import uk.vpn.vpnuk.api.VpnUkInfoApi
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.data.repository.VpnAccountRepository
import uk.vpn.vpnuk.ui.splash.SplashScreenVM
import javax.inject.Inject

@HiltViewModel
class AmazonMainVM @Inject constructor(
    private val localRepository: LocalRepository,
    private val serverListApi: ServerListVaultApi,
    private val vpnUkInfoApi: VpnUkInfoApi,
) : ViewModel() {

    val repo = VpnAccountRepository()
    val vpnAccounts = repo.vpnAccountsList
    val error = repo.error

    fun findActiveVpnAccount(login: String, password: String) {
        repo.getVpnAccountsFirstLoginAttempt(login, password)
    }

    fun updateServers() = viewModelScope.launch {
        when(val request = serverListApi.getServerList()){
            is NetworkResponse.Success ->{
                Log.d("kek", "Saving servers Amaz request3 - $request")

                localRepository.serversList = request.body.servers ?: listOf()

                Log.d("kek", "servers saved. LocalRepo - ${localRepository.serversList}")
            }
            is NetworkResponse.Error ->{}
            else -> {}
        }
    }
}