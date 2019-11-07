package com.example.mdmclient.BaseEntity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.example.mdmclient.EventHandler;
import com.example.mdmclient.Utils.AppUtility;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.example.mdmclient.App.SharedPrefConstants.QUEUE_CONSUMED;

public class RabbitMQConsumerClass{

    private Context mContext;
    private static String mExchange;
    private String mExchangeType;
    private static Channel mChannel;

    public  RabbitMQConsumerClass(Context _ctx, String _exchange, String _exchangeType,
                                 Channel _channel) {
       this.mContext = _ctx;
       this.mExchange = _exchange;
       this.mExchangeType = _exchangeType;
       this.mChannel = _channel;
    }

    private static byte[] mLastMessage;
    private static String mReceivedFrom = "";


    // An interface that can be subscribed to receive new Queue messages from Activites/Services
    public interface OnReceiveMessageHandler{
        public void onReceiveMessage(byte[] message, String receivedFrom);
    };


    //A reference to the listener, we can only have one at a time(for now)
    private static OnReceiveMessageHandler mOnReceiveMessageHandler;

    /**
     *
     * Set the callback for received messages
     * @param handler The callback
     */

    public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler){
        mOnReceiveMessageHandler = handler;
    };

    private static Handler mMessageHandler = new Handler();

    static final Runnable mReturnMessage = new Runnable() {
        public void run() {
            mOnReceiveMessageHandler.onReceiveMessage(mLastMessage, mReceivedFrom);
        }
    };



    public boolean consumeQueue(String queue) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    DefaultConsumer MySubscription = new DefaultConsumer(mChannel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            String routingKey = envelope.getRoutingKey();
                            String contentType = properties.getContentType();
                            long deliveryTag = envelope.getDeliveryTag();
                            Log.i(TAG, "ReceivedMsg");
                            mLastMessage = body;
                            mReceivedFrom = routingKey;
                            mMessageHandler.post(mReturnMessage);
                            mChannel.basicAck(deliveryTag, false);
                        }


                        @Override
                        public void handleCancel(String consumerTag) throws IOException {
                            // consumer has been cancelled unexpectedly
                            Log.i(TAG, consumerTag);
                        }
                    };
                    mChannel.basicConsume(queue, false, MySubscription);

                    if (mExchange == "fanout")
                        AddBinding("", queue); //fanout has default binding

                } catch (Exception e) {
                    EventBus.getDefault().post(new EventHandler("ConsumedStatus","not consumed"));
                    e.printStackTrace();
                    return false;
                }
                AppUtility.setConsumeStatus(QUEUE_CONSUMED, mContext);
                EventBus.getDefault().post(new EventHandler("ConsumedStatus","consumed"));
                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
            }

        }.execute();
        return false;
    }

    /**
     * Add a binding between this consumers Queue and the Exchange with routingKey
     * @param routingKey the binding key eg GOOG
     */

    public static void AddBinding(String routingKey, String queue)
    {
        try {
            mChannel.queueBind(queue, mExchange, routingKey);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Remove binding between this consumers Queue and the Exchange with routingKey
     * @param routingKey the binding key eg GOOG
     */

    public void RemoveBinding(String routingKey, String queue)
    {
        try {
            mChannel.queueUnbind(queue, mExchange, routingKey);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    public Boolean isTheQueueConsumed(String queue){
        long consumerCount =0;
        if(mChannel.isOpen()){
            try {
                consumerCount = mChannel.consumerCount(queue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(consumerCount > 0){
            return true;
        }else{
            return false;
        }
    }

}
