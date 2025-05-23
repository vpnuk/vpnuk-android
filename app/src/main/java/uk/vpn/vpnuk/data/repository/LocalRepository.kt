/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.model.AppInfo
import uk.vpn.vpnuk.model.DnsServer
import uk.vpn.vpnuk.remote.Server
import javax.inject.Inject
import kotlin.reflect.KProperty


class LocalRepository @Inject constructor(context: Context) {

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

    inner class JsonListPreferenceDelegate<T>(clazz: Class<T>) {
        private val typeToken = TypeToken.getParameterized(ArrayList::class.java, clazz).type
        operator fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
            val json = sharedPreferences.getString(property.name, "")!!
            if (json.isEmpty()) return arrayListOf()
            return gson.fromJson(json, typeToken)
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>?) =
            sharedPreferences
                .edit()
                .putString(property.name, gson.toJson(value))
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
    var cachedSelectedCountry by StringPreferenceDelegate() //UK or USA (user can choose different servers when creating new account from Amazon)



    var previousOvpnConfigVersion by StringPreferenceDelegate()
    var newOvpnConfigTxt by StringPreferenceDelegate()
    var currentServer by JsonPreferenceDelegate(Server::class.java)
    var settings by JsonPreferenceDelegate(Settings::class.java)


    var isAppDownloadedFromAmazon by BooleanPreferenceDelegate()
    var isLoginByUserCreds by BooleanPreferenceDelegate(false)
    var serversList by JsonListPreferenceDelegate(Server::class.java)
    var customDns by JsonPreferenceDelegate(DnsServer::class.java)
    var allAppsInfoList by JsonListPreferenceDelegate(AppInfo::class.java)
    var excludedApps by JsonListPreferenceDelegate(AppInfo::class.java)
    var excludedWebsites by JsonListPreferenceDelegate(String::class.java)
    var useObfuscation by BooleanPreferenceDelegate(false)

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