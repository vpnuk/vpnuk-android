/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageWebsites

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.databinding.ActivityManageAppsBinding
import uk.vpn.vpnuk.databinding.ActivityManageWebsitesBinding


class ManageWebsitesActivity : BaseActivity() {

    private lateinit var bind: ActivityManageWebsitesBinding
    private val vm: ManageWebsitesViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityManageWebsitesBinding.inflate(layoutInflater)
        setContentView(bind.root)

        initView()
    }

    private fun initView() {

    }
}