/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.utils

import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout


fun String.isEmailValid(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}


fun TextInputLayout.doOnTextChange(listener: (newText: String) -> Unit) {
    this.editText?.doOnTextChanged { text, start, before, count -> listener(text.toString()) }
}

fun TextInputLayout.getText() : String {
    return this.editText?.text.toString()
}

fun TextInputLayout.setText(text: String?) {
    this.editText?.setText(text)
    this.editText?.setSelection(this.editText?.text?.length ?: 0)
}