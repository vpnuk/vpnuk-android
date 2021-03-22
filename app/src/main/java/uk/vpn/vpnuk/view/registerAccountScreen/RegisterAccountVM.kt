/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.view.registerAccountScreen

import androidx.lifecycle.ViewModel
import uk.vpn.vpnuk.data.repository.RegisterUserRepository

class RegisterAccountVM : ViewModel() {

    val repo = RegisterUserRepository()

    val successToken = repo.successToken
    val createdSubscription = repo.createSubscription

    val error = repo.error


    fun registerUser(userName: String, email: String, password: String) {
        repo.registerUser(userName, email, password)
    }

    fun createSubscription() {
        repo.createSubscription()
    }


}