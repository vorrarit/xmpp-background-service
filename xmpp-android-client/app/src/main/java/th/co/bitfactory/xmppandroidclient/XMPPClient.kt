package th.co.bitfactory.xmppandroidclient

import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import de.measite.minidns.util.InetAddressUtil
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.ping.PingManager
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class XMPPClient private constructor(val server:String, val domain:String, val username:String, val password:String):Thread() {

    private val random = Random()

    private var config: XMPPTCPConnectionConfiguration? = null
    private var conn: AbstractXMPPConnection? = null
    private var chatManager: ChatManager? = null
    private var reconnectionManager: ReconnectionManager? = null
    private var roster: Roster? = null
    private var deliveryReceiptManager: DeliveryReceiptManager? = null
    private var isThreadRunning = false
    private var threadId: Long? = 0

    var messageLiveData = SingleLiveEvent<Message>()

    companion object {
        val TAG = XMPPClient::javaClass.name
        var xmppClient: XMPPClient? = null

        fun getInstance(server:String, domain:String, username:String, password:String): XMPPClient {
            if (xmppClient == null) {
                xmppClient = XMPPClient(server, domain, username, password)
            }

            return xmppClient!!
        }
    }

    override fun run() {
        isThreadRunning = true
        threadId = Thread.currentThread().id

        SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager")

        config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, password)
                .setXmppDomain(domain)
                .setHostAddress(InetAddressUtil.ipv4From(server))
                .setPort(5222)
                .setDebuggerEnabled(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setSendPresence(true)
                .build()


        conn = XMPPTCPConnection(config)
        conn?.let {
            it.addConnectionListener(object : ConnectionListener {
                override fun reconnectionSuccessful() {
                    printLog("***reconnection success***")
                }

                override fun reconnectionFailed(e: Exception?) {
                    printLog("***reconnection failed - ${e!!.message}***")
                }

                override fun reconnectingIn(seconds: Int) {
                    printLog("***reconnecting in $seconds***")
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

            if (chatManager == null) {
                chatManager = ChatManager.getInstanceFor(it)
                chatManager?.let {
                    it.addIncomingListener(object : IncomingChatMessageListener {
                        override fun newIncomingMessage(from: EntityBareJid?, message: Message?, chat: Chat?) {
                            printLog("***new incoming message $conn " + message?.toXML().toString() + "***")
                            messageLiveData.postValue(message)
                        }

                    })
                }
            }

            reconnectionManager = ReconnectionManager.getInstanceFor(it)
            reconnectionManager?.let {
                it.enableAutomaticReconnection()
                ReconnectionManager.setEnabledPerDefault(true)
            }

            roster = Roster.getInstanceFor(it)
            roster?.isRosterLoadedAtLogin = false

            deliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(it)
            deliveryReceiptManager?.let {
                it.autoAddDeliveryReceiptRequests()
                it.autoReceiptMode = DeliveryReceiptManager.AutoReceiptMode.always
                it.addReceiptReceivedListener(object: ReceiptReceivedListener {
                    override fun onReceiptReceived(fromJid: Jid?, toJid: Jid?, receiptId: String?, receipt: Stanza?) {
                        printLog("receipt received for $receiptId")
                    }
                })
            }

            ServerPingWithAlarmManager.getInstanceFor(it).isEnabled = true

            connect()

            while (isThreadRunning) {
                printLog(if (conn!!.isConnected!!) "o" else ".")

                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {}

            }
        }

    }

    fun connect() {
        conn?.let {
            Thread(Runnable {
                if (!it.isConnected) it.connect().login()
            }).start()
        }
    }

    fun disconnect() {
        conn?.disconnect()
        isThreadRunning = false
    }

    fun isConnected():Boolean {
        conn?.let {
            return it.isConnected
        }
        return false
    }

    fun send(toUser:String, message:String) {
        chatManager?.let {
            val chat = it.chatWith(JidCreate.entityBareFrom("$toUser@openfire.bitfactory.co.th"))
            val msg = Message()
            msg.body = message
            chat.send(msg)
        }
    }

    fun printLog(message:String) {
        Log.d(TAG, message)
        Crashlytics.log(message)
        FirebaseMessaging.getInstance()
                .send(RemoteMessage.Builder("1081019550804@gcm.googleapis.com")
                        .setMessageId(Integer.toString(random.nextInt(9999)))
                        .addData("XMPPClient",  "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} Thread ID $threadId [${Thread.currentThread().id}] $message").build()
                )
    }
}