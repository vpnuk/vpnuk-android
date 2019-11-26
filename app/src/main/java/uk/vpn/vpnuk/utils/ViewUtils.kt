package uk.vpn.vpnuk.utils

import android.widget.EditText
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.widget.textChanges

fun TabLayout.setTabs(list: List<String>) {
    removeAllTabs()
    list.forEach {
        addTab(newTab().apply {
            text = it
        })
    }
}

fun TabLayout.select(position: Int) =
    getTabAt(position)!!.select()

fun TabLayout.selectedTab() = getTabAt(selectedTabPosition)!!

fun TabLayout.setTabListener(listener: (String, Int) -> Unit) {
    addOnTabSelectedListener(object :
        TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
        override fun onTabReselected(p0: TabLayout.Tab) {
            listener.invoke(p0.text.toString(), selectedTabPosition)
        }

        override fun onTabUnselected(p0: TabLayout.Tab) {
        }

        override fun onTabSelected(p0: TabLayout.Tab) {
            listener.invoke(p0.text.toString(), selectedTabPosition)
        }

    })
}

fun EditText.textEmpty() = this.textChanges().map { it.isEmpty() }

