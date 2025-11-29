package com.company.inventory.dao;

import com.company.inventory.model.StockMovement;

import java.util.List;

public interface StockMovementDao {
    void recordMovement(StockMovement movement);

    List<StockMovement> listByItem(Long itemId);
}
