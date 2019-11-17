package uk.vpn.vpnuk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.core.App
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ProfileManager
import kotlinx.android.synthetic.main.activity_main.*
import uk.vpn.vpnuk.remote.Repository
import uk.vpn.vpnuk.utils.*
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var repository: Repository

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = Repository.instance(this)
        tabsSocketType.setTabs(SocketType.values().map { it.value })
        tabsSocketType.setTabListener {
            tabsPort.setTabs(SocketType.byValue(it)!!.ports)
        }
        tabsSocketType.getTabAt(0)!!.select()
        fabSelectAddress.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerListActivity::class.java))
        }
        etLogin.addTextChangedListener {
            updateBtConnectState()
        }
        etPassword.addTextChangedListener {
            updateBtConnectState()
        }
        if (BuildConfig.DEBUG) {
            etLogin.setText("stan")
            etPassword.setText("stan")
        }
        btConnect.setOnClickListener {
            val currentServer = repository.getCurrentServer()!!
            startVpn(
                etLogin.text.toString(),
                etPassword.text.toString(),
                currentServer.address!!,
                tabsSocketType.selectedTab().text.toString(),
                tabsPort.selectedTab().text.toString()
            )
        }

    }

    private fun updateBtConnectState() {
        btConnect.isEnabled =
            etLogin.text.isNotEmpty() && etPassword.text.isNotEmpty() && repository.getCurrentServer() != null
    }

    override fun onStart() {
        super.onStart()
        repository.getCurrentServer()?.let {
            tvAddress.text = it.address
            tvDns.text = it.dns
            tvCity.text = it.location!!.city
            fabSelectAddress.setImageResource(getImageResByName("${it.location!!.icon!!.toLowerCase()}1"))
        }
        updateBtConnectState()
    }

    @Throws(IOException::class)
    private fun getTextFromAsset(): String {
        var reader: BufferedReader? = null
        val stringBuilder = StringBuilder()
        try {
            reader = BufferedReader(
                InputStreamReader(assets.open("openvpn.txt"), "UTF-8")
            )

            // do reading, usually loop until end of file reading
            reader.readLines().forEach {
                stringBuilder.append(it)
                stringBuilder.append('\n')
            }
        } catch (e: IOException) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    //log the exception
                }

            }
        }
        return stringBuilder.toString()
    }

    private fun startVpn(userName: String, password: String, ip: String, socket: String, port: String) {
        App.connection_status = 1
        var inputStream: ByteArrayInputStream? = null
        var bufferedReader: BufferedReader? = null
        inputStream =
            ByteArrayInputStream(prepareConfig(ip, socket, port))
        bufferedReader =
            BufferedReader(InputStreamReader(inputStream))

        val cp = ConfigParser()
        cp.parseConfig(bufferedReader)

        var vp = cp.convertProfile()

        vp.mName = Build.MODEL
        vp.mUsername = userName
        vp.mPassword = password

        val pm = ProfileManager.getInstance(this@MainActivity)
        pm.addProfile(vp)
        pm.saveProfileList(this@MainActivity)
        pm.saveProfile(this@MainActivity, vp)
        vp = pm.getProfileByName(Build.MODEL)
        val intent = Intent(applicationContext, LaunchVPN::class.java)
        intent.putExtra(LaunchVPN.EXTRA_KEY, vp.getUUID().toString())
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
        App.isStart = false
    }

    private fun prepareConfig(ip: String, socket: String, port: String) =
        getTextFromAsset()
            .replace("<ip>", ip)
            .replace("<port>", port)
            .replace("<socket>", socket)
            .toByteArray(Charset.forName("UTF-8"))
}
