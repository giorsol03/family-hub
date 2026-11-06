package com.solomonidis.familyhub.grocery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.grocery.GroceryDtos.AddItemResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryDefaultResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroceryServiceTest {

    @Autowired
    GroceryService service;

    @Autowired
    GroceryDefaultService defaultService;

    @Autowired
    GroceryItemRepository itemRepository;

    @Test
    void addsNewItem() {
        AddItemResponse result = service.add(item("Flour", "1", QuantityUnit.KG), GrocerySource.MANUAL);

        assertThat(result.merged()).isFalse();
        assertThat(result.item().id()).isNotNull();
        assertThat(result.item().source()).isEqualTo(GrocerySource.MANUAL);
        assertThat(result.item().bought()).isFalse();
    }

    @Test
    void mergesSameNameCaseInsensitivelyWithCompatibleUnits() {
        AddItemResponse first = service.add(item("Flour", "500", QuantityUnit.G), GrocerySource.MANUAL);
        AddItemResponse second = service.add(item("  flour ", "1", QuantityUnit.KG), GrocerySource.RECIPE);

        assertThat(second.merged()).isTrue();
        assertThat(second.item().id()).isEqualTo(first.item().id());
        assertThat(second.item().quantity()).isEqualByComparingTo("1.5");
        assertThat(second.item().unit()).isEqualTo(QuantityUnit.KG);
        assertThat(second.item().name()).isEqualTo("Flour");
        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    void incompatibleUnitsCreateSeparateLines() {
        service.add(item("Tomatoes", "2", QuantityUnit.PCS), GrocerySource.MANUAL);
        AddItemResponse second = service.add(item("Tomatoes", "500", QuantityUnit.G), GrocerySource.MANUAL);

        assertThat(second.merged()).isFalse();
        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void boughtItemsAreNotMergedInto() {
        AddItemResponse first = service.add(item("Milk", "1", QuantityUnit.L), GrocerySource.MANUAL);
        service.setBought(first.item().id(), true);

        AddItemResponse second = service.add(item("Milk", "1", QuantityUnit.L), GrocerySource.MANUAL);

        assertThat(second.merged()).isFalse();
        assertThat(second.item().id()).isNotEqualTo(first.item().id());
    }

    @Test
    void mergingKeepsBothNotes() {
        service.add(new GroceryItemRequest("Coffee", GroceryCategory.PANTRY, BigDecimal.ONE, QuantityUnit.PACK,
            null, "decaf"), GrocerySource.MANUAL);
        AddItemResponse merged = service.add(new GroceryItemRequest("Coffee", GroceryCategory.PANTRY, BigDecimal.ONE,
            QuantityUnit.PACK, null, "ground"), GrocerySource.MANUAL);

        assertThat(merged.item().quantity()).isEqualByComparingTo("2");
        assertThat(merged.item().notes()).isEqualTo("decaf; ground");
    }

    @Test
    void addFromDefaultUsesTheDefaultsValues() {
        GroceryDefaultResponse cucumbers = defaultService.list(false).stream()
            .filter(d -> d.name().equals("Cucumbers")).findFirst().orElseThrow();

        AddItemResponse result = service.addFromDefault(cucumbers.id());

        assertThat(result.item().source()).isEqualTo(GrocerySource.DEFAULT);
        assertThat(result.item().quantity()).isEqualByComparingTo("4");
        assertThat(result.item().unit()).isEqualTo(QuantityUnit.PCS);
        assertThat(result.item().category()).isEqualTo(GroceryCategory.FRUIT_VEG);

        assertThat(service.addFromDefault(cucumbers.id()).item().quantity()).isEqualByComparingTo("8");
    }

    @Test
    void piecesMustBeWholeNumbers() {
        assertThatThrownBy(() -> service.add(item("Bread", "1.5", QuantityUnit.PCS), GrocerySource.MANUAL))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("whole number");
    }

    @Test
    void tickingSetsAndClearsBoughtAt() {
        Long id = service.add(item("Eggs", "1", QuantityUnit.PACK), GrocerySource.MANUAL).item().id();

        GroceryItemResponse ticked = service.setBought(id, true);
        assertThat(ticked.bought()).isTrue();
        assertThat(ticked.boughtAt()).isNotNull();

        GroceryItemResponse unticked = service.setBought(id, false);
        assertThat(unticked.bought()).isFalse();
        assertThat(unticked.boughtAt()).isNull();
    }

    @Test
    void clearBoughtRemovesOnlyTickedItems() {
        Long bread = service.add(item("Bread", "1", QuantityUnit.PCS), GrocerySource.MANUAL).item().id();
        service.add(item("Milk", "1", QuantityUnit.L), GrocerySource.MANUAL);
        service.setBought(bread, true);

        assertThat(service.clearBought()).isEqualTo(1);
        assertThat(service.list()).extracting(GroceryItemResponse::name).containsExactly("Milk");
    }

    @Test
    void updateDoesNotMerge() {
        service.add(item("Flour", "1", QuantityUnit.KG), GrocerySource.MANUAL);
        Long sugar = service.add(item("Sugar", "1", QuantityUnit.KG), GrocerySource.MANUAL).item().id();

        service.update(sugar, item("Flour", "2", QuantityUnit.KG));

        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void seedDefaultsExistAndCanBeDeactivated() {
        assertThat(defaultService.list(false)).extracting(GroceryDefaultResponse::name)
            .contains("Milk", "Bread", "Eggs", "Cucumbers", "Tomatoes", "Flour", "Olive oil", "Coffee", "Toilet paper");

        Long milk = defaultService.list(false).stream().filter(d -> d.name().equals("Milk")).findFirst().orElseThrow().id();
        defaultService.setActive(milk, false);

        assertThat(defaultService.list(false)).extracting(GroceryDefaultResponse::name).doesNotContain("Milk");
        assertThat(defaultService.list(true)).extracting(GroceryDefaultResponse::name).contains("Milk");
    }

    private static GroceryItemRequest item(String name, String quantity, QuantityUnit unit) {
        return new GroceryItemRequest(name, GroceryCategory.PANTRY, new BigDecimal(quantity), unit, null, null);
    }
}
