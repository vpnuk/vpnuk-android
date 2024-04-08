/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.murgupluoglu.flagkit.FlagKit
import uk.vpn.vpnuk.remote.Server

fun Server.getIconResourceName(context: Context) =
    context.getImageResByName("${location.icon.toLowerCase()}1")

fun Server.getIsoDrawable(context: Context) : Drawable? {
    var iso = this.location.icon.toLowerCase()
    when(iso){
        "uk" -> iso = "gb"
    }

    return FlagKit.getDrawable(context, iso)
}

fun Server.getIsoIcon(context: Context) : IconCompat {
    var iso = this.location.icon.toLowerCase()
    when(iso){
        "uk" -> iso = "gb"
    }

    return IconCompat.createWithAdaptiveBitmap(FlagKit.getDrawable(context, iso)!!.toBitmap())
}
