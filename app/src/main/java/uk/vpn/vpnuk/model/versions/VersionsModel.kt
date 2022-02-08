/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model.versions

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class VersionsModel(
    @SerializedName("servers")
    @Expose
    val servers: String? = null,

    @SerializedName("dns")
    @Expose
    val dns: String? = null,

    @SerializedName("ovpn")
    @Expose
    val ovpn: String? = null,

    @SerializedName("win32_generic")
    @Expose
    val win32Generic: String? = null
)