package com.chaitany.carbonview;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassificationEngine1 {

    public static Map<String, Double> classifyAndCalculate(List<CarbonEmission11> emissionsList) {
        Map<String, Double> scopeEmissions = new HashMap<>();
        scopeEmissions.put("Scope 1", 0.0);
        scopeEmissions.put("Scope 2", 0.0);
        scopeEmissions.put("Scope 3", 0.0);

        for (CarbonEmission11 emission : emissionsList) {
            double calculatedEmission = emission.calculateEmission();
            String scope = emission.classifyScope();

            scopeEmissions.put(scope, scopeEmissions.get(scope) + calculatedEmission);
        }

        Log.d("ClassificationEngine", "Scope-wise Emissions: " + scopeEmissions);
        return scopeEmissions;
    }
}

