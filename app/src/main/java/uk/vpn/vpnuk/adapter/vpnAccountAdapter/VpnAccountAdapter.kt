/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.adapter.vpnAccountAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vpn_account.view.*
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount
import uk.vpn.vpnuk.view.ServerViewHolder

class VpnAccountAdapter(
    val context: Context,
    val items: List<Vpnaccount>,
    val listener: (account: Vpnaccount) -> Unit
) : RecyclerView.Adapter<VpnAccountAdapter.VpnAccountVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpnAccountVH {
        return VpnAccountVH(LayoutInflater.from(parent.context).inflate(R.layout.item_vpn_account, parent, false))
    }

    override fun onBindViewHolder(holder: VpnAccountVH, position: Int) {
        val item = items[position]


        val serverName = item.server?.description.toString().split("Server:")[1].split("<")[0]

        holder.itemView.vVpnAccountItemServerName.text = serverName
        holder.itemView.vVpnAccountItemUsername.text = item.username
        holder.itemView.vVpnAccountItemPassword.text = item.password

        holder.itemView.setOnClickListener {
            listener(item)
        }
    }

    override fun getItemCount() = items.size


    class VpnAccountVH(view: View) : RecyclerView.ViewHolder(view)
}