package com.example.mdmclient;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mdmclient.RabbitMQInitializationService;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class ServiceInitializationWorkerJob extends Worker {

    public ServiceInitializationWorkerJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent serviceIntent = new Intent(getApplicationContext(), RabbitMQInitializationService.class);
        Log.i(TAG, "Worker Started");
        serviceIntent.setAction("test.action.start");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(serviceIntent);
        } else {
            getApplicationContext().startService(serviceIntent);
        }

        return Result.success();
    }
}
