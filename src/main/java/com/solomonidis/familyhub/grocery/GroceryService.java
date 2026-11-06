package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.grocery.GroceryDtos.AddItemResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shopping list. All additions go through {@link #add} so the merging rule always applies. */
@Service
@Transactional
public class GroceryService {

    private final GroceryItemRepository itemRepository;
    private final GroceryDefaultRepository defaultRepository;
    private final Clock clock;

    public GroceryService(GroceryItemRepository itemRepository, GroceryDefaultRepository defaultRepository, Clock clock) {
        this.itemRepository = itemRepository;
        this.defaultRepository = defaultRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GroceryItemResponse> list() {
        return itemRepository.findAllByOrderByBoughtAscNameAsc().stream().map(GroceryItemResponse::of).toList();
    }

    /**
     * Adds an item, or — if an unbought item with the same name (case-insensitive) and a compatible unit
     * is already on the list — increases that item's quantity instead (see {@link Quantity#plus}).
     * Also used by the meal planner with {@code source = RECIPE}.
     */
    public AddItemResponse add(GroceryItemRequest request, GrocerySource source) {
        String name = request.name().trim();
        Quantity incoming = toQuantity(request.quantity(), request.unit(), "quantity");
        String notes = blankToNull(request.notes());

        for (GroceryItem existing : itemRepository.findByBoughtFalseAndNameIgnoreCaseOrderByIdAsc(name)) {
            Optional<Quantity> merged = existing.getQuantity().plus(incoming);
            if (merged.isPresent()) {
                existing.setQuantity(merged.get());
                existing.appendNotes(notes);
                return new AddItemResponse(GroceryItemResponse.of(existing), true);
            }
        }
        GroceryItem item = itemRepository.save(
            new GroceryItem(name, request.category(), incoming, request.merchant(), notes, source));
        return new AddItemResponse(GroceryItemResponse.of(item), false);
    }

    public AddItemResponse addFromDefault(Long defaultId) {
        GroceryDefault d = defaultRepository.findById(defaultId)
            .orElseThrow(() -> new NotFoundException("Grocery default " + defaultId + " not found"));
        Quantity q = d.getDefaultQuantity();
        return add(new GroceryItemRequest(d.getName(), d.getCategory(), q.amount(), q.unit(),
            d.getPreferredMerchant(), null), GrocerySource.DEFAULT);
    }

    /**
     * Adds a recipe ingredient (source RECIPE), through the normal merging rule.
     * <ul>
     *   <li>Pieces and packs are rounded up to whole numbers (half an onion → 1 onion).</li>
     *   <li>Without a category, the category and shop of an active favourite with the same name are used
     *       (else OTHER / any shop).</li>
     *   <li>{@code note} (e.g. "For Pancakes") is kept on the line; merged lines collect each note.</li>
     * </ul>
     */
    public AddItemResponse addIngredient(String name, BigDecimal quantity, QuantityUnit unit,
                                         GroceryCategory category, String note) {
        BigDecimal amount = unit.wholeNumbersOnly() ? quantity.setScale(0, RoundingMode.CEILING) : quantity;
        Optional<GroceryDefault> favourite = defaultRepository.findFirstByNameIgnoreCaseAndActiveTrue(name.trim());
        GroceryCategory resolvedCategory = category != null ? category
            : favourite.map(GroceryDefault::getCategory).orElse(GroceryCategory.OTHER);
        Merchant merchant = favourite.map(GroceryDefault::getPreferredMerchant).orElse(null);
        return add(new GroceryItemRequest(name, resolvedCategory, amount, unit, merchant, note), GrocerySource.RECIPE);
    }

    /** Plain edit — no merging, so the user stays in control of what they typed. */
    public GroceryItemResponse update(Long id, GroceryItemRequest request) {
        GroceryItem item = get(id);
        item.setName(request.name().trim());
        item.setCategory(request.category());
        item.setQuantity(toQuantity(request.quantity(), request.unit(), "quantity"));
        item.setMerchant(request.merchant());
        item.setNotes(blankToNull(request.notes()));
        return GroceryItemResponse.of(item);
    }

    public GroceryItemResponse setBought(Long id, boolean bought) {
        GroceryItem item = get(id);
        item.markBought(bought, clock.instant());
        return GroceryItemResponse.of(item);
    }

    public void delete(Long id) {
        itemRepository.delete(get(id));
    }

    /** Removes every ticked item; returns how many were removed. */
    public long clearBought() {
        return itemRepository.deleteByBoughtTrue();
    }

    private GroceryItem get(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Grocery item " + id + " not found"));
    }

    /** Builds a quantity, enforcing whole numbers for pieces and packs. */
    static Quantity toQuantity(BigDecimal amount, QuantityUnit unit, String field) {
        if (unit.wholeNumbersOnly() && amount.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException(field, "Use a whole number for pieces and packs");
        }
        return new Quantity(amount, unit);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
