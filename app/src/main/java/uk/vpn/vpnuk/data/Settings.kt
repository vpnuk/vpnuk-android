/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.local

data class Settings(
    val socket: String,
    val port: String,
    val reconnect: Boolean,
    val mtu: String?,
    val credentials: Credentials?
)

data class Credentials(
    val login: String,
    val password: String
)

object DefaultSettings{
    const val MTU_DEFAULT = "1500"
    val MTU_LIST = arrayOf(
        "1100",
        "1150",
        "1200",
        "1250",
        "1300",
        "1350",
        "1400",
        "1450"
    )
}
