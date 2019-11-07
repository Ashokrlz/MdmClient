package com.example.mdmclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdmclient.Utils.AppUtility;
import com.example.mdmclient.Utils.DefaultExceptionHandlerToRebootApp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.example.mdmclient.App.SharedPrefConstants.QUEUE_NOT_CONSUMED;

public class MainActivity extends AppCompatActivity {

    private TextView connectionStatus, lastActualMessage, lastMessageTime, failureCause,
            queueConsumptionStatus, lastAccuracy;
    private LinearLayout causeFailureLayout;
    private Context context;
    private WorkManager mWorkManager;
    private String IMEINo;
    private static final int REQUEST_READ_PHONE_STATE = 112;
    private static final int LOCATION_PERMISSION_ID = 1001;
    private TelephonyManager telephonyManager;

    public static final String LOCAL_BROADCAST_RECEIVER = "ssbLocalBroadcast";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Removes title bar
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
        }

        setContentView(R.layout.activity_main);

        //Textview initializations
        connectionStatus = findViewById(R.id.connectionStatusResult);
        lastActualMessage = findViewById(R.id.lastActualMessage);
        lastMessageTime = findViewById(R.id.lastMessageTime);
        causeFailureLayout = findViewById(R.id.causeFailureLayout);
        failureCause = findViewById(R.id.failureCause);
        queueConsumptionStatus = findViewById(R.id.queueConsumptionStatusResult);
        lastAccuracy = findViewById(R.id.lastAccuracy);

        context = getApplicationContext();


        //Initialize ExceptionHandler, restarts the app during unexpected crashes.
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandlerToRebootApp(this));

        //Initialize status in app boot up
        AppUtility.setConsumeStatus(QUEUE_NOT_CONSUMED, this);
        connectionStatus.setText("Not Connected!!");


        //Check for app permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
        }

        //Access Telephony service in order to get IMEI of the device
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        GetIMEIOfMyDevice();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventHandler event) {
        switch (event.command) {
            case "Connection":
                causeFailureLayout.setVisibility(View.INVISIBLE);
                connectionStatus.setText(event.value);
                break;
            case "LastUpdate":
                causeFailureLayout.setVisibility(View.INVISIBLE);
                lastActualMessage.setText(event.value);
            case "ShutdownBroadcast":
                causeFailureLayout.setVisibility(View.INVISIBLE);
                lastActualMessage.setText(event.value);
            case "ConsumedStatus":
                causeFailureLayout.setVisibility(View.INVISIBLE);
                lastActualMessage.setText(event.value);
            case "UpdateTime":
                causeFailureLayout.setVisibility(View.INVISIBLE);
                lastMessageTime.setText(event.value);        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }


    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                GetIMEIOfMyDevice();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
    }


    public void GetIMEIOfMyDevice(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
            } else {
                IMEINo = telephonyManager.getImei();
                AppUtility.setIMEI(IMEINo, this);


                //Constraint to check whether there is internet connectivity. Once internet
                // becomes active Worker process will kickstart the service

                Constraints myConstraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest someWork = new OneTimeWorkRequest.Builder(ServiceInitializationWorkerJob.class)
                        .setConstraints(myConstraints)
                        .build();
                OneTimeWorkRequest oneTimeWorkRequest = someWork;
                WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
            }
        }
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
