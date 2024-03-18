/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageWebsites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.vpn.vpnuk.databinding.ItemDomainBinding

class WebsitesRecyclerAdapter(
    var items: MutableList<String>,
    val deleteListener: (domain: String, position: Int) -> Unit
) : RecyclerView.Adapter<WebsitesRecyclerAdapter.WebsitesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsitesViewHolder {
        return WebsitesViewHolder(
            ItemDomainBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: WebsitesViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, deleteListener, position)
    }

    class WebsitesViewHolder(private val view: ItemDomainBinding) : RecyclerView.ViewHolder(view.root){
        fun bind(
            item: String,
            deleteListener: (domain: String, position: Int) -> Unit,
            position: Int
        ) {
            view.tvDomainName.text = item
            view.ivDelete.setOnClickListener { deleteListener(item, position) }
        }
    }
}