/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.local

data class Settings(
    val socket: String,
    val port: String,
    val reconnect : Boolean,
    val credentials: Credentials?
)

data class Credentials(
    val login: String,
    val password: String
)