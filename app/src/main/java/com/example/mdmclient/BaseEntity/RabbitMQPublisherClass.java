package com.example.mdmclient.BaseEntity;

import android.content.Context;

import com.rabbitmq.client.Channel;

import java.io.IOException;

public class RabbitMQPublisherClass{

    private Context mContext;
    private String mExchange;
    private String mRouteKey;
    private Channel mChannel;

    public RabbitMQPublisherClass(Context _ctx, String _exchange, String _routeKey,
                                 Channel _channel) {
        this.mContext = _ctx;
        this.mExchange = _exchange;
        this.mRouteKey = _routeKey;
        this.mChannel = _channel;
    }


    public void publishToExchange(String message){
        try {
            if(mChannel.isOpen()) {
                mChannel.basicPublish(mExchange, mRouteKey, null, message.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
