package th.co.bitfactory.xmppandroidclient

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
class StartXMPPServiceJob: JobService() {

    private val random = Random()

    companion object {
        val TAG = StartXMPPServiceJob::javaClass.name
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        printLog("onStartJob")
        applicationContext.startService(Intent(applicationContext, XMPPService::class.java))
        return  true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        printLog("onStopJob")
        return true
    }

    private fun printLog(message:String) {
        Log.d(TAG, message)
        Crashlytics.log(message)
        FirebaseMessaging.getInstance()
                .send(RemoteMessage.Builder("1081019550804@gcm.googleapis.com")
                        .setMessageId(Integer.toString(random.nextInt(9999)))
                        .addData("StartXMPPServiceJob",  "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())} Thread ID " + Thread.currentThread().id + " " + message).build()
                )
    }
}