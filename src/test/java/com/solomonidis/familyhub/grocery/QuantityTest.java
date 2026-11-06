package com.solomonidis.familyhub.grocery;

import static com.solomonidis.familyhub.grocery.QuantityUnit.G;
import static com.solomonidis.familyhub.grocery.QuantityUnit.KG;
import static com.solomonidis.familyhub.grocery.QuantityUnit.L;
import static com.solomonidis.familyhub.grocery.QuantityUnit.ML;
import static com.solomonidis.familyhub.grocery.QuantityUnit.PACK;
import static com.solomonidis.familyhub.grocery.QuantityUnit.PCS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The grocery merging rule (CLAUDE.md: unit conversion must have unit tests). */
class QuantityTest {

    @ParameterizedTest(name = "{0} {1} + {2} {3} = {4} {5}")
    @CsvSource({
        // examples from the requirements
        "1,    KG, 1,    KG, 2,    KG",
        "500,  G,  1,    KG, 1.5,  KG",
        // grams stay grams below 1 kg, switch to kg at 1000 g
        "200,  G,  300,  G,  500,  G",
        "600,  G,  600,  G,  1.2,  KG",
        "500,  G,  500,  G,  1,    KG",
        "1,    KG, 250,  G,  1.25, KG",
        "0.5,  KG, 0.2,  KG, 700,  G",
        // volume works the same way
        "1,    L,  1,    L,  2,    L",
        "250,  ML, 1,    L,  1.25, L",
        "330,  ML, 330,  ML, 660,  ML",
        "1.5,  L,  500,  ML, 2,    L",
        // counts and packs only add up with themselves
        "4,    PCS, 2,   PCS, 6,   PCS",
        "1,    PACK, 2,  PACK, 3,  PACK",
    })
    void compatibleUnitsAreAddedAndNormalised(String a, QuantityUnit ua, String b, QuantityUnit ub,
                                              String expected, QuantityUnit expectedUnit) {
        Quantity sum = Quantity.of(a, ua).plus(Quantity.of(b, ub)).orElseThrow();

        assertThat(sum.unit()).isEqualTo(expectedUnit);
        assertThat(sum.amount()).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} cannot be merged")
    @CsvSource({
        "PCS,  G",
        "PCS,  PACK",
        "PACK, KG",
        "G,    ML",
        "KG,   L",
        "L,    PCS",
    })
    void incompatibleUnitsAreNotMerged(QuantityUnit a, QuantityUnit b) {
        assertThat(Quantity.of("2", a).plus(Quantity.of("500", b))).isEmpty();
        assertThat(Quantity.of("500", b).plus(Quantity.of("2", a))).isEmpty();
    }

    @Test
    void additionIsCommutative() {
        Quantity a = Quantity.of("750", G);
        Quantity b = Quantity.of("0.5", KG);

        assertThat(a.plus(b)).isEqualTo(b.plus(a));
    }

    @Test
    void amountsHaveNoTrailingZerosOrScientificNotation() {
        Quantity two = Quantity.of("1.000", KG).plus(Quantity.of("1.000", KG)).orElseThrow();
        Quantity thousand = Quantity.of("600", G).plus(Quantity.of("400", G)).orElseThrow();

        assertThat(two.amount()).isEqualTo(new BigDecimal("2"));
        assertThat(thousand.toString()).isEqualTo("1 KG");
        assertThat(Quantity.of("1000", PCS).amount().toPlainString()).isEqualTo("1000");
    }

    @Test
    void roundsToThreeDecimals() {
        assertThat(Quantity.of("1.23456", KG).amount()).isEqualTo(new BigDecimal("1.235"));
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> Quantity.of("0", G)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Quantity.of("-1", G)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void piecesAndPacksAreWholeNumbersOnly() {
        assertThat(PCS.wholeNumbersOnly()).isTrue();
        assertThat(PACK.wholeNumbersOnly()).isTrue();
        assertThat(KG.wholeNumbersOnly()).isFalse();
        assertThat(L.wholeNumbersOnly()).isFalse();
    }
}
