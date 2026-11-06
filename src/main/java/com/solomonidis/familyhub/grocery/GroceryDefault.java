package com.solomonidis.familyhub.grocery;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A reusable grocery template ("favourite") that can be added to the list in one tap. */
@Entity
@Table(name = "grocery_default")
public class GroceryDefault extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private GroceryCategory category;

    @Column(name = "default_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal defaultQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private QuantityUnit unit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "preferred_merchant", length = 20)
    private Merchant preferredMerchant;

    @Column(nullable = false)
    private boolean active = true;

    protected GroceryDefault() {
        // for JPA
    }

    public GroceryDefault(String name, GroceryCategory category, Quantity defaultQuantity, Merchant preferredMerchant) {
        this.name = name;
        this.category = category;
        setDefaultQuantity(defaultQuantity);
        this.preferredMerchant = preferredMerchant;
    }

    public Quantity getDefaultQuantity() {
        return new Quantity(defaultQuantity, unit);
    }

    public void setDefaultQuantity(Quantity quantity) {
        this.defaultQuantity = quantity.amount();
        this.unit = quantity.unit();
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

    public Merchant getPreferredMerchant() {
        return preferredMerchant;
    }

    public void setPreferredMerchant(Merchant preferredMerchant) {
        this.preferredMerchant = preferredMerchant;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
