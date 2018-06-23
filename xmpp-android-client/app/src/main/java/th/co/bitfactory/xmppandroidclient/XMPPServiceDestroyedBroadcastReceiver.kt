package th.co.bitfactory.xmppandroidclient

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.util.*
import android.support.v4.content.ContextCompat.startForegroundService



class XMPPServiceDestroyedBroadcastReceiver: BroadcastReceiver() {

    private val random = Random()

    companion object {
        val TAG = XMPPServiceDestroyedBroadcastReceiver::javaClass.name
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        printLog("receive destroyed message")
        context?.let {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                var builder = JobInfo.Builder(0, ComponentName(context, StartXMPPServiceJob::class.java))
//                        .setMinimumLatency(1000)
//                        .setOverrideDeadline(3000)
//
//                context.getSystemService(JobScheduler::class.java)
//                        .schedule(builder.build())
//            } else {
//                context.startService(Intent(context, XMPPService::class.java))
//            }

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(Intent(context, XMPPService::class.java))
//            } else {
//                context.startService(Intent(context, XMPPService::class.java))
//            }

            context.startService(Intent(context, XMPPService::class.java))
        }
    }

    private fun printLog(message:String) {
        Log.d(TAG, message)
        Crashlytics.log(message)
        FirebaseMessaging.getInstance()
                .send(RemoteMessage.Builder("1081019550804@gcm.googleapis.com")
                        .setMessageId(Integer.toString(random.nextInt(9999)))
                        .addData("XMPPServiceDestroyedBroadcastReceiver",  "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} Thread ID " + Thread.currentThread().id + " " + message).build()
                )
    }
}