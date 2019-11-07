package com.example.mdmclient.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.mdmclient.MainActivity;

public class DefaultExceptionHandlerToRebootApp implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;
    Activity activity;

    public DefaultExceptionHandlerToRebootApp(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        Intent intent = new Intent(activity, MainActivity.class);

        intent.putExtra("crash", true);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                activity.getBaseContext(), 0, intent, intent.getFlags());

        //Following code will restart application after 0.5 seconds
        AlarmManager mgr = (AlarmManager) activity.getBaseContext()
                .getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500,
                pendingIntent);

        //This will finish activity manually
        activity.finish();

        //This will stop application and take out from it.
        System.exit(2);

    }
}
