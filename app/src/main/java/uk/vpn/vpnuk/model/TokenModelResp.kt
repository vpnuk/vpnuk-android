/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName




data class TokenModelResp(
    @SerializedName("access_token")
    @Expose
    var accessToken: String? = null,

    @SerializedName("token_type")
    @Expose
    var tokenType: String? = null
)