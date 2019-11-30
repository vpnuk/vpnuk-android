package uk.vpn.vpnuk

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

enum class ConnectionState(@StringRes val nameId: Int) {
    LEVEL_CONNECTED(R.string.status_level_connected),
    LEVEL_VPNPAUSED(R.string.status_level_vpnpaused),
    LEVEL_CONNECTING_SERVER_REPLIED(R.string.status_level_connecting_server_replied),
    LEVEL_CONNECTING_NO_SERVER_REPLY_YET(R.string.status_level_connecting_no_server_reply_yet),
    LEVEL_NONETWORK(R.string.status_level_nonetwork),
    LEVEL_NOTCONNECTED(R.string.status_level_notconnected),
    LEVEL_START(R.string.status_level_start),
    LEVEL_AUTH_FAILED(R.string.status_level_auth_failed),
    LEVEL_WAITING_FOR_USER_INPUT(R.string.status_level_waiting_for_user_input),
    UNKNOWN_LEVEL(R.string.status_unknown_level);

    @ColorInt
    fun color(context: Context): Int {
        val colorRes = when(this) {
            LEVEL_CONNECTED -> R.color.colorPrimary
            LEVEL_NOTCONNECTED -> R.color.colorAccent2
            else -> R.color.colorOtherStatus
        }
        return ContextCompat.getColor(context, colorRes)
    }
}