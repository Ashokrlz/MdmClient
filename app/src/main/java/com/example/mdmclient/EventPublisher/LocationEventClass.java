package com.example.mdmclient.EventPublisher;

import android.content.Context;
import android.location.Location;

import com.example.mdmclient.BaseEntity.RabbitMQPublisherClass;
import com.rabbitmq.client.Channel;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationEventClass extends RabbitMQPublisherClass {
    public LocationEventClass(Context _ctx, String _exchange, String _routeKey, Channel _channel) {
        super(_ctx, _exchange, _routeKey, _channel);
    }


    public void publishLocationUpdates(String IMEINo, Location location){
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Imei", IMEINo);
            jsonBody.put("Latitude", location.getLatitude());
            jsonBody.put("Longitude",location.getLongitude());
            jsonBody.put("Odometer","0");
            jsonBody.put("Distance","0");
            jsonBody.put("Heading",location.getBearing());
            jsonBody.put("StatusId", "1");
            jsonBody.put("Speed", location.getSpeed());
            jsonBody.put("Accuracy", location.getAccuracy());
            jsonBody.put("Altitude", location.getAltitude());
            jsonBody.put("Provider", location.getProvider());
            jsonBody.put("SpeedAccuracy", location.getSpeedAccuracyMetersPerSecond());
            publish(jsonBody.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void publish(String JSONString){
        publishToExchange(JSONString);
    }
}
