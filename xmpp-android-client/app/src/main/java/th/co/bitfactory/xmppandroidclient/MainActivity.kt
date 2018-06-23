package th.co.bitfactory.xmppandroidclient

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import de.measite.minidns.util.InetAddressUtil

import kotlinx.android.synthetic.main.activity_main.*
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import java.lang.Exception
import android.content.Context
import android.os.Build
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import io.fabric.sdk.android.Fabric
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = MainActivity::class.java.name
    }

    private lateinit var analytics: FirebaseAnalytics
    private val random = Random()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        printLog("onCreate")
        analytics = FirebaseAnalytics.getInstance(this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.channel_name)
            val channelId = getString(R.string.channel_id)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }

        fab.setOnClickListener { view ->

            startService(Intent(this, XMPPServiceV2::class.java))

            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    0,
                    Intent(this, XMPPServiceV2::class.java),
                    PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    Calendar.getInstance().timeInMillis,
                    1000 * 60 * 5,
                    pendingIntent
            )

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        printLog("onResume")
    }

    override fun onPause() {
        super.onPause()
        printLog("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        printLog("onDestroy")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                sendBroadcast(Intent(this, XMPPServiceDestroyedBroadcastReceiver::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun printLog(message:String) {
        Log.d(TAG, message)
        Crashlytics.log(message)
        FirebaseMessaging.getInstance()
                .send(RemoteMessage.Builder("1081019550804@gcm.googleapis.com")
                        .setMessageId(Integer.toString(random.nextInt(9999)))
                        .addData("MainActivity", "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} Thread ID " + Thread.currentThread().id + " " + message).build()
                )
    }
}
