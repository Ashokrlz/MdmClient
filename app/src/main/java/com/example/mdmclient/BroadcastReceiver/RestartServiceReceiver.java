package com.example.mdmclient.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.mdmclient.RabbitMQInitializationService;
import com.example.mdmclient.Utils.AppUtility;


/*******************************************************************************
 * Start MDM Client automatically on Application close event by OS.
 * Receiver to detect App destroy event.
 *******************************************************************************/


public class RestartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("RabbitMQInitializationService", "Worker created");

        if(!AppUtility.isMyServiceRunning(RabbitMQInitializationService.class, context)) {
            Intent serviceIntent = new Intent(context, RabbitMQInitializationService.class);
            Log.i("RabbitMQInitializationService", "Worker Started");
            serviceIntent.setAction("test.action.start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
