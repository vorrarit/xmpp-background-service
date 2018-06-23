package th.co.bitfactory.xmppandroidclient

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import de.measite.minidns.util.InetAddressUtil
import io.fabric.sdk.android.Fabric
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.Jid
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class XMPPService : LifecycleService() {

    private var notificationId = 1
    private val random = Random()
    private var isStarted = false
    private var threadId = 0L

    companion object {
        val TAG = XMPPService::javaClass.name
        val xmppClient = XMPPClient.getInstance("openfire.bitfactory.co.th", "openfire.bitfactory.co.th", "user2", "password")
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        printLog("***xmpp service on bind***")
        return MyBinder()
    }

    override fun onCreate() {
        super.onCreate()
//        startForeground(1, Notification())
        threadId = Thread.currentThread().id
        Fabric.with(this, Crashlytics())
        printLog("***xmpp service on create***")
    }

    override fun onDestroy() {
        super.onDestroy()
        xmppClient.disconnect()
        printLog("***xmpp service on destroy***")

        var broadcastIntent = Intent(this, XMPPServiceDestroyedBroadcastReceiver::class.java)
        sendBroadcast(broadcastIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        printLog("***xmpp service on start command***")

        if (!xmppClient.isConnected()) {
            try {
                printLog("xmppClient state " + xmppClient.state.toString())
                xmppClient.start()
            } catch (e: IllegalThreadStateException) {
                printLog("xmppClient is already running")
                xmppClient.connect()
            }

            isStarted = true
        }

        var messageLiveData = xmppClient.messageLiveData
        messageLiveData.observe(this, object:Observer<Message> {
            override fun onChanged(message: Message?) {
                message?.let {
                    var delayInfo: DelayInformation? = message.getExtension("delay", "urn:xmpp:delay")
                    if (delayInfo == null) {
                        printLog("***no delay***")
                    } else {
                        printLog("***delay***")
                    }

                    var launcherIntent = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                    var pendingIntent = TaskStackBuilder.create(applicationContext)
                            .addParentStack(launcherIntent.component)
                            .addNextIntent(launcherIntent)
                            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                    var builder = NotificationCompat.Builder(applicationContext, getString(R.string.channel_id))
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(message?.body)
                            .setContentText(message?.body)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setAutoCancel(true)
                    var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationId++, builder.build())
                }
            }
        })

        return START_STICKY
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

    inner class MyBinder: Binder() {
        fun getService(): XMPPService {
            return this@XMPPService
        }
    }
}