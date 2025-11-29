package com.company.inventory.dao;

import com.company.inventory.model.Stock;

import java.util.Optional;

public interface StockDao {

    Stock upsertStock(Stock stock) ;


    Optional<Stock> getStock(Long itemId);
}
