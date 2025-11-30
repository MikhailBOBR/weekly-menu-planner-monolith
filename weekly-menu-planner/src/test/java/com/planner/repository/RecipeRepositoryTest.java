package com.planner.repository;

import com.planner.model.MealType;
import com.planner.model.Recipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("findByMealType находит рецепты с заданным типом приёма пищи")
    void findByMealType_returnsResultsForExistingMealType() {
        // Подготовка: сохраняем тестовый рецепт
        Recipe breakfast = Recipe.builder()
                .title("Test breakfast")
                .dietType("REGULAR")
                .mealType(MealType.BREAKFAST)
                .calories(300)
                .build();
        recipeRepository.save(breakfast);

        List<Recipe> breakfasts = recipeRepository.findByMealType(MealType.BREAKFAST);
        assertThat(breakfasts)
                .isNotNull()
                .allSatisfy(r -> assertThat(r.getMealType()).isEqualTo(MealType.BREAKFAST));
    }

    @Test
    @DisplayName("findByDietTypeIgnoreCaseAndMealType фильтрует по диете и типу приёма пищи")
    void findByDietTypeAndMealType_filtersCorrectly() {
        // Подготовка: сохраняем несколько рецептов с разными диетами
        Recipe veganLunch = Recipe.builder()
                .title("Test vegan lunch")
                .dietType("VEGAN")
                .mealType(MealType.LUNCH)
                .calories(400)
                .build();
        Recipe regularLunch = Recipe.builder()
                .title("Test regular lunch")
                .dietType("REGULAR")
                .mealType(MealType.LUNCH)
                .calories(500)
                .build();
        recipeRepository.saveAll(List.of(veganLunch, regularLunch));

        List<Recipe> veganLunches = recipeRepository.findByDietTypeIgnoreCaseAndMealType("veGan", MealType.LUNCH);

        assertThat(veganLunches)
                .isNotNull()
                .allSatisfy(r -> {
                    assertThat(r.getDietType()).isNotNull();
                    assertThat(r.getDietType()).isEqualToIgnoringCase("VEGAN");
                    assertThat(r.getMealType()).isEqualTo(MealType.LUNCH);
                });
    }
}


