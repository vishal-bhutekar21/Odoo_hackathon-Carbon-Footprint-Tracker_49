package com.chaitany.carbonview;

public class VehicleData {
    private String vehicleId;
    private int kmTraveled;
    private String fuelType;
    private float emissions;

    public VehicleData(String vehicleId, int kmTraveled, String fuelType, float emissions) {
        this.vehicleId = vehicleId;
        this.kmTraveled = kmTraveled;
        this.fuelType = fuelType;
        this.emissions = emissions;
    }

    // Getters
    public String getVehicleId() { return vehicleId; }
    public int getKmTraveled() { return kmTraveled; }
    public String getFuelType() { return fuelType; }
    public float getEmissions() { return emissions; }
}