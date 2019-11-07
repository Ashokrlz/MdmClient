package com.example.mdmclient.Utils;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mdmclient.App.AppConstant;

import org.json.JSONObject;

public class InvokeApi {

    private static RequestQueue mReqQueue;
    private static Context mContext;


    public interface VolleyResponseListener {
        void onError(String message);

        void onResponse(JSONObject response);
    }


    public static void fetchAppConfigs(Context _context, final VolleyResponseListener listener) {
        mContext = _context;
        mReqQueue = Volley.newRequestQueue(mContext);

        JSONObject jsonBody = new JSONObject();

        String appConfigUrl = AppConstant.FETCH_BASE_URL + AppConstant.FETCH_APP_CONFIG_ENDPOINT;


        JsonObjectRequest jr = new JsonObjectRequest(Request.Method.GET, appConfigUrl,
                jsonBody, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                // TODO Auto-generated method stub
                listener.onResponse(response);
            }

        }, new Response.ErrorListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Auto-generated method stub
                try {
                    listener.onError(error.getMessage());
                }catch (Exception e){
                    listener.onError("Error");
                }
            }
        });

        //jr.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mReqQueue.add(jr);
    }
}
