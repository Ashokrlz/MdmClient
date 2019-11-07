package com.example.mdmclient.BaseEntity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdmclient.EventHandler;
import com.example.mdmclient.R;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static com.example.mdmclient.App.AppConstant.CLIENT_CERTPASSWORD;
import static com.example.mdmclient.App.AppConstant.SERVER_CERTPASSWORD;
import static com.example.mdmclient.App.AppConstant.TLS_VERSION;

public class RabbitMQAuthClass {
    private static String mServer;
    private static int mPort;
    private static Context mContext;
    private static Channel mChannel = null;
    private static Connection mConnection;
    private static boolean Running;

    /**
     * @param server       The server address
     * @param port         The server port
     * @param ctx          Application Context
     */

    public RabbitMQAuthClass(String server, int port, Context ctx) {
        this.mServer = server;
        this.mPort = port;
        this.mContext = ctx;
    }


    public void disposeRabbitMQConnection() {
        Running = false;
        try {
            if (mConnection != null)
                mConnection.close();
            if (mChannel != null) {
                mChannel.abort();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
             e.printStackTrace();
        }
    }

    /**
     * Connect to the broker and create the exchange
     *
     * @return success
     */
    public static boolean connectToRabbitMQ() {
        if (mChannel != null && mChannel.isOpen())//already declared
        {
            Running = true;
            return true;
        }

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    char[] keyPassphrase = CLIENT_CERTPASSWORD.toCharArray();
                    KeyStore ks = null;
                    ks = KeyStore.getInstance("PKCS12");
                    ks.load(mContext.getResources().openRawResource(R.raw.client_certificate),
                            keyPassphrase);

                    KeyManagerFactory kmf = null;
                    kmf = KeyManagerFactory.getInstance("X509");
                    kmf.init(ks, keyPassphrase);

                    char[] trustPassphrase = SERVER_CERTPASSWORD.toCharArray();
                    KeyStore tks = null;
                    tks = KeyStore.getInstance("BKS");
                    tks.load(mContext.getResources().openRawResource(R.raw.bksstore),
                            trustPassphrase);

                    TrustManagerFactory tmf = null;
                    tmf = TrustManagerFactory.getInstance("X509");
                    tmf.init(tks);

                    SSLContext c = null;
                    c = SSLContext.getInstance(TLS_VERSION);
                    c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                    ConnectionFactory connectionFactory = new ConnectionFactory();
                    connectionFactory.setHost(mServer);
                    connectionFactory.setPort(mPort);
                    connectionFactory.useSslProtocol(c);
                    connectionFactory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
                    connectionFactory.enableHostnameVerification();
                    connectionFactory.setAutomaticRecoveryEnabled(true);
                    connectionFactory.setNetworkRecoveryInterval(5000);
                    connectionFactory.setConnectionTimeout(20000);

                    mConnection = connectionFactory.newConnection();
                    if (mConnection instanceof Recoverable) {
                        // This cast is possible for connections created by a factory that supports auto-recovery
                        ((Recoverable) mConnection).addRecoveryListener(new RecoveryListener() {
                            @Override
                            public void handleRecovery(Recoverable recoverable) {
                                Log.i("RabbitMQInitializationService", "ConnectionEstablished");
                                EventBus.getDefault().post(new EventHandler("Connection","ConnectionRecovered"));
                            }

                            @Override
                            public void handleRecoveryStarted(Recoverable recoverable) {
                                Log.i("RabbitMQInitializationService", "ConnectionAttemptStarted");
                                EventBus.getDefault().post(new EventHandler("Connection","Trying to Reconnect to RabbitMQ..."));
                            }
                        });
                    }

                    mConnection.addShutdownListener(new ShutdownListener() {
                        public void shutdownCompleted(ShutdownSignalException cause) {
                            String hardError = "";
                            String applInit = "";
                            if (cause.isHardError()) {
                                hardError = "connection";
                            } else {
                                hardError = "channel";
                            }

                            if (cause.isInitiatedByApplication()) {
                                applInit = "application";
                            } else {
                                applInit = "broker";
                            }
                            EventBus.getDefault().post(new EventHandler("ShutdownBroadcast",
                                    "Disconnected" + "Caused by " + applInit + " at the " + hardError + " level."));
                        }
                    });

                    mChannel = mConnection.createChannel();
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                Running = aBoolean;
                if(Running){
                    EventBus.getDefault().post(new EventHandler("Connection","Connected"));
                }
            }

        }.execute();
        return false;
    }


    public Channel getChannel() {
        return mChannel;
    }
}
