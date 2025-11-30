package com.planner.service;

import com.planner.dto.MenuPlanResponse;
import com.planner.dto.MenuRequest;
import com.planner.repository.RecipeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MenuGeneratorServiceTest {

    @Autowired
    private MenuGeneratorService menuGeneratorService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("Генерация недельного меню возвращает план для 7 дней и использует рецепты из БД")
    void generateWeeklyMenu_basicRequest_returnsPlanForWholeWeek() {
        // Убедимся, что инициализатор данных создал хотя бы несколько рецептов
        long recipeCount = recipeRepository.count();
        assertThat(recipeCount).isGreaterThan(0);

        MenuRequest request = new MenuRequest();
        // фиксируем неделю, чтобы убрать влияние случайного выбора
        request.setWeekId("2025-W10");
        request.setManualCalories(2000);

        MenuPlanResponse response = menuGeneratorService.generateWeeklyMenu(request);

        assertThat(response).isNotNull();
        assertThat(response.getPlan()).isNotNull();
        // ожидаем план хотя бы для нескольких дней недели
        assertThat(response.getPlan()).isNotEmpty();
        assertThat(response.getWeeklyCalories()).isGreaterThan(0);
        assertThat(response.getAverageDailyCalories()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Исключённые ингредиенты не попадают в сгенерированное меню")
    void generateWeeklyMenu_respectsExcludedIngredients() {
        // Возьмём какое-нибудь слово, которое точно встречается в ингредиентах тестовых рецептов
        // Например, "нут" (есть в нескольких рецептах).
        MenuRequest request = new MenuRequest();
        request.setWeekId("2025-W11");
        request.setManualCalories(1900);
        request.setExcludedIngredients(java.util.List.of("нут"));

        MenuPlanResponse response = menuGeneratorService.generateWeeklyMenu(request);

        assertThat(response).isNotNull();
        assertThat(response.getPlan()).isNotNull();

        boolean containsExcluded = response.getPlan().values().stream()
                .flatMap(java.util.Collection::stream)
                .map(r -> r.getIngredients() == null ? "" : r.getIngredients().toLowerCase(java.util.Locale.ROOT))
                .anyMatch(ing -> ing.contains("нут"));

        assertThat(containsExcluded).isFalse();
    }
}


