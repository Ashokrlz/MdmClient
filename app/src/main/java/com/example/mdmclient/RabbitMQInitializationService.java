package com.example.mdmclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdmclient.BaseEntity.AppConfigClass;
import com.example.mdmclient.BaseEntity.RabbitMQAuthClass;
import com.example.mdmclient.BaseEntity.RabbitMQConsumerClass;
import com.example.mdmclient.BroadcastReceiver.RestartServiceReceiver;
import com.example.mdmclient.EventPublisher.IncidentEventClass;
import com.example.mdmclient.EventPublisher.LocationEventClass;
import com.example.mdmclient.EventPublisher.PTTEventClass;
import com.example.mdmclient.Utils.AppUtility;
import com.example.mdmclient.Utils.InvokeApi;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

import static com.example.mdmclient.App.AppConstant.REGISTER_MDM_RECEIVER;
import static com.example.mdmclient.App.AppConstant.SERVER_ADDR;
import static com.example.mdmclient.App.AppConstant.SERVER_PORT;
import static com.example.mdmclient.App.SharedPrefConstants.QUEUE_NOT_CONSUMED;
import static com.example.mdmclient.BaseEntity.RabbitMQEventTypes.INCIDENT_EVENT;
import static com.example.mdmclient.BaseEntity.RabbitMQEventTypes.LOCATION_UPDATE_EVENT;
import static com.example.mdmclient.BaseEntity.RabbitMQEventTypes.PTT_EVENT;
import static com.example.mdmclient.BaseEntity.RabbitMQEventTypes.SOS_EMERGENCY_EVENT;

public class RabbitMQInitializationService extends Service {


    private static String TAG = "RabbitMQInitializationService";

    //Event-Specific Publishers
    private static PTTEventClass pttPublisher;
    private static IncidentEventClass incidentPublisher;
    private static LocationEventClass locationPublisher;

    //Receiver to listen to MOI Patrol events
    private BroadcastReceiver mdmReceiver;


    private static RabbitMQAuthClass mAuth;
    private static RabbitMQConsumerClass mConsumer;

    //Configuring wakelock to keep the device awake when run in the background
    private PowerManager.WakeLock mWakeLock;

    private static Boolean isConnected = false;
    private static Boolean isConsumed = false;

    private String patrolAppName = "moipatrol";
    private String patrolPackageName = "com.example.moipatrol";
    private String IMEIno = "", mLastRecordedLatitude="", mLastRecordedLongitude="";

    //Broadcast ID that MOI Patrol will be listening to:
    private final static String MOI_PATROL_BROADCAST_ID = "com.moi.patrol.COMMAND";


    /*******************************************************************************
     *   To run the service as a foreground service.
     *   So the OS doesn't kill the app when it runs as a background service.
     *   It is required to define channel id and notification id.
     *******************************************************************************/

    private final static String FOREGROUND_CHANNEL_ID = "foreground_channel_id";
    private NotificationManager mNotificationManager;
    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;


    //Controllers for the Service
    public static class ACTION {
        public static final String MAIN_ACTION = "test.action.main";
        public static final String START_ACTION = "test.action.start";
        public static final String STOP_ACTION = "test.action.stop";
    }


    public RabbitMQInitializationService() {
    }

    //Boilerplate
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Enters On Start");

        if (intent == null) {
            startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
            if(!isConnected) {
                getAppConfigs();
            }else{
                EventBus.getDefault().post(new EventHandler("Connection","Connected"));
            }
        }else {
            switch (intent.getAction()) {
                case ACTION.START_ACTION:
                    startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                    if (!isConnected) {
                        getAppConfigs();
                    } else {
                        EventBus.getDefault().post(new EventHandler("Connection","Connected"));
                    }
                    break;
                case ACTION.STOP_ACTION:
                    stopForeground(true);
                    stopSelf();
                    break;
                default:
                    stopForeground(true);
                    stopSelf();
            }
        }

        //Register BroadCast Receiver to listen to Patrol App Events.
        registerBroadcastReceiver();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service Started");

        // Register Event Bus for in-app commmunication
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        EventBus.getDefault().post(new EventHandler("Connection", "Service started"));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


    @Override
    public void onDestroy() {

        //Unregister Patrol Broadcast Receiver when App is destroyed by OS.
        if(mdmReceiver != null) {
            unregisterReceiver(mdmReceiver);
        }

        //Release Android Wakelock if held.
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        //Dispose RabbitMQConnection on App Destroy event
        if(mAuth != null){
            mAuth.disposeRabbitMQConnection();
        }

        //Unregister Android EventBus
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        Log.i(TAG, "Service Destroyed");

        //Restart App on App Destroy
        Intent broadcastIntent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(broadcastIntent);
    }


    /*******************************************************************************
     *  Publishes the event to the rabbit queue.
     *  Event types - PTT, IncidentComment, LocationUpdate, EMERGENCY
     *******************************************************************************/

    public void publish(String event, String msg){
        if(isConnected) {
            switch (event) {
                case PTT_EVENT:
                    pttPublisher.publishToExchange(msg);
                    break;
                case INCIDENT_EVENT:
                    incidentPublisher.publishToExchange(msg);
                    break;
                case LOCATION_UPDATE_EVENT:
                    locationPublisher.publishToExchange(msg);
                    break;
                case SOS_EMERGENCY_EVENT:
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(msg, JsonObject.class); // parse
                    if(mLastRecordedLatitude !=null && mLastRecordedLongitude != null) {
                        jsonObject.addProperty("latitude", mLastRecordedLatitude);
                        jsonObject.addProperty("longitude", mLastRecordedLongitude);
                    }
                    String emergencyJson = jsonObject.toString();
                    EventBus.getDefault().post(new EventHandler("LastUpdate",emergencyJson));
                    pttPublisher.publishToExchange(emergencyJson);
            }
        }
    }




    /*******************************************************************************
     * RabbitMQ Connection Handler
     *******************************************************************************/

    public void connectToRabbit() {

        Context _ctx = getApplicationContext();


        if (mAuth == null) {

            Optional<AppConfigClass> configs = AppUtility.getAppConfigs(_ctx);

            if(configs.isPresent()) {
                AppConfigClass _config = configs.get();

                mAuth = new RabbitMQAuthClass(_config.getServerHost(),
                        SERVER_PORT,
                        _ctx
            );

            } else {

                mAuth = new RabbitMQAuthClass(SERVER_ADDR,
                        SERVER_PORT,
                        _ctx);
            }
        }

            isConnected = mAuth.connectToRabbitMQ();

            Optional<String> _status = AppUtility.getConsumeStatus(this);

            if(_status.isPresent()){
                String consumeStatus = _status.get();

                if(consumeStatus.equalsIgnoreCase(QUEUE_NOT_CONSUMED)){
                    if (isConnected) {

                        Channel _channel = mAuth.getChannel();

                        //Rabbit Publisher PTT Event

                        pttPublisher = new PTTEventClass(_ctx, "AndriodPTTTopic", "ptttopic.pttqueue"
                                , _channel);

                        //Rabbit Publisher Incident

                        incidentPublisher = new IncidentEventClass(_ctx, "AndriodIMEIWebInTopic", "imeiwebintopic.imeiwebinqueue"
                                , _channel);


                        //Rabbit Publisher Location

                        locationPublisher = new LocationEventClass(_ctx, "AndriodLocationTopic", "locationtopic.locationqueue"
                                , _channel);


                        //Rabbit Consumer IMEI WebOutTopic
                        Optional<String> opt = AppUtility.getIMEI(_ctx);

                        if (opt.isPresent()) {
                            try {
                                IMEIno = opt.get();
                            }catch (Exception e){
                                Log.i(TAG, "Failed getting IMEINo");
                            }

                            mConsumer = new RabbitMQConsumerClass(_ctx, "AndriodIMEIWebOutTopic",
                                    "imeiwebouttopic." + IMEIno
                                    , _channel);

                            //Consume Queue
                            isConsumed = mConsumer.consumeQueue(IMEIno);


                            //Consumer receiver handler which will be invoked by the Consume Base
                            // class

                            mConsumer.setOnReceiveMessageHandler(new RabbitMQConsumerClass.OnReceiveMessageHandler() {
                                public void onReceiveMessage(byte[] message, String receivedFrom) {
                                    try {
                                        if(!AppUtility.isAppRunning(getApplicationContext(), patrolPackageName)) {
                                            if(AppUtility.isAppOnForeground(getApplicationContext(), patrolPackageName)) {
                                                AppUtility.openApp(getApplicationContext(), patrolAppName, patrolPackageName);
                                            }
                                        }
                                        String messageReceived = "";
                                        messageReceived = new String(message, "UTF8");
                                        Log.i(TAG, messageReceived);
                                        EventBus.getDefault().post(new EventHandler("LastUpdate",messageReceived));
                                        sendBroadcastToPatrol(messageReceived);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        LocationParams.Builder builder = new LocationParams.Builder()
                                .setAccuracy(LocationAccuracy.HIGH)
                                .setDistance(1)
                                .setInterval(1000 * 5);

                        SmartLocation.with(getApplicationContext()).location().config(builder.build())
                                .start(new OnLocationUpdatedListener() {
                                    @Override
                                    public void onLocationUpdated(Location location) {
                                        Log.i("LocationUpdates", "Lat-" + location.getLatitude() + "Long" +
                                                "-" + location.getLongitude());
                                        mLastRecordedLatitude = String.valueOf(location.getLatitude());
                                        mLastRecordedLongitude = String.valueOf(location.getLongitude());
                                        locationPublisher.publishLocationUpdates(IMEIno, location);
                                    }
                                });
                        }
                     }
            }
    }


    /*******************************************************************************
     * Fetch App configs that is sent during App Startup.
     * Queue info, Exchange Name etc..
     *******************************************************************************/

    public void getAppConfigs(){
            InvokeApi.fetchAppConfigs(getApplicationContext(), new InvokeApi.VolleyResponseListener() {
                @Override
                public void onError(String message) {
                    Log.e(TAG, message);
                }

                @Override
                public void onResponse(JSONObject response) {
                    Log.i(TAG, response.toString());
                    AppUtility.parseJSONRespone(getApplicationContext(), response);
                    connectToRabbit();
                }
            });
    }


    /*******************************************************************************
     * Register Broadcast receiver that listens to events sent from Patrol App.
     * Onrecieve Publishes to the concerned RabbitMQ Queue
     *******************************************************************************/


    private void registerBroadcastReceiver()
    {
        mdmReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent != null) {
                    String message = intent.getExtras().getString("COMMAND");
                    String event = intent.getExtras().getString("EVENT_TYPE");

                    Intent i = new Intent(context, RabbitMQInitializationService.class);
                    i.putExtra("PublishMessage", message);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    EventBus.getDefault().post(new EventHandler("LastUpdate",message));
                    publish(event, message);
                }
            }
        };
        IntentFilter filter = new IntentFilter(REGISTER_MDM_RECEIVER);
        registerReceiver(mdmReceiver, filter);
    }


    /*******************************************************************************
     * Prepare Notification to display it to the end-user. Which is mandatory to
     * keep the service alive when in background.
     *******************************************************************************/

    private Notification prepareNotification() {
        // handle build version above android oreo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                mNotificationManager.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            CharSequence name = "something";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance);
            channel.enableVibration(false);
            mNotificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // make a stop intent
        Intent stopIntent = new Intent(this, RabbitMQInitializationService.class);
        stopIntent.setAction(ACTION.STOP_ACTION);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // notification builder
        NotificationCompat.Builder notificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
        notificationBuilder
                .setSmallIcon(R.mipmap.mdmclient_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        return notificationBuilder.build();
    }



    /*******************************************************************************
     * Send Broadcast to MOI Patrol App
     * MOI_PATROL_BROADCAST_ID - MOI patrol app will be listening to the ID
     *******************************************************************************/

    public void sendBroadcastToPatrol(String messageReceived){
        final Intent i = new Intent();
        i.putExtra("MessageReceived", messageReceived);
        i.setAction(MOI_PATROL_BROADCAST_ID);
        i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        getApplicationContext().sendBroadcast(i);
    }
}
