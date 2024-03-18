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
import uk.vpn.vpnuk.utils.emitFlow
import javax.inject.Inject


@HiltViewModel
class ManageWebsitesViewModel @Inject constructor(
    private val localRepository: LocalRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(ManageWebsitesViewState())
    val viewState = _viewState.asStateFlow()


    fun getExcludedWebsites() {
        emitFlow(_viewState, _viewState.value.copy(excludedWebsitesList = localRepository.excludedWebsites))
    }

    fun onDeleteWebsiteClick(domain: String) {
        val newList = _viewState.value.excludedWebsitesList.toMutableList()
        newList.remove(domain)
        localRepository.excludedWebsites = newList

        _viewState.value.excludedWebsitesList = newList

        if(newList.isEmpty()){
            emitFlow(_viewState, _viewState.value.copy(excludedWebsitesList = newList))
        }
    }

    fun onAddWebsiteClick() {
        val newDomain = _viewState.value.domainText

        if(!localRepository.excludedWebsites.contains(newDomain)){
            val excludedWebsites = localRepository.excludedWebsites.toMutableList()
            excludedWebsites.add(newDomain)
            localRepository.excludedWebsites = excludedWebsites

            emitFlow(_viewState, _viewState.value.copy(excludedWebsitesList = excludedWebsites, domainText = ""))
        }
    }

    fun onDomainTextChanged(text: CharSequence?) {
        val text = text.toString()
        emitFlow(_viewState, _viewState.value.copy(domainText = text))
    }


    data class ManageWebsitesViewState(
        var excludedWebsitesList: List<String> = listOf(),
        val domainText: String = "",
    )
}