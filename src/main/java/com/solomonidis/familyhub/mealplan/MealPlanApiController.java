package com.solomonidis.familyhub.mealplan;

import com.solomonidis.familyhub.mealplan.MealPlanDtos.AddToGroceriesRequest;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.AddToGroceriesResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlanRequest;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlannedResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.WeekResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plan")
public class MealPlanApiController {

    private final MealPlanService service;

    public MealPlanApiController(MealPlanService service) {
        this.service = service;
    }

    /** The Mon–Sun week containing {@code weekOf} (default: this week). */
    @GetMapping
    public WeekResponse week(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf) {
        return service.week(weekOf);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlannedResponse plan(@Valid @RequestBody PlanRequest request) {
        return service.plan(request);
    }

    @GetMapping("/{id}")
    public PlannedResponse entry(@PathVariable Long id) {
        return service.plannedWithIngredients(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        service.remove(id);
    }

    @PostMapping("/{id}/add-to-groceries")
    public AddToGroceriesResponse addToGroceries(@PathVariable Long id, @Valid @RequestBody AddToGroceriesRequest request) {
        return service.addToGroceries(id, request.ingredientIndexes());
    }
}
