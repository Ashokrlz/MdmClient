package com.example.mdmclient.Utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdmclient.App.SharedPrefConstants;
import com.example.mdmclient.BaseEntity.AppConfigClass;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class AppUtility {

    private static String mAppConfigsStorage = "mdmclient_appconfig";
    private static String mMainStorage = "mdmclient_main";


    public static void parseJSONRespone(Context _context, JSONObject _response) {
        // TODO Auto-generated method stub

         String serverHost = "";
         String pttQueue = "";
         String pttTopic = "";
         String locationTopic = "";
         String locationQueue = "";
         String imeiTopic = "";
         String imeiRouteKey = "";
         String pttRouteKey = "";
         String locationRouteKey = "";
         String webInTopic ="";
         String webInQueue = "";
         String webInRouteKey = "";
         String webOutTopic = "";
         String webOutRouteKey = "";

        try {
            JSONObject responseData = _response;
            if (responseData.has("androidRabbitMQHost")) {
                serverHost = responseData.getString("androidRabbitMQHost");
            }
            if (responseData.has("androidRabbitMQPTTQueue")) {
                pttQueue = responseData.getString("androidRabbitMQPTTQueue");
            }
            if (responseData.has("androidRabbitMQPTTTopic")) {
                pttTopic = responseData.getString("androidRabbitMQPTTTopic");
            }
            if (responseData.has("androidRabbitMQLocationTopic")) {
                locationTopic = responseData.getString("androidRabbitMQLocationTopic");
            }
            if (responseData.has("androidRabbitMQLocationQueue")) {
                locationQueue = responseData.getString("androidRabbitMQLocationQueue");
            }

            if (responseData.has("androidRabbitMQPTTRouteKey")) {
                pttRouteKey = responseData.getString("androidRabbitMQPTTRouteKey");
            }
            if (responseData.has("androidRabbitMQLocationRouteKey")) {
                locationRouteKey = responseData.getString("androidRabbitMQLocationRouteKey");
            }
            if (responseData.has("andriodIMEIWebInTopic")) {
                webInTopic = responseData.getString("andriodIMEIWebInTopic");
            }
            if (responseData.has("andriodIMEIWebInQueue")) {
                webInQueue = responseData.getString("andriodIMEIWebInQueue");
            }
            if (responseData.has("andriodIMEIWebInRouteKey")) {
                webInRouteKey = responseData.getString("andriodIMEIWebInRouteKey");
            }
            if (responseData.has("andriodIMEIWebOutTopic")) {
                webOutTopic = responseData.getString("andriodIMEIWebOutTopic");
            }
            if (responseData.has("andriodIMEIWebOutRouteKey")) {
                webOutRouteKey = responseData.getString("andriodIMEIWebOutRouteKey");
            }

            storeAppConfigs(new AppConfigClass(serverHost, pttQueue, pttTopic, locationTopic,
                    locationQueue, imeiTopic, imeiRouteKey, pttRouteKey, locationRouteKey, webInTopic, webInQueue, webInRouteKey,
                    webOutTopic, webOutRouteKey ), _context);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }



    public static void storeAppConfigs(AppConfigClass _config, Context _ctx){
        SharedPreferences pref = _ctx.getSharedPreferences(mAppConfigsStorage, Context.MODE_PRIVATE);
        SharedPreferences.Editor edt = pref.edit();

        Gson gson = new Gson();
        String json = gson.toJson(_config);
        edt.putString(SharedPrefConstants.APP_CONFIGS, json);
        edt.commit();
    }


    public static Optional<AppConfigClass> getAppConfigs(Context _ctx){
        Gson gson = new Gson();
        SharedPreferences pref = _ctx.getSharedPreferences(mAppConfigsStorage, Context.MODE_PRIVATE);
        String json = pref.getString(SharedPrefConstants.APP_CONFIGS, "");
        AppConfigClass config = gson.fromJson(json, AppConfigClass.class);
        if(config != null)
            return Optional.of(config);
        else
            return Optional.empty();
    }


    public static void setIMEI(String imei, Context _ctx){
        SharedPreferences pref = _ctx.getSharedPreferences(mMainStorage, Context.MODE_PRIVATE);
        SharedPreferences.Editor edt = pref.edit();
        edt.putString(SharedPrefConstants.IMEI_NO, imei);
        edt.commit();
    }


    public static Optional<String> getIMEI(Context _ctx){
        SharedPreferences pref = _ctx.getSharedPreferences(mMainStorage, Context.MODE_PRIVATE);
        String imei = pref.getString(SharedPrefConstants.IMEI_NO, "");
        return Optional.of(imei);
    }


    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public static void setConsumeStatus(String status, Context _ctx){
        SharedPreferences pref = _ctx.getSharedPreferences(mMainStorage, Context.MODE_PRIVATE);
        SharedPreferences.Editor edt = pref.edit();
        edt.putString(SharedPrefConstants.CONSUME_STATUS, status);
        edt.commit();
    }

    public static Optional<String> getConsumeStatus(Context _ctx){
        SharedPreferences pref = _ctx.getSharedPreferences(mMainStorage, Context.MODE_PRIVATE);
        String imei = pref.getString(SharedPrefConstants.CONSUME_STATUS, "");
        return Optional.of(imei);
    }


    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null)
        {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isAppOnForeground(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }


    public static void openApp(Context context, String appName, String packageName) {
        if (isAppInstalled(context, packageName))
            if (isAppEnabled(context, packageName))
                context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
            else Toast.makeText(context, appName + " app is not enabled.", Toast.LENGTH_SHORT).show();
        else Toast.makeText(context, appName + " app is not installed.", Toast.LENGTH_SHORT).show();
    }


    private static boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }


    private static boolean isAppEnabled(Context context, String packageName) {
        boolean appStatus = false;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packageName, 0);
            if (ai != null) {
                appStatus = ai.enabled;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appStatus;
    }
}
