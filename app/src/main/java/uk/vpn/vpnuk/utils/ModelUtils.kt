package uk.vpn.vpnuk.utils

import android.content.Context
import uk.vpn.vpnuk.remote.Server

fun Server.getIconResourceName(context: Context) =
    context.getImageResByName("${location.icon.toLowerCase()}1")