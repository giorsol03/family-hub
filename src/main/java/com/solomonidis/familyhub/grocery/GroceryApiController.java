package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.grocery.GroceryDtos.AddItemResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.BoughtRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.ClearBoughtResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryDefaultRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryDefaultResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
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
@RequestMapping("/api/groceries")
public class GroceryApiController {

    private final GroceryService groceryService;
    private final GroceryDefaultService defaultService;

    public GroceryApiController(GroceryService groceryService, GroceryDefaultService defaultService) {
        this.groceryService = groceryService;
        this.defaultService = defaultService;
    }

    // ---- shopping list ----

    @GetMapping("/items")
    public List<GroceryItemResponse> items() {
        return groceryService.list();
    }

    /** 200 rather than 201: the result may be an existing, merged line. */
    @PostMapping("/items")
    public AddItemResponse add(@Valid @RequestBody GroceryItemRequest request) {
        return groceryService.add(request, GrocerySource.MANUAL);
    }

    @PostMapping("/items/from-default/{defaultId}")
    public AddItemResponse addFromDefault(@PathVariable Long defaultId) {
        return groceryService.addFromDefault(defaultId);
    }

    @PutMapping("/items/{id}")
    public GroceryItemResponse update(@PathVariable Long id, @Valid @RequestBody GroceryItemRequest request) {
        return groceryService.update(id, request);
    }

    @PutMapping("/items/{id}/bought")
    public GroceryItemResponse setBought(@PathVariable Long id, @Valid @RequestBody BoughtRequest request) {
        return groceryService.setBought(id, request.bought());
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groceryService.delete(id);
    }

    @PostMapping("/items/clear-bought")
    public ClearBoughtResponse clearBought() {
        return new ClearBoughtResponse(groceryService.clearBought());
    }

    // ---- defaults ----

    @GetMapping("/defaults")
    public List<GroceryDefaultResponse> defaults(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return defaultService.list(includeInactive);
    }

    @PostMapping("/defaults")
    @ResponseStatus(HttpStatus.CREATED)
    public GroceryDefaultResponse createDefault(@Valid @RequestBody GroceryDefaultRequest request) {
        return defaultService.create(request);
    }

    @PutMapping("/defaults/{id}")
    public GroceryDefaultResponse updateDefault(@PathVariable Long id, @Valid @RequestBody GroceryDefaultRequest request) {
        return defaultService.update(id, request);
    }

    @PostMapping("/defaults/{id}/deactivate")
    public GroceryDefaultResponse deactivateDefault(@PathVariable Long id) {
        return defaultService.setActive(id, false);
    }

    @PostMapping("/defaults/{id}/activate")
    public GroceryDefaultResponse activateDefault(@PathVariable Long id) {
        return defaultService.setActive(id, true);
    }
}
