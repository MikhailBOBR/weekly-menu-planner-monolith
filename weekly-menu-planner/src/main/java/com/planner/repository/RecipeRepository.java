package com.planner.repository;

import com.planner.model.MealType;
import com.planner.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByDietTypeIgnoreCase(String dietType);

    List<Recipe> findByDietTypeIgnoreCaseAndMealType(String dietType, MealType mealType);

    List<Recipe> findByMealType(MealType mealType);
}
