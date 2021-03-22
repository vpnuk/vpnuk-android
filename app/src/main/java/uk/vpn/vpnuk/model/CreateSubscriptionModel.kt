/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class CreateSubscriptionModel(
    @SerializedName("product_id")
    @Expose
    var productId: String? = null,

    @SerializedName("product_id_source")
    @Expose
    var productIdSource: String? = null,

    @SerializedName("country")
    @Expose
    var country: String? = null
)