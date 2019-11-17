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