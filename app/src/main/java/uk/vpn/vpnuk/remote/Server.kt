/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.remote

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ServerVersion(
    val servers: String
)

data class Servers(
    @SerializedName("servers")
    @Expose
    val servers: List<Server>?
)

data class Server(
    @SerializedName("type")
    @Expose
    val type: String,
    @SerializedName("address")
    @Expose
    val address: String,
    @SerializedName("dns")
    @Expose
    val dns: String,
    //val speed: String,
    @SerializedName("location")
    @Expose
    val location: Location
)

data class Location(
    @SerializedName("icon")
    @Expose
    val icon: String,
    @SerializedName("city")
    @Expose
    val city: String,
    @SerializedName("name")
    @Expose
    val name: String
)
