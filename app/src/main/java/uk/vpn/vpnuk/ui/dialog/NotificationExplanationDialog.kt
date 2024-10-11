/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.databinding.DialogNotificationExplanationBinding

class NotificationExplanationDialog(): DialogFragment() {

    lateinit var bind: DialogNotificationExplanationBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogNotificationExplanationBinding.inflate(LayoutInflater.from(requireContext()))


        bind.title.text = "Mandatory permission"
        bind.message.movementMethod = LinkMovementMethod.getInstance()

        bind.button.setOnClickListener {
            val settingsIntent: Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, 123)
            startActivity(settingsIntent)

            dismiss()
        }

        return AlertDialog.Builder(requireActivity())
            .setView(bind.root)
            .create()
    }


}