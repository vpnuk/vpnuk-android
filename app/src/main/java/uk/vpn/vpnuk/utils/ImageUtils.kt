/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.utils

import android.R.attr.name
import android.content.Context
import androidx.annotation.DrawableRes

@DrawableRes
fun Context.getImageResByName(name: String): Int {
    return resources.getIdentifier(
        name, "drawable",
        packageName
    )
}