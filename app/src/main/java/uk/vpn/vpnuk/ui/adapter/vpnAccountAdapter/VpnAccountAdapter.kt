/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.adapter.vpnAccountAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.vpn.vpnuk.databinding.ItemVpnAccountBinding
import uk.vpn.vpnuk.model.subscriptionModel.Vpnaccount

class VpnAccountAdapter(
    val context: Context,
    val items: List<Vpnaccount>,
    val listener: (account: Vpnaccount) -> Unit
) : RecyclerView.Adapter<VpnAccountAdapter.VpnAccountVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpnAccountVH {
        return VpnAccountVH(ItemVpnAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VpnAccountVH, position: Int) {
        val item = items[position]
        holder.bind(item, listener)
    }

    override fun getItemCount() = items.size


    class VpnAccountVH(private val view: ItemVpnAccountBinding) : RecyclerView.ViewHolder(view.root) {
        fun bind(item: Vpnaccount, listener: (account: Vpnaccount) -> Unit){
            val serverName = item.server?.description.toString().split("Server:")[1].split("<")[0]

            view.vVpnAccountItemServerName.text = serverName
            view.vVpnAccountItemUsername.text = item.username
            view.vVpnAccountItemPassword.text = item.password

            view.root.setOnClickListener {
                listener(item)
            }
        }
    }
}