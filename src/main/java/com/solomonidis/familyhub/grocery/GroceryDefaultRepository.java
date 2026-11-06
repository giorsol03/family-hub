package com.solomonidis.familyhub.grocery;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryDefaultRepository extends JpaRepository<GroceryDefault, Long> {

    List<GroceryDefault> findAllByOrderByNameAsc();

    List<GroceryDefault> findByActiveTrueOrderByNameAsc();

    Optional<GroceryDefault> findFirstByNameIgnoreCaseAndActiveTrue(String name);
}
