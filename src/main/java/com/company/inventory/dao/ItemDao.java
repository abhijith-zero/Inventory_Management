package com.company.inventory.dao;

import com.company.inventory.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemDao {
    Item create(Item item) ;
    Item update(Item item) ;
    Optional<Item> findById(Long id) ;
    Optional<Item> findBySku(String sku) ;
    List<Item> search(String query);

}
