package uk.vpn.vpnuk.utils

import android.widget.EditText
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*

fun TabLayout.setTabs(list: List<String>) {
    removeAllTabs()
    list.forEach {
        addTab(newTab().apply {
            text = it
        })
    }
}

fun TabLayout.selectedTab() = getTabAt(selectedTabPosition)!!

fun TabLayout.setTabListener(listener: (String) -> Unit) {
    addOnTabSelectedListener(object :
        TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
        override fun onTabReselected(p0: TabLayout.Tab) {
            listener.invoke(p0.text.toString())
        }

        override fun onTabUnselected(p0: TabLayout.Tab) {
        }

        override fun onTabSelected(p0: TabLayout.Tab) {
            listener.invoke(p0.text.toString())
        }

    })
}
