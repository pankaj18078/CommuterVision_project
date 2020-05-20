package com.example.comvision;

import android.location.Location;

public class User {
    private String id;
    private double latitude;
    private double longitude;

    public User(){

    }

    public User(String id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
