package com.planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.dto.MenuRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/recipes возвращает список (может быть пустым, но без ошибки)")
    void getAllRecipes_returnsOk() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/generate-plan с базовым запросом возвращает валидный план")
    void generatePlan_withBasicRequest_returnsPlan() throws Exception {
        MenuRequest request = new MenuRequest();
        request.setWeekId("2025-W10");
        request.setManualCalories(1800);

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        post("/api/generate-plan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").exists())
                .andExpect(jsonPath("$.targetCalories").value(1800));
    }

    @Test
    @DisplayName("GET /api/generate-plan с query-параметрами возвращает план по умолчанию")
    void generatePlan_withQueryParams_returnsPlan() throws Exception {
        mockMvc.perform(
                        get("/api/generate-plan")
                                .param("diet", "ALL")
                                .param("calories", "2100")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").exists())
                .andExpect(jsonPath("$.targetCalories").value(2100));
    }
}


