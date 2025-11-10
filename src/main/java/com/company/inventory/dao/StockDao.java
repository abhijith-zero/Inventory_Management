package com.company.inventory.dao;

public interface StockDao {

    void upsertStock(long itemId, long warehouseId, int qty) throws Exception;


    int getQuantity(long itemId, long warehouseId) throws Exception;
}
