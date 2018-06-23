package th.co.bitfactory.th.co.bitfactory.xmpppcclient

import de.measite.minidns.util.InetAddressUtil
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class KtXmppClient(val username:String) {

    private var config: XMPPTCPConnectionConfiguration
    private var conn: AbstractXMPPConnection
    private var chatManager: ChatManager
    private var reconnectionManager: ReconnectionManager

    init {
        SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager")

        config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, "password")
                .setXmppDomain("openfire.bitfactory.co.th")
                .setHostAddress(InetAddressUtil.ipv4From("openfire.bitfactory.co.th"))
                .setPort(5222)
                .setDebuggerEnabled(true)
                .setCustomSSLContext(createCustomTrustSSLContext())
                .build()

        conn = XMPPTCPConnection(config)
        conn.addConnectionListener(object : ConnectionListener {
            override fun reconnectionSuccessful() {
                printLog("***reconnection success***")
            }

            override fun reconnectionFailed(e: Exception?) {
                printLog("***reconnection failed***")
            }

            override fun reconnectingIn(seconds: Int) {
                printLog("***reconnecting in***")
            }

            override fun connected(connection: XMPPConnection?) {
                printLog("***connected***")
            }

            override fun connectionClosedOnError(e: Exception?) {
                e?: let { printLog(e!!.message!!) }
            }

            override fun connectionClosed() {
                printLog( "***closed***")
            }

            override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
                printLog("***authenticated***")

            }
        })

        reconnectionManager = ReconnectionManager.getInstanceFor(conn)
        reconnectionManager.enableAutomaticReconnection()

        chatManager = ChatManager.getInstanceFor(conn)
        chatManager?.addIncomingListener(object:IncomingChatMessageListener {

            override fun newIncomingMessage(from: EntityBareJid?, message: Message?, chat: Chat?) {
                message?.let {
                    var delayInfo: DelayInformation? = message.getExtension("delay", "urn:xmpp:delay")
                    printLog("***new incoming message***")
                    printLog("***$delayInfo***")
                    printLog("[${message.from}] ${message.body}")
                }
            }
        })

    }

    fun connect() = conn.connect().login()

    fun disconnect() = conn.disconnect()

    fun send(toUser:String, message:String) {
        val chat = chatManager.chatWith(JidCreate.entityBareFrom("$toUser@openfire.bitfactory.co.th"))
        val msg = Message()
        msg.body = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) + " " + message
        chat.send(msg)
    }

    private fun createTrustAllSSLContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(object: X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext
    }

    private fun createCustomTrustSSLContext(): SSLContext {
        val keyStorePassword = "password"

        val certStream = javaClass.classLoader.getResourceAsStream("ejabberd.cer")
        val certificates = CertificateFactory.getInstance("X.509")
                .generateCertificates(certStream)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, keyStorePassword.toCharArray())
        keyStore.setCertificateEntry("1", certificates.first())

        val keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        )
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(keyStore)

        val trustManagers = trustManagerFactory.trustManagers

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustManagers, java.security.SecureRandom())
        return sslContext
    }

    fun printLog(message: String) {
        println("[$username] ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} $message")
    }
}

fun main(args: Array<String>) {
    var running = true

    if (args.size < 1) {
        println("please input username")
        return
    }

    val xmppClient = KtXmppClient(args[0])
    xmppClient.connect()

    println("Press Q to Quit. ")
    while (running) {
        val input = readLine().toString().split(" ")

        when (input[0].trim().toLowerCase()) {
            "q" -> {
                xmppClient.disconnect()
                running = false
            }
            "s" -> {
                val message = input.filterIndexed{idx, it -> idx > 1 }.reduce{ s1, s2 -> "$s1 $s2" }
                xmppClient.send(input[1], message)
            }
            "d" -> xmppClient.disconnect()
            "c" -> xmppClient.connect()
        }

        Thread.sleep(400)
    }
}