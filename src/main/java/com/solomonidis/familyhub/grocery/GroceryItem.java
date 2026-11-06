package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One line on the shopping list. "Added by" is {@link #getCreatedBy()}. */
@Entity
@Table(name = "grocery_item")
public class GroceryItem extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private GroceryCategory category;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private QuantityUnit unit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private Merchant merchant;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean bought;

    @Column(name = "bought_at")
    private Instant boughtAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private GrocerySource source;

    protected GroceryItem() {
        // for JPA
    }

    public GroceryItem(String name, GroceryCategory category, Quantity quantity, Merchant merchant,
                       String notes, GrocerySource source) {
        this.name = name;
        this.category = category;
        setQuantity(quantity);
        this.merchant = merchant;
        this.notes = notes;
        this.source = source;
    }

    public Quantity getQuantity() {
        return new Quantity(quantity, unit);
    }

    public void setQuantity(Quantity quantity) {
        this.quantity = quantity.amount();
        this.unit = quantity.unit();
    }

    /** Merging two lines: keep both notes when they differ. */
    public void appendNotes(String moreNotes) {
        if (moreNotes == null || moreNotes.isBlank()) {
            return;
        }
        if (notes == null || notes.isBlank()) {
            notes = moreNotes;
        } else if (!notes.contains(moreNotes)) {
            String combined = notes + "; " + moreNotes;
            notes = combined.length() > 500 ? combined.substring(0, 500) : combined;
        }
    }

    public void markBought(boolean bought, Instant now) {
        this.bought = bought;
        this.boughtAt = bought ? now : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GroceryCategory getCategory() {
        return category;
    }

    public void setCategory(GroceryCategory category) {
        this.category = category;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isBought() {
        return bought;
    }

    public Instant getBoughtAt() {
        return boughtAt;
    }

    public GrocerySource getSource() {
        return source;
    }
}
