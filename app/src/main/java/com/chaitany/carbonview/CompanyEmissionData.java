package com.chaitany.carbonview;

public class CompanyEmissionData {
    private String period;
    private float scope1Value;
    private float scope2Value;
    private float scope3Value;

    public CompanyEmissionData(String period, float scope1Value, float scope2Value, float scope3Value) {
        this.period = period;
        this.scope1Value = scope1Value;
        this.scope2Value = scope2Value;
        this.scope3Value = scope3Value;
    }

    // Getters
    public String getPeriod() { return period; }
    public float getScope1Value() { return scope1Value; }
    public float getScope2Value() { return scope2Value; }
    public float getScope3Value() { return scope3Value; }
}