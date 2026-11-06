package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.member.MemberSummary;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Request/response records for the grocery API. */
public final class GroceryDtos {

    private GroceryDtos() {
    }

    public record GroceryItemRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull GroceryCategory category,
        @NotNull @DecimalMin(value = "0.001", message = "Must be more than 0")
        @Digits(integer = 7, fraction = 3, message = "Too many digits") BigDecimal quantity,
        @NotNull QuantityUnit unit,
        Merchant merchant,
        @Size(max = 500) String notes
    ) {
    }

    public record GroceryItemResponse(
        Long id,
        String name,
        GroceryCategory category,
        BigDecimal quantity,
        QuantityUnit unit,
        Merchant merchant,
        String notes,
        boolean bought,
        Instant boughtAt,
        GrocerySource source,
        MemberSummary addedBy,
        Instant createdAt
    ) {
        static GroceryItemResponse of(GroceryItem item) {
            Quantity q = item.getQuantity();
            return new GroceryItemResponse(item.getId(), item.getName(), item.getCategory(), q.amount(), q.unit(),
                item.getMerchant(), item.getNotes(), item.isBought(), item.getBoughtAt(), item.getSource(),
                MemberSummary.of(item.getCreatedBy()), item.getCreatedAt());
        }
    }

    /** Result of adding to the list: {@code merged=true} when an existing line's quantity was increased. */
    public record AddItemResponse(GroceryItemResponse item, boolean merged) {
    }

    public record BoughtRequest(@NotNull Boolean bought) {
    }

    public record ClearBoughtResponse(long removed) {
    }

    public record GroceryDefaultRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull GroceryCategory category,
        @NotNull @DecimalMin(value = "0.001", message = "Must be more than 0")
        @Digits(integer = 7, fraction = 3, message = "Too many digits") BigDecimal defaultQuantity,
        @NotNull QuantityUnit unit,
        Merchant preferredMerchant
    ) {
    }

    public record GroceryDefaultResponse(
        Long id,
        String name,
        GroceryCategory category,
        BigDecimal defaultQuantity,
        QuantityUnit unit,
        Merchant preferredMerchant,
        boolean active
    ) {
        static GroceryDefaultResponse of(GroceryDefault d) {
            Quantity q = d.getDefaultQuantity();
            return new GroceryDefaultResponse(d.getId(), d.getName(), d.getCategory(), q.amount(), q.unit(),
                d.getPreferredMerchant(), d.isActive());
        }
    }
}
