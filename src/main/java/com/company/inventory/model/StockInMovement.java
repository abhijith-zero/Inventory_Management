package com.company.inventory.model;

import java.time.LocalDateTime;

public final class StockInMovement extends StockMovement {
    public StockInMovement(Long id, Long itemId, int qty, String reason, LocalDateTime timestamp){
        super(id, itemId, qty, MovementType.IN, reason, timestamp);
    }

    @Override
    public void applyTo(Stock stock) {
        stock.increase(getQty());
    }
}
