package com.chaitany.carbonview;

import java.util.ArrayList;
import java.util.List;

public class EmissionsDataRepository {
    // Existing method to get monthly data
    public List<CompanyEmissionData> getMonthlyData() {
        List<CompanyEmissionData> data = new ArrayList<>();
        data.add(new CompanyEmissionData("Jan", 120, 80, 450));
        data.add(new CompanyEmissionData("Feb", 115, 85, 460));
        data.add(new CompanyEmissionData("Mar", 130, 75, 440));
        data.add(new CompanyEmissionData("Apr", 125, 88, 470));
        data.add(new CompanyEmissionData("May", 118, 82, 455));
        data.add(new CompanyEmissionData("Jun", 122, 79, 445));
        return data;
    }

    // Existing method to get all vehicle data
    public List<VehicleData> getVehicleData() {
        List<VehicleData> data = new ArrayList<>();
        data.add(new VehicleData("VH001", 12500, "Diesel", 3.2f));
        data.add(new VehicleData("VH002", 15000, "Electric", 1.5f));
        data.add(new VehicleData("VH003", 9800, "Gasoline", 2.8f));
        data.add(new VehicleData("VH004", 11200, "Diesel", 2.9f));
        data.add(new VehicleData("VH005", 13400, "Electric", 1.7f));
        return data;
    }

    // New method to get vehicle data based on date range
    public List<VehicleData> getVehicleData(String dateRange) {
        List<VehicleData> allVehicleData = getVehicleData(); // Get all vehicle data
        List<VehicleData> filteredData = new ArrayList<>();

        // Simulate filtering based on date range
        // In a real application, you would filter based on actual data
        switch (dateRange) {
            case "Last 7 Days":
                // Add logic to filter for the last 7 days
                filteredData.add(allVehicleData.get(0)); // Example: add first vehicle
                break;
            case "Last Month":
                // Add logic to filter for the last month
                filteredData.add(allVehicleData.get(1)); // Example: add second vehicle
                break;
            case "Last Year":
                // Add logic to filter for the last year
                filteredData.add(allVehicleData.get(2)); // Example: add third vehicle
                break;
            default:
                filteredData = allVehicleData; // No filtering
                break;
        }

        return filteredData;
    }
}