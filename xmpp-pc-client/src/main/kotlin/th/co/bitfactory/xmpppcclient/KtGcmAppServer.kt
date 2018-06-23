package th.co.bitfactory.xmpppcclient

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.measite.minidns.util.InetAddressUtil
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.provider.ProviderManager
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLSocketFactory

class KtGcmAppServer {

    var xmppClientThread:Thread? = null

    fun stop() {
        xmppClientThread?.interrupt()
    }

    fun start() {
        xmppClientThread = object:Thread() {
            var config: XMPPTCPConnectionConfiguration
            var conn: AbstractXMPPConnection
            var chatManager: ChatManager? = null
            var roster: Roster
            var reconnectionManager: ReconnectionManager

            init {
//                config = XMPPTCPConnectionConfiguration.builder()
//                        .setUsernameAndPassword("user2", "password")
//                        .setXmppDomain("openfire.bitfactory.co.th")
//                        .setHostAddress(InetAddressUtil.ipv4From("openfire.bitfactory.co.th"))
//                        .setPort(5222)
//                        .setDebuggerEnabled(true)
//                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
//                        .build()
                SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager")
                config = XMPPTCPConnectionConfiguration.builder()
                        .setXmppDomain("gcm.googleapis.com")
                        .setUsernameAndPassword("1081019550804@gcm.googleapis.com", "AIzaSyAMlid8JYjMGvzOjl9WyaQ9EGxyj8RYbFQ")
                        .setHostAddress(InetAddressUtil.ipv4From("fcm-xmpp.googleapis.com"))
                        .setPort(5236)
                        .setDebuggerEnabled(false)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                        .setSocketFactory(SSLSocketFactory.getDefault())
                        .setSendPresence(false)
                        .build()

                conn = XMPPTCPConnection(config)

                reconnectionManager = ReconnectionManager.getInstanceFor(conn)
                reconnectionManager.enableAutomaticReconnection()

                roster = Roster.getInstanceFor(conn)
                roster.isRosterLoadedAtLogin = false

            }

            override fun run() {
                try {
                    conn.addConnectionListener(object : ConnectionListener {
                        override fun reconnectionSuccessful() {
                            println("***reconnection success***")
                        }

                        override fun reconnectionFailed(e: Exception?) {
                            println("***reconnection failed***")
                        }

                        override fun reconnectingIn(seconds: Int) {
                            println("***reconnecting in***")
                        }

                        override fun connected(connection: XMPPConnection?) {
                            println("***connected***")
                        }

                        override fun connectionClosedOnError(e: Exception?) {
                            e?: let { println(e!!.message!!) }
                        }

                        override fun connectionClosed() {
                            println( "***closed***")
                        }

                        override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
                            println("***authenticated***")
                            chatManager = ChatManager.getInstanceFor(conn)
//                            chatManager?.addIncomingListener(object:IncomingChatMessageListener {
//
//                                override fun newIncomingMessage(from: EntityBareJid?, message: Message?, chat: Chat?) {
//                                    message?.let {
//                                        var delayInfo: DelayInformation? = message.getExtension("delay", "urn:xmpp:delay")
//                                        println("***new incoming message***")
//                                        println("***$delayInfo***")
//                                        println("***${message.body}***")
//                                        println("***$conn***")
//                                    }
//                                }
//                            })
                        }
                    })


                    ProviderManager.addExtensionProvider("gcm",
                            "gcm:mobile:data",
                            object:ExtensionElementProvider<GcmPacketExtension>() {
                                override fun parse(parser: XmlPullParser?, initialDepth: Int): GcmPacketExtension {
                                    println("parsing in gcm provider")
                                    parser?.let {
                                        var json = parser.nextText()
                                        return GcmPacketExtension(json)
                                    }
                                    throw Exception("parser is null")
                                }
                            })

                    conn.addAsyncStanzaListener(
                            object:StanzaListener {
                                override fun processStanza(packet: Stanza?) {
                                    packet?.let {
//                                        println(it.toXML().toString())

                                        var delayInfo: DelayInformation? = packet.getExtension("delay", "urn:xmpp:delay")
                                        if (delayInfo == null) {
                                            var packetExtension = packet.getExtension("google:mobile:data") as GcmPacketExtension
                                            var jGcmMessage = JsonParser().parse(packetExtension.json).asJsonObject
                                            if (!jGcmMessage.has("message_type") && jGcmMessage.has("from")) {
                                                sendAck(jGcmMessage["from"].asString, jGcmMessage["message_id"].asString)
                                                var jData = jGcmMessage.getAsJsonObject("data")
                                                jData.keySet().forEach {
                                                    println("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} $it -> ${jGcmMessage.get("message_id").asString} ${jData[it].toString()}")
                                                }
                                            } else {
                                                println("not implemented")
                                                println(packet.toString())
                                            }
                                        } else {

                                        }

                                    }
                                }

                            },
                            object :StanzaFilter {
                                override fun accept(stanza: Stanza?): Boolean {
                                    stanza?.let {
                                        if (stanza.hasExtension("gcm", "google:mobile:data")) {
//                                            println("filter accepted")
                                            return true
                                        } else {
                                            println("filter rejected")
                                            return false
                                        }
                                    }
                                    return false
                                }

                            }
                    )

                    conn.connect().login()

                } catch (e: InterruptedException) {
                    println("closed by interupt")
                    conn.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun interrupt() {
                super.interrupt()
                println("closed by interrupt")
                conn.disconnect()
            }

            fun sendAck(toUser:String, messageId:String) {
                var mapResponse = mapOf<String, String>("to" to toUser, "message_id" to messageId, "message_type" to "ack")
                var jsonRequest = GsonBuilder().create().toJson(mapResponse)
//                println(jsonRequest)
                var message = Message()
                message.addExtension(GcmPacketExtension(jsonRequest))
                conn.sendStanza(message)
            }
        }

        xmppClientThread!!.start()

    }


}

fun main(args: Array<String>) {
    val xmppClient = KtGcmAppServer()
    xmppClient.start()
    var input:String = ""

    println("Press Q to Quit. ")
    while (true) {
        input = readLine().toString()

        if (input.trim().toLowerCase() == "q") {
            xmppClient.stop()
            break
        }
    }
}