package com.example.resqlink;


public class Hospital {
    private String name;
    private String address;
    private String phone; // Emergency hotline
    private double latitude;
    private double longitude;
    private double distance; // in km

    public Hospital(String name, String address, String phone, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }

    // Setter for distance
    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setPhone(String phoneNumber) {
        this.phone = phoneNumber;
    }


    private int scrapeAttempts = 0;

    public int getScrapeAttempts() {
        return scrapeAttempts;
    }

    public void incrementScrapeAttempts() {
        this.scrapeAttempts++;

    }

    public void setAddress(String address) {
        this.address = address;
    }
}
