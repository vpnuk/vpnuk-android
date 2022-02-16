/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.amazonVersion

import androidx.lifecycle.ViewModel
import uk.vpn.vpnuk.data.repository.VpnAccountRepository

class AmazonMainVM : ViewModel() {

    val repo = VpnAccountRepository()
    val vpnAccounts = repo.vpnAccountsList


    fun findActiveVpnAccount(login: String, password: String) {
        repo.getVpnAccountsFirstLoginAttempt(login, password)
    }
}