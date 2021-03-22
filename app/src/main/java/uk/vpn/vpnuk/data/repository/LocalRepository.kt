/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.reflect.KProperty

open class LocalRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(VPNUK_PREFS, Context.MODE_PRIVATE);

    private val gson = Gson()

    inner class StringPreferenceDelegate(private val defaultValue: String = "") {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String =
            sharedPreferences.getString(property.name, defaultValue)!!

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
            sharedPreferences
                .edit()
                .putString(property.name, value)
                .apply()
    }

    inner class BooleanPreferenceDelegate(private val defaultValue: Boolean = false) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            sharedPreferences.getBoolean(property.name, defaultValue)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
            sharedPreferences
                .edit()
                .putBoolean(property.name, value)
                .apply()
    }

    inner class JsonPreferenceDelegate<T>(private val clazz: Class<T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val json = sharedPreferences.getString(property.name, "null")!!
            if (json.isEmpty())
                return null
            return gson.fromJson(json, clazz)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) =
            sharedPreferences
                .edit()
                .putString(property.name, gson.toJson(value))
                .apply()
    }

    inner class IntPreferenceDelegate(private val defaultValue: Int = 0) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            sharedPreferences.getInt(property.name, defaultValue)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
            sharedPreferences
                .edit()
                .putInt(property.name, value)
                .apply()
    }



    var token by StringPreferenceDelegate()

    var initialUserName by StringPreferenceDelegate()
    var initialPassword by StringPreferenceDelegate()
    var initialEmail by StringPreferenceDelegate()


    var vpnUsername by StringPreferenceDelegate()
    var vpnPassword by StringPreferenceDelegate()
    var vpnDescription by StringPreferenceDelegate()
    var vpnIp by StringPreferenceDelegate()
    var vpnServerName by StringPreferenceDelegate()
    
    var purchasedSubId by IntPreferenceDelegate()



    fun clear() {
        sharedPreferences
            .edit()
            .clear()
            .apply()
    }

    companion object {
        const val VPNUK_PREFS = "VPNUK_PREFS"
    }
}