package jp.ad.wide.sfc.arch.attendancesystem

import android.app.Application
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

/**
 * AppController
 */
class AppController : Application() {

    private val TAG: String = AppController::class.java.simpleName

    private val requestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    fun addToRequestQueue(req: JsonObjectRequest, tag: String?) {
        // set the default tag if tag is empty
        req.tag = if (tag.isNullOrEmpty()) TAG else tag
        requestQueue.add(req)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        requestQueue.add(req)
    }

    fun cancelPendingRequests(tag: Any) {
        requestQueue?.cancelAll(tag)
    }
}
