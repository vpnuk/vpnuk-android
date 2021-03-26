/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.quickLaunch

import androidx.lifecycle.ViewModel
import uk.vpn.vpnuk.data.repository.RegisterUserRepository

class QuickLaunchVM : ViewModel(){

    val repo = RegisterUserRepository()


    val tokenSuccess = repo.successToken
    val isUserRegisteredFromApp = repo.isUserRegisteredFromApp
    val isSubscriptionExpired = repo.isSubscriptionExpired


    fun checkRegisteredSource(email: String, source: String = "app"){
        repo.checkRegisteredSource(email, source)
    }

    fun checkSubscriptionState(productId: Int) {
        repo.checkSubscriptionState(productId)
    }

    fun updateToken() {
        repo.getToken()
    }

    fun renewSubscription(amazonReceiptId: String, amazonUserId: String, pendingOrderId: String) {
        repo.renewSubscription(amazonUserId, amazonReceiptId, pendingOrderId)
    }
}