package com.planner.dto;

import com.planner.model.Recipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class MenuPlanResponse {

    private Map<String, List<Recipe>> plan;
    private int targetCalories;
    private int averageDailyCalories;
    private int weeklyCalories;
    private MenuRequest profile;
}

