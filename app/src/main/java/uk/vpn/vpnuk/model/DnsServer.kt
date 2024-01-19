/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName




data class DnsServer(
    @SerializedName("name")
    @Expose
    var name: String? = null,

    @SerializedName("primary")
    @Expose
    var primary: String? = null,

    @SerializedName("secondary")
    @Expose
    var secondary: String? = null
)