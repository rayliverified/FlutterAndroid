package com.flutter.android;

public class FlutterConfig {
    private String flavor;
    private String accessToken;
    private String guid;
    private String deviceID;
    private String sessionID;
    private String versionName;
    private int versionCode;


    public FlutterConfig() {
    }

    public FlutterConfig(String flavor, String accessToken) {
        this.flavor = flavor;
        this.accessToken = accessToken;
    }

    public void setErrorReporting(String guid, String deviceID, String sessionID, String versionName, int versionCode) {
        this.guid = guid;
        this.deviceID = deviceID;
        this.sessionID = sessionID;
        this.versionName = versionName;
        this.versionCode = versionCode;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getGuid() { return guid; }

    public void setGuid(String guid) { this.guid = guid; }

    public String getDeviceID() { return deviceID; }

    public void setDeviceID(String deviceID) { this.deviceID = deviceID; }

    public String getSessionID() { return sessionID; }

    public void setSessionID(String sessionID) { this.sessionID = sessionID; }

    public String getVersionName() { return versionName; }

    public void setVersionName(String versionName) { this.versionName = versionName; }

    public int getVersionCode() { return versionCode; }

    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }
}