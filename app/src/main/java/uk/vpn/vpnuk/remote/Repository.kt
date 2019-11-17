package uk.vpn.vpnuk.remote

import android.content.Context
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.xml.sax.InputSource
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
//import retrofit2.converter.jaxb.JaxbConverterFactory
import retrofit2.http.GET
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter

//import javax.xml.bind.JAXBContext
//import javax.xml.bind.Marshaller

interface Requests {
    @GET("servers.xml")
    fun getServers(): Single<Servers>
}

class Repository(val context: Context) {
    val prefs = context.getSharedPreferences("servers", Context.MODE_PRIVATE)
    val retrofit = Retrofit.Builder()
        .baseUrl("https://www.vpnuk.info/serverlist/")
        .client(OkHttpClient.Builder()/*.addInterceptor(HttpLoggingInterceptor())*/.build())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(TikXmlConverterFactory.create())
        .build()
    //    Convertr
    val api = retrofit.create(Requests::class.java)
//    val jaxbContext = JAXBContext.newInstance(Servers::class.java)
//    val createMarshaller = jaxbContext.createMarshaller().apply {
//        setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
//    }
//    val unmarshaller = jaxbContext.createUnmarshaller()

    fun setServerId(id: String) {
        prefs.edit()
            .putString("id", id)
            .apply()
    }

    fun getCurrentServer() = getServers().find { prefs.getString("id", null) == it.address }

    fun getServers() =
        listOf(
            Server("shared", "78.129.194.131", "shared61.vpnuk.net", "UK", "Maidenhead", "UK 61"),
            Server("shared", "107.150.40.26", "shared8-us.vpnuk.net", "US", "Kansas", "US 8"),
            Server("shared", "67.215.4.146", "shared1-ca.vpnuk.net", "CA", "Montreal", "CA 1"),
            Server("shared", "67.215.4.170", "shared2-ca.vpnuk.net", "CA", "Montreal", "CA 2"),
            Server("shared", "80.74.131.84", "shared1-ch.vpnuk.net", "CH", "Zurich", "CH 1")
        )


//        api.getServers()
//        .map {
//            it.string()
//        }
//        .doOnSuccess {
//            prefs.edit()
//                .putString("servers", it)
//                .apply()

//            val stringWriter = StringWriter()
//            createMarshaller.marshal(it, stringWriter)
//            prefs.edit()
//                .putString("servers", stringWriter.toString())
//        }
//        .map {
//            val servers = unmarshaller.unmarshal(InputSource(StringReader(it)))
//            return@map (servers as Servers).servers
//        }

    companion object {
        var repository: Repository? = null
        fun instance(context: Context): Repository {
            if (repository == null) {
                repository = Repository(context.applicationContext)
            }
            return repository!!
        }
    }
}