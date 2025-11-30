package com.planner.service;

import com.planner.dto.DayPreference;
import com.planner.dto.MenuPlanResponse;
import com.planner.dto.MenuRequest;
import com.planner.model.MealType;
import com.planner.model.Recipe;
import com.planner.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MenuGeneratorService {

    private static final String[] DAYS = {
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота", "Воскресенье"
    };
    private static final DateTimeFormatter WEEK_FORMAT = DateTimeFormatter.ofPattern("YYYY-'W'ww");

    private final RecipeRepository recipeRepository;

    public MenuGeneratorService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public MenuPlanResponse generateWeeklyMenu(MenuRequest rawRequest) {
        MenuRequest request = normalize(rawRequest);
        int targetCalories = calculateTargetCalories(request);
        Map<String, List<Recipe>> plan = new LinkedHashMap<>();
        Set<Long> usedIds = new HashSet<>();
        int weeklyCalories = 0;
        Map<String, DayPreference> preferenceMap = buildPreferenceMap(request);
        Random baseRandom = buildRandom(request.getWeekId());

        for (String day : DAYS) {
            DayPreference preference = preferenceMap.get(day);
            Random dayRandom = new Random(baseRandom.nextLong());
            List<Recipe> dailyMeals = buildDailyMeals(request, targetCalories, usedIds, preference, dayRandom);
            plan.put(day, dailyMeals);
            weeklyCalories += dailyMeals.stream()
                    .map(Recipe::getCalories)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        int averageDailyCalories = plan.isEmpty() ? 0 : weeklyCalories / DAYS.length;

        return MenuPlanResponse.builder()
                .plan(plan)
                .targetCalories(targetCalories)
                .averageDailyCalories(averageDailyCalories)
                .weeklyCalories(weeklyCalories)
                .profile(request)
                .build();
    }

    private Random buildRandom(String weekId) {
        if (!StringUtils.hasText(weekId)) {
            return new Random();
        }
        String normalized = weekId.trim().toUpperCase(Locale.ROOT);
        try {
            String isoWeekId = normalized.matches(".*-W\\d{1,2}$")
                    ? normalized + "-1"
                    : normalized;
            LocalDate weekStart = LocalDate.parse(isoWeekId, DateTimeFormatter.ISO_WEEK_DATE);
            long seed = weekStart.toEpochDay() ^ normalized.hashCode();
            return new Random(seed);
        } catch (DateTimeParseException ex) {
            return new Random(normalized.hashCode());
        }
    }

    private Map<String, DayPreference> buildPreferenceMap(MenuRequest request) {
        return request.getDayPreferences().stream()
                .filter(Objects::nonNull)
                .filter(DayPreference::isActive)
                .filter(pref -> StringUtils.hasText(pref.getDay()))
                .collect(Collectors.toMap(
                        pref -> pref.getDay().trim(),
                        Function.identity(),
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }

    private MenuRequest normalize(MenuRequest request) {
        if (request == null) {
            return new MenuRequest();
        }
        if (request.getDiet() == null) request.setDiet("ALL");
        if (request.getGender() == null) request.setGender("FEMALE");
        if (request.getAge() == null || request.getAge() <= 0) request.setAge(30);
        if (request.getHeight() == null || request.getHeight() <= 0) request.setHeight(165.0);
        if (request.getWeight() == null || request.getWeight() <= 0) request.setWeight(60.0);
        if (request.getActivity() == null) request.setActivity("MODERATE");
        if (request.getGoal() == null) request.setGoal("MAINTAIN");
        if (request.getMealsPerDay() == null || request.getMealsPerDay() < 3) request.setMealsPerDay(3);
        if (request.getMealsPerDay() > 5) request.setMealsPerDay(5);
        if (request.getIncludeSnack() == null) request.setIncludeSnack(Boolean.FALSE);
        return request;
    }

    private int calculateTargetCalories(MenuRequest request) {
        Integer manual = request.getManualCalories();
        if (manual != null) {
            return manual;
        }

        double weight = request.getWeight();
        double height = request.getHeight();
        int age = request.getAge();
        boolean isMale = "MALE".equalsIgnoreCase(request.getGender());

        if ("REGULAR".equalsIgnoreCase(request.getDiet())
                && "MAINTAIN".equalsIgnoreCase(request.getGoal())) {
            return isMale ? 2200 : 1500;
        }

        double bmr = 10 * weight + 6.25 * height - 5 * age + (isMale ? 5 : -161);
        double activityFactor = switch (request.getActivity().toUpperCase(Locale.ROOT)) {
            case "LOW" -> 1.2;
            case "ACTIVE" -> 1.55;
            case "VERY_ACTIVE" -> 1.725;
            default -> 1.375; // MODERATE
        };
        double goalFactor = switch (request.getGoal().toUpperCase(Locale.ROOT)) {
            case "LOSE" -> 0.85;
            case "GAIN" -> 1.15;
            default -> 1.0;
        };

        return (int) Math.round(bmr * activityFactor * goalFactor);
    }

    private List<Recipe> buildDailyMeals(MenuRequest request,
                                         int targetCalories,
                                         Set<Long> usedIds,
                                         DayPreference preference,
                                         Random random) {
        List<MealType> pattern = determinePattern(request);
        int[] distribution = distributeCalories(targetCalories, pattern);
        Random randomSource = random != null ? random : new Random();

        List<Recipe> meals = new ArrayList<>();
        for (int i = 0; i < pattern.size(); i++) {
            MealType mealType = pattern.get(i);
            Recipe recipe = pickRecipeForMeal(request, mealType, distribution[i], usedIds, preference, randomSource);
            if (recipe != null) {
                meals.add(recipe);
                if (recipe.getId() != null) {
                    usedIds.add(recipe.getId());
                }
            }
        }
        return meals;
    }

    private List<MealType> determinePattern(MenuRequest request) {
        int meals = request.getMealsPerDay();
        if (Boolean.TRUE.equals(request.getIncludeSnack()) && meals < 4) {
            meals = 4;
        }
        return switch (meals) {
            case 5 -> List.of(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.SNACK, MealType.DINNER);
            case 4 -> List.of(MealType.BREAKFAST, MealType.SNACK, MealType.LUNCH, MealType.DINNER);
            default -> List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);
        };
    }

    private int[] distributeCalories(int targetCalories, List<MealType> pattern) {
        double snackRatio = pattern.stream().filter(type -> type == MealType.SNACK).count() > 1 ? 0.1 : 0.15;
        List<Double> ratios = pattern.stream()
                .map(type -> switch (type) {
                    case BREAKFAST -> 0.28;
                    case LUNCH -> 0.37;
                    case DINNER -> 0.25;
                    case SNACK -> snackRatio;
                })
                .collect(Collectors.toList());

        double total = ratios.stream().mapToDouble(Double::doubleValue).sum();
        int[] distribution = new int[ratios.size()];
        for (int i = 0; i < ratios.size(); i++) {
            distribution[i] = (int) Math.round(targetCalories * (ratios.get(i) / total));
        }
        return distribution;
    }

    private Recipe pickRecipeForMeal(MenuRequest request,
                                     MealType mealType,
                                     int mealCalories,
                                     Set<Long> usedIds,
                                     DayPreference preference,
                                     Random random) {

        List<Recipe> candidates = getCandidatesFor(request.getDiet(), mealType, preference);

        if (candidates.isEmpty()) {
            candidates = recipeRepository.findAll();
        }

        List<Recipe> filtered = candidates.stream()
                .filter(Objects::nonNull)
                .filter(recipe -> recipe.getMealType() == null || recipe.getMealType() == mealType)
                .filter(recipe -> matchesPreference(recipe, preference))
                .filter(recipe -> !containsExcludedIngredient(recipe, request.getExcludedIngredients()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            filtered = candidates;
        }

        List<Recipe> unused = filtered.stream()
                .filter(recipe -> recipe.getId() == null || !usedIds.contains(recipe.getId()))
                .collect(Collectors.toList());
        if (!unused.isEmpty()) {
            filtered = unused;
        }

        Random randomSource = random != null ? random : new Random();
        Collections.shuffle(filtered, randomSource);

        List<Recipe> ranked = filtered.stream()
                .sorted(Comparator.comparingInt(r -> scoreRecipe(r, mealCalories, usedIds)))
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            return fallbackRecipe(mealType);
        }

        int poolSize = Math.min(ranked.size(), Math.max(2, ranked.size() / 2));
        int pickIndex = randomSource.nextInt(poolSize);
        return ranked.get(pickIndex);
    }

    private int scoreRecipe(Recipe recipe, int targetCalories, Set<Long> usedIds) {
        int calories = recipe.getCalories() == null ? 0 : recipe.getCalories();
        int diff = Math.abs(calories - targetCalories);
        boolean alreadyUsed = recipe.getId() != null && usedIds.contains(recipe.getId());
        return diff + (alreadyUsed ? 500 : 0);
    }

    private Recipe fallbackRecipe(MealType mealType) {
        return Recipe.builder()
                .title("Добавьте больше рецептов")
                .description("Недостаточно блюд для подбора этого приёма пищи.")
                .mealType(mealType)
                .calories(0)
                .dietType("ALL")
                .build();
    }

    private List<Recipe> getCandidatesFor(String diet, MealType mealType, DayPreference preference) {
        String effectiveDiet = preference != null && StringUtils.hasText(preference.getPreferredDiet())
                ? preference.getPreferredDiet()
                : diet;

        if (!StringUtils.hasText(effectiveDiet) || "ALL".equalsIgnoreCase(effectiveDiet)) {
            return recipeRepository.findByMealType(mealType);
        }
        return recipeRepository.findByDietTypeIgnoreCaseAndMealType(effectiveDiet, mealType);
    }

    private boolean matchesPreference(Recipe recipe, DayPreference preference) {
        if (preference == null || !preference.isActive()) {
            return true;
        }

        if (StringUtils.hasText(preference.getPreferredCuisine())
                && (recipe.getCuisine() == null
                || !recipe.getCuisine().equalsIgnoreCase(preference.getPreferredCuisine()))) {
            return false;
        }

        if (!preference.getKeywords().isEmpty()) {
            String haystack = (recipe.getTitle() + " "
                    + Optional.ofNullable(recipe.getDescription()).orElse("") + " "
                    + Optional.ofNullable(recipe.getIngredients()).orElse(""))
                    .toLowerCase(Locale.ROOT);
            boolean keywordMatch = preference.getKeywords().stream()
                    .filter(StringUtils::hasText)
                    .map(word -> word.toLowerCase(Locale.ROOT))
                    .anyMatch(haystack::contains);
            if (!keywordMatch) {
                return false;
            }
        }

        return true;
    }

    private boolean containsExcludedIngredient(Recipe recipe, List<String> excluded) {
        if (excluded == null || excluded.isEmpty()) {
            return false;
        }
        String ingredients = recipe.getIngredients();
        if (!StringUtils.hasText(ingredients)) {
            return false;
        }
        String normalized = ingredients.toLowerCase(Locale.ROOT);
        return excluded.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }
}
