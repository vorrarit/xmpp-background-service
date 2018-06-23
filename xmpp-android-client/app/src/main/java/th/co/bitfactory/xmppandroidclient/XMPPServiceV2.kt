package th.co.bitfactory.xmppandroidclient

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import io.fabric.sdk.android.Fabric
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.packet.DelayInformation
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.*

class XMPPServiceV2 : LifecycleService() {

    private val random = Random()
    private var notificationId = 1
    private var threadId = 0L

    private var isStarted = false
    private var mThread: Thread? = null
    private var mHandler: Handler? = null
    private var xmppClientV2: XMPPClientV2? = null

    companion object {
        val TAG = XMPPServiceV2::class.java.name
    }

    init {
        threadId = Thread.currentThread().id
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
        super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        printLog("XMPPServiceV2 onStartCommand $isStarted $mThread")
        if (!isStarted) {
            isStarted = true
            if (mThread == null || !mThread!!.isAlive) {
                printLog("XMPPServiceV2 create new thread")
                mThread = Thread(Runnable {
                    Looper.prepare()
                    mHandler = Handler()
                    xmppClientV2 = XMPPClientV2(
                            "openfire.bitfactory.co.th",
                            "openfire.bitfactory.co.th",
                            "user2",
                            "password",
                            createCustomTrustSSLContext())
                    xmppClientV2!!.connect()
                    Looper.loop()
                })
                mThread!!.start()
            }

        }

        xmppClientV2?.let {
            if (!it.isConnected()) {
                Log.d(TAG, "manually reconnect from service")
                mHandler?.post(Runnable {
                    try {
                        it.connect()
                    } catch (e: Exception) {
                        printLog(e.message!!)
                    }

                })
            }

            printLog("add observation")
            it.messageLiveData?.observe(this, object: Observer<Message> {
                override fun onChanged(message: Message?) {
                    message?.let {
                        val delayInfo: DelayInformation? = message.getExtension("delay", "urn:xmpp:delay")
                        if (delayInfo == null) {
                            printLog("***no delay***")
                        } else {
                            printLog("***delay***")
                        }

                        val launcherIntent = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                        val pendingIntent = TaskStackBuilder.create(applicationContext)
                                .addParentStack(launcherIntent.component)
                                .addNextIntent(launcherIntent)
                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

                        val builder = NotificationCompat.Builder(applicationContext, getString(R.string.channel_id))
                                .setContentIntent(pendingIntent)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(message?.body)
                                .setContentText(message?.body)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setDefaults(NotificationCompat.DEFAULT_ALL)
                                .setAutoCancel(true)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(notificationId++, builder.build())
                    }
                }
            })
        }


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        FirebaseAnalytics.getInstance(this)
        printLog("XMPPServiceV2 onCreate")
    }

    override fun onDestroy() {
        printLog("XMPPServiceV2 onDestroy")
        isStarted = false
        mHandler?.let{
            it.post(Runnable {
                xmppClientV2?.let {
                    it.disconnect()
                }
            })
        }
        super.onDestroy()
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

        val certStream =  applicationContext.resources.openRawResource(R.raw.ejabberd)
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


