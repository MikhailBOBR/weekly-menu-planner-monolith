package com.planner.model;

public enum MealType {
    BREAKFAST("Завтрак"),
    SNACK("Перекус"),
    LUNCH("Обед"),
    DINNER("Ужин");

    private final String label;

    MealType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

