/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.mainScreen

import androidx.lifecycle.ViewModel
import uk.vpn.vpnuk.data.repository.VpnAccountRepository

class MainVM : ViewModel() {

    val repo = VpnAccountRepository()

    val vpnAccounts = repo.vpnAccountsList


    fun findActiveVpnAccount(login: String, password: String) {
        repo.getVpnAccountsFirstLoginAttempt(login, password)
    }
}