package th.co.bitfactory.xmppandroidclient

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main.*

/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityFragment : Fragment() {

    private fun isMyServiceRunning(serviceClass: Class<XMPPServiceV2>): Boolean {
        context?.let {
            val manager = it.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getRunningServices(Int.MAX_VALUE).forEach {
                if (it.service.className == serviceClass.name) {
                    return true
                }
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onResume() {
        super.onResume()
        text1.text = if (isMyServiceRunning(XMPPServiceV2::class.java)) "service running" else "service die"
    }


}
