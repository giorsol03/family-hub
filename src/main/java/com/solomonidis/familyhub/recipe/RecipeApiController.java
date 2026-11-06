package com.solomonidis.familyhub.recipe;

import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeResponse;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeApiController {

    private final RecipeService service;

    public RecipeApiController(RecipeService service) {
        this.service = service;
    }

    /** Optional {@code q} (search text) and {@code tag} filters. */
    @GetMapping
    public List<RecipeSummary> list(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String tag) {
        return service.list(q, tag);
    }

    @GetMapping("/tags")
    public List<String> tags() {
        return service.tags();
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(@Valid @RequestBody RecipeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
