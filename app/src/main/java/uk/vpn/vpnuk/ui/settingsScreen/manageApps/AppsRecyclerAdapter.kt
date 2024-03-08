/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageApps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.vpn.vpnuk.databinding.ItemAppBinding
import uk.vpn.vpnuk.model.AppInfo


class AppsRecyclerAdapter(
    val context: Context,
    val items: List<AppInfo>,
    val listener: (account: AppInfo, checked: Boolean) -> Unit
) : RecyclerView.Adapter<AppsRecyclerAdapter.AppsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        return AppsViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, listener)
    }

    class AppsViewHolder(private val view: ItemAppBinding) : RecyclerView.ViewHolder(view.root){
        fun bind(item: AppInfo, listener: (account: AppInfo, checked: Boolean) -> Unit) {
            view.ivIcon.setImageDrawable(view.root.context.packageManager.getApplicationIcon(item.packageName))
            view.tvName.text = item.name
            view.checkAppUseVpn.setOnCheckedChangeListener(null)
            view.checkAppUseVpn.isChecked = item.isChecked

            view.checkAppUseVpn.setOnCheckedChangeListener { _, isChecked ->
                if(item.isChecked != isChecked) listener(item, isChecked)
            }
        }
    }
}