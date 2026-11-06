package com.solomonidis.familyhub.grocery;

import java.math.BigDecimal;

/**
 * Units for groceries (and later recipe ingredients). Units with the same {@link Dimension} can be
 * converted into each other via {@link #factor()} (amount in the dimension's base unit).
 */
public enum QuantityUnit {
    G(Dimension.MASS, 1),
    KG(Dimension.MASS, 1000),
    ML(Dimension.VOLUME, 1),
    L(Dimension.VOLUME, 1000),
    /** Plain count, e.g. 4 cucumbers. */
    PCS(Dimension.COUNT, 1),
    PACK(Dimension.PACKS, 1);

    public enum Dimension { MASS, VOLUME, COUNT, PACKS }

    private final Dimension dimension;
    private final BigDecimal factor;

    QuantityUnit(Dimension dimension, int factor) {
        this.dimension = dimension;
        this.factor = BigDecimal.valueOf(factor);
    }

    public Dimension dimension() {
        return dimension;
    }

    /** How many base units (g, ml, pcs, pack) one of this unit is. */
    public BigDecimal factor() {
        return factor;
    }

    public boolean isCompatibleWith(QuantityUnit other) {
        return dimension == other.dimension;
    }

    /** Pieces and packs are counted, so only whole numbers make sense. */
    public boolean wholeNumbersOnly() {
        return dimension == Dimension.COUNT || dimension == Dimension.PACKS;
    }

    static QuantityUnit baseUnit(Dimension dimension) {
        return switch (dimension) {
            case MASS -> G;
            case VOLUME -> ML;
            case COUNT -> PCS;
            case PACKS -> PACK;
        };
    }

    /** The bigger unit used once a total reaches 1000 base units (kg, l), or null if there is none. */
    static QuantityUnit largeUnit(Dimension dimension) {
        return switch (dimension) {
            case MASS -> KG;
            case VOLUME -> L;
            case COUNT, PACKS -> null;
        };
    }
}
