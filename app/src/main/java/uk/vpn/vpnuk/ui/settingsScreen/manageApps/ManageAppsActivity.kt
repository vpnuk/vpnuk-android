/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageApps

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.databinding.ActivityManageAppsBinding
import uk.vpn.vpnuk.model.AppInfo
import uk.vpn.vpnuk.utils.observeFlow


class ManageAppsActivity : BaseActivity() {

    private lateinit var bind: ActivityManageAppsBinding
    private val vm: ManageAppsViewModel by viewModels()

    private lateinit var adapter: AppsRecyclerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityManageAppsBinding.inflate(layoutInflater)
        setContentView(bind.root)
        supportActionBar?.title = "VPN connections for apps"

        vm.getApplications()

        initView()
        observe()
    }

    private fun observe() {
        observeFlow(vm.viewState, ::render)
    }

    private fun render(viewState: ManageAppsViewModel.ManageAppsViewState) {
        initRecycler(viewState.applicationsList)
        initProgress(viewState.isProgress)
    }

    private fun initProgress(progress: Boolean) {
        bind.pbProgress.visibility = if (progress) View.VISIBLE else View.GONE
    }

    private fun initRecycler(applicationsList: List<AppInfo>) {
        val adapter = AppsRecyclerAdapter(context = this, items = applicationsList) { app, checked ->
            vm.onAppItemChecked(app, checked)
        }

        bind.rvAppsList.adapter = adapter
    }

    private fun initView() {
        bind.etSearch.editText?.doOnTextChanged { text, _, _, _ ->
            vm.onSearchTextChange(text)
        }
    }
}