package com.chaitany.carbonview;

public class CarbonEmission11{
    private String activityType;
    private String companyName;
    private double consumptionAmount;
    private String date;
    private double emissionFactor;

    public CarbonEmission11(String activityType, String companyName, double consumptionAmount, String date, double emissionFactor) {
        this.activityType = activityType;
        this.companyName = companyName;
        this.consumptionAmount = consumptionAmount;
        this.date = date;
        this.emissionFactor = emissionFactor;
    }

    public double calculateEmission() {
        return consumptionAmount * emissionFactor;
    }

    public String classifyScope() {
        switch (activityType.toLowerCase()) {
            case "business travel":
            case "fuel combustion":
            case "company vehicles":
            case "natural gas usage":
            case "diesel generators":
                return "Scope 1";

            case "electricity usage":
            case "purchased heat":
            case "cooling":
            case "office lighting":
            case "data center electricity":
            case "data center energy":
                return "Scope 2";

            case "employee commuting":
            case "waste disposal":
            case "supply chain emissions":
            case "logistics":
            case "product transportation":
            case "customer product usage":
            case "end-of-life product disposal":
            case "purchased goods and services":
            case "leased assets":
            case "water usage":
            case "paper consumption":
            case "vendor activities":
                return "Scope 3";

            default:
                return "Scope 3";
        }
    }

    public String getActivityType() { return activityType; }
    public String getCompanyName() { return companyName; }
    public String getDate() { return date; }
}
