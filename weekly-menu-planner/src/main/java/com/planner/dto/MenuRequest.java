package com.planner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MenuRequest {

    private String diet = "ALL";
    private String gender = "FEMALE";
    private Integer age = 30;
    private Double height = 165.0; // cm
    private Double weight = 60.0;  // kg
    private String activity = "MODERATE";
    private String goal = "MAINTAIN";
    private Integer mealsPerDay = 3;
    private Boolean includeSnack = Boolean.FALSE;
    private List<String> excludedIngredients = List.of();
    private List<DayPreference> dayPreferences = List.of();
    private Integer manualCalories;
    private String weekId;

    public List<String> getExcludedIngredients() {
        return excludedIngredients == null ? Collections.emptyList() : excludedIngredients;
    }

    public List<DayPreference> getDayPreferences() {
        return dayPreferences == null ? Collections.emptyList() : dayPreferences;
    }

    public Integer getManualCalories() {
        return manualCalories != null && manualCalories > 0 ? manualCalories : null;
    }
}

