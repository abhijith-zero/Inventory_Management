package com.company.inventory.dao;

import com.company.inventory.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemDao {
    Long create(Item item) throws Exception;
    Long update(Item item) throws Exception;
    Optional<Item> findById(Long id) throws Exception;
    Optional<Item> findBySku(String sku) throws Exception;
    List<Item> listAll() throws Exception;
}
