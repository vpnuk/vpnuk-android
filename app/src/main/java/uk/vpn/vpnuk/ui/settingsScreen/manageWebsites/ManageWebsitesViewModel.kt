/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageWebsites

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.vpn.vpnuk.data.repository.LocalRepository
import javax.inject.Inject


@HiltViewModel
class ManageWebsitesViewModel @Inject constructor(
    private val localRepository: LocalRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(ManageWebsitesViewState())
    val viewState = _viewState.asStateFlow()




    data class ManageWebsitesViewState(
        val excludedWebsitesList: List<String> = listOf()
    )
}