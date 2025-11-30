package com.planner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DayPreference {

    private String day;
    private String preferredDiet;
    private String preferredCuisine;
    private List<String> keywords = List.of();

    public List<String> getKeywords() {
        return keywords == null ? Collections.emptyList() : keywords;
    }

    public boolean isActive() {
        return StringUtils.hasText(preferredDiet)
                || StringUtils.hasText(preferredCuisine)
                || !getKeywords().isEmpty();
    }
}

