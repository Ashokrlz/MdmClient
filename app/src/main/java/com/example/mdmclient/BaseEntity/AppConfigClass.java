package com.example.mdmclient.BaseEntity;

public class AppConfigClass {
    private String serverHost = "";
    private String pttQueue = "";
    private String pttTopic = "";
    private String locationTopic = "";
    private String locationQueue = "";
    private String imeiTopic = "";
    private String imeiRouteKey = "";
    private String pttRouteKey = "";
    private String locationRouteKey = "";
    private String webInTopic ="";
    private String webInQueue = "";
    private String webInRouteKey = "";
    private String webOutTopic = "";
    private String webOutRouteKey = "";

    public AppConfigClass(){}

    public AppConfigClass(String _serverHost, String _pttQueue, String _pttTopic, String _locationTopic, String _locationQueue,
                          String _imeiTopic, String _imeiRouteKey, String _pttRouteKey, String _locationRouteKey, String _webInTopic,
                          String _webInQueue, String _webInRouteKey, String _webOutTopic, String _webOutRouteKey) {
        this.serverHost = _serverHost;
        this.pttQueue = _pttQueue;
        this.pttTopic = _pttTopic;
        this.locationTopic = _locationTopic;
        this.locationQueue = _locationQueue;
        this.imeiTopic = _imeiTopic;
        this.imeiRouteKey = _imeiRouteKey;
        this.pttRouteKey = _pttRouteKey;
        this.locationRouteKey = _locationRouteKey;
        this.webInTopic = _webInTopic;
        this.webInQueue = _webInQueue;
        this.webInRouteKey = _webInRouteKey;
        this.webOutTopic = _webOutTopic;
        this.webOutRouteKey = _webOutRouteKey;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getPttQueue() {
        return pttQueue;
    }

    public void setPttQueue(String pttQueue) {
        this.pttQueue = pttQueue;
    }

    public String getPttTopic() {
        return pttTopic;
    }

    public void setPttTopic(String pttTopic) {
        this.pttTopic = pttTopic;
    }

    public String getLocationTopic() {
        return locationTopic;
    }

    public void setLocationTopic(String locationTopic) {
        this.locationTopic = locationTopic;
    }

    public String getLocationQueue() {
        return locationQueue;
    }

    public void setLocationQueue(String locationQueue) {
        this.locationQueue = locationQueue;
    }

    public String getImeiTopic() {
        return imeiTopic;
    }

    public void setImeiTopic(String imeiTopic) {
        this.imeiTopic = imeiTopic;
    }

    public String getImeiRouteKey() {
        return imeiRouteKey;
    }

    public void setImeiRouteKey(String imeiRouteKey) {
        this.imeiRouteKey = imeiRouteKey;
    }

    public String getPttRouteKey() {
        return pttRouteKey;
    }

    public void setPttRouteKey(String pttRouteKey) {
        this.pttRouteKey = pttRouteKey;
    }

    public String getLocationRouteKey() {
        return locationRouteKey;
    }

    public void setLocationRouteKey(String locationRouteKey) {
        this.locationRouteKey = locationRouteKey;
    }

    public String getWebInTopic() {
        return webInTopic;
    }

    public void setWebInTopic(String webInTopic) {
        this.webInTopic = webInTopic;
    }

    public String getWebInQueue() {
        return webInQueue;
    }

    public void setWebInQueue(String webInQueue) {
        this.webInQueue = webInQueue;
    }

    public String getWebInRouteKey() {
        return webInRouteKey;
    }

    public void setWebInRouteKey(String webInRouteKey) {
        this.webInRouteKey = webInRouteKey;
    }

    public String getWebOutTopic() {
        return webOutTopic;
    }

    public void setWebOutTopic(String webOutTopic) {
        this.webOutTopic = webOutTopic;
    }

    public String getWebOutRouteKey() {
        return webOutRouteKey;
    }

    public void setWebOutRouteKey(String webOutRouteKey) {
        this.webOutRouteKey = webOutRouteKey;
    }
}
