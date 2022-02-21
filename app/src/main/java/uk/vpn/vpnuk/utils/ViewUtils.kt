/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.utils

import android.text.SpannableString
import android.text.style.URLSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.widget.textChanges
import uk.vpn.vpnuk.URLSpanNoUnderline

fun View.gone() {
    this.visibility = View.GONE
}
fun View.visible(){
    this.visibility = View.VISIBLE
}

fun TextView.stripUnderlines() {
    val s = SpannableString(text)
    val spans = s.getSpans(0, s.length, URLSpan::class.java)
    for (span in spans) {
        val start = s.getSpanStart(span)
        val end = s.getSpanEnd(span)
        s.removeSpan(span)
        val newSpan = URLSpanNoUnderline(span.url)
        s.setSpan(newSpan, start, end, 0)
    }
    text = s
}

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

