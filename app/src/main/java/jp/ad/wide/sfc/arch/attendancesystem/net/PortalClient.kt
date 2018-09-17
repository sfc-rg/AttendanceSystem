package jp.ad.wide.sfc.arch.attendancesystem.net

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import jp.ad.wide.sfc.arch.attendancesystem.AppController
import org.json.JSONObject

class PortalClient(
    private val appController: AppController,
    private val successListener: Response.Listener<JSONObject>,
    private val errorListener: Response.ErrorListener,
    private val accessToken: String
) {

    fun createAttendance(studentNumber: String) {
        val parameters = JSONObject().apply {
            put("access_token", accessToken)
            put("student_id", studentNumber)
        }

        val jsonObjReq = JsonObjectRequest(
            Request.Method.POST,
            "$API_V1_URL/attendances",
            parameters,
            successListener,
            errorListener
        )
        appController.addToRequestQueue(jsonObjReq, "/attendances")
    }

    companion object {
        private const val API_V1_URL = "https://portal.sfc.wide.ad.jp/api/v1"
    }
}
