/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.settingsScreen
import androidx.lifecycle.ViewModel
import uk.vpn.vpnuk.data.repository.RegisterUserRepository
import uk.vpn.vpnuk.data.repository.VpnAccountRepository


class SettingsViewModel : ViewModel() {

    val repo = VpnAccountRepository()
    val registerRepo = RegisterUserRepository()


    val allSubscriptions = repo.allSubscriptions
    val isRegisteredFromApp = registerRepo.isUserRegisteredFromApp
    val isSubscriptionExpired = registerRepo.isSubscriptionExpired

    var selectedSubscription = 0


    fun getServerList() {
        repo.getAllVpnAccounts()
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


}