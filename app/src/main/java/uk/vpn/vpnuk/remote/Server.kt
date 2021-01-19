/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.remote

data class ServerVersion(
    val servers: String
)

data class Servers(
    val servers: List<Server>
)

data class Server(
    val type: String,
    val address: String,
    val dns: String,
    //val speed: String,
    val location: Location
)

data class Location(
    val icon: String,
    val city: String,
    val name: String
)
