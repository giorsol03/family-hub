package com.solomonidis.familyhub.grocery;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<GroceryItem> findAllByOrderByBoughtAscNameAsc();

    /** Candidates for merging: unbought lines with the same name, oldest first. */
    List<GroceryItem> findByBoughtFalseAndNameIgnoreCaseOrderByIdAsc(String name);

    long deleteByBoughtTrue();
}
