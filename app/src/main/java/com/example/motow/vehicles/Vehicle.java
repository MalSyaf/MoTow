package com.example.motow.vehicles;

import java.io.Serializable;

public class Vehicle implements Serializable {

    public String vehicleId, plateNumber, brand, model, color;

    public Vehicle(String vehicleId, String plateNumber, String brand, String model, String color) {
        this.vehicleId = vehicleId;
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.color = color;
    }

    public Vehicle() {
        //
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
