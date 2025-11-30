package com.planner.controller;

import com.planner.dto.MenuPlanResponse;
import com.planner.dto.MenuRequest;
import com.planner.model.Recipe;
import com.planner.repository.RecipeRepository;
import com.planner.service.MenuGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MenuController {

    private final MenuGeneratorService menuService;
    private final RecipeRepository recipeRepository;

    public MenuController(MenuGeneratorService menuService, RecipeRepository recipeRepository) {
        this.menuService = menuService;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/recipes")
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @GetMapping("/recipes/{id}")
    public Recipe getRecipe(@PathVariable long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рецепт не найден"));
    }

    @PostMapping("/recipes")
    public Recipe addRecipe(@RequestBody Recipe recipe) {
        recipe.setId(null);
        return recipeRepository.save(recipe);
    }

    @PutMapping("/recipes/{id}")
    public Recipe updateRecipe(@PathVariable long id, @RequestBody Recipe payload) {
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рецепт не найден"));
        payload.setId(existing.getId());
        return recipeRepository.save(payload);
    }

    @DeleteMapping("/recipes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable long id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Рецепт не найден");
        }
        recipeRepository.deleteById(id);
    }

    @PostMapping("/generate-plan")
    public MenuPlanResponse generatePersonalPlan(@RequestBody MenuRequest request) {
        return menuService.generateWeeklyMenu(request);
    }

    @GetMapping("/generate-plan")
    public MenuPlanResponse generatePlan(
            @RequestParam(defaultValue = "ALL") String diet,
            @RequestParam(defaultValue = "2000") int calories) {
        MenuRequest request = new MenuRequest();
        request.setDiet(diet);
        request.setManualCalories(calories);
        return menuService.generateWeeklyMenu(request);
    }
}
