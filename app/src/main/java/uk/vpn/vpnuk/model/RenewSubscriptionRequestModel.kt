/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class RenewSubscriptionRequestModel(
    @SerializedName("amazon_user_id")
    @Expose
    var amazon_user_id: String? = null,

    @SerializedName("receipt_id")
    @Expose
    var receipt_id: String? = null
) {
}