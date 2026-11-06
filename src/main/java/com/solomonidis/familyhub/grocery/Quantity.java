package com.solomonidis.familyhub.grocery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * An amount with a unit, plus the grocery merging rule:
 * <ul>
 *   <li>G↔KG and ML↔L are compatible (1 kg = 1000 g, 1 l = 1000 ml); PCS only with PCS; PACK only with PACK.</li>
 *   <li>The sum is expressed in kg / l once it reaches 1000 g / ml, otherwise in g / ml.
 *       E.g. 500 g + 1 kg = 1.5 kg; 200 g + 300 g = 500 g; 1 kg + 1 kg = 2 kg.</li>
 * </ul>
 */
public record Quantity(BigDecimal amount, QuantityUnit unit) {

    /** Matches the NUMERIC(10, 3) columns. */
    static final int SCALE = 3;
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    public Quantity {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(unit, "unit");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + amount);
        }
        amount = clean(amount);
    }

    public static Quantity of(String amount, QuantityUnit unit) {
        return new Quantity(new BigDecimal(amount), unit);
    }

    public boolean isCompatibleWith(Quantity other) {
        return unit.isCompatibleWith(other.unit);
    }

    /** The combined quantity, or empty when the units can't be combined (e.g. 2 PCS + 500 G). */
    public Optional<Quantity> plus(Quantity other) {
        if (!isCompatibleWith(other)) {
            return Optional.empty();
        }
        return Optional.of(fromBase(inBaseUnit().add(other.inBaseUnit()), unit.dimension()));
    }

    BigDecimal inBaseUnit() {
        return amount.multiply(unit.factor());
    }

    private static Quantity fromBase(BigDecimal base, QuantityUnit.Dimension dimension) {
        QuantityUnit large = QuantityUnit.largeUnit(dimension);
        if (large != null && base.compareTo(THOUSAND) >= 0) {
            return new Quantity(base.divide(large.factor()), large);
        }
        return new Quantity(base, QuantityUnit.baseUnit(dimension));
    }

    /** Rounds to 3 decimals and drops trailing zeros (2.000 -> 2, 1.500 -> 1.5) without scientific notation. */
    static BigDecimal clean(BigDecimal value) {
        BigDecimal stripped = value.setScale(SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + unit;
    }
}
