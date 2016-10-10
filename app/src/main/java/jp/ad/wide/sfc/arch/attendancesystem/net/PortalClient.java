package jp.ad.wide.sfc.arch.attendancesystem.net;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import jp.ad.wide.sfc.arch.attendancesystem.AppController;

public class PortalClient {
    final static String API_V1_URL = "https://portal.sfc.wide.ad.jp/api/v1";
    protected String mAccessToken;
    protected Response.Listener<JSONObject> mSuccessListener;
    protected Response.ErrorListener mErrorListener;

    public PortalClient(Response.Listener<JSONObject> successListener, Response.ErrorListener errorListener, String accessToken) {
        mSuccessListener = successListener;
        mErrorListener = errorListener;
        mAccessToken = accessToken;
    }

    public void createAttendance(String studentNumber) {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("access_token", mAccessToken);
            parameters.put("student_id", studentNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST, API_V1_URL + "/attendances", parameters, mSuccessListener, mErrorListener);
        AppController.getInstance().addToRequestQueue(jsonObjReq, "/attendances");
    }
}
