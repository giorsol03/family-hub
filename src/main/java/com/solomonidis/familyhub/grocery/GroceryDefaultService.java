package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryDefaultRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryDefaultResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reusable grocery templates. Deactivating hides a default without losing it. */
@Service
@Transactional
public class GroceryDefaultService {

    private final GroceryDefaultRepository repository;

    public GroceryDefaultService(GroceryDefaultRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GroceryDefaultResponse> list(boolean includeInactive) {
        List<GroceryDefault> defaults = includeInactive
            ? repository.findAllByOrderByNameAsc()
            : repository.findByActiveTrueOrderByNameAsc();
        return defaults.stream().map(GroceryDefaultResponse::of).toList();
    }

    public GroceryDefaultResponse create(GroceryDefaultRequest request) {
        GroceryDefault d = new GroceryDefault(request.name().trim(), request.category(),
            GroceryService.toQuantity(request.defaultQuantity(), request.unit(), "defaultQuantity"),
            request.preferredMerchant());
        return GroceryDefaultResponse.of(repository.save(d));
    }

    public GroceryDefaultResponse update(Long id, GroceryDefaultRequest request) {
        GroceryDefault d = get(id);
        d.setName(request.name().trim());
        d.setCategory(request.category());
        d.setDefaultQuantity(GroceryService.toQuantity(request.defaultQuantity(), request.unit(), "defaultQuantity"));
        d.setPreferredMerchant(request.preferredMerchant());
        return GroceryDefaultResponse.of(d);
    }

    public GroceryDefaultResponse setActive(Long id, boolean active) {
        GroceryDefault d = get(id);
        d.setActive(active);
        return GroceryDefaultResponse.of(d);
    }

    private GroceryDefault get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Grocery default " + id + " not found"));
    }
}
