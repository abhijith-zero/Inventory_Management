package com.company.inventory.model;

import java.time.LocalDateTime;

public final class StockOutMovement extends StockMovement {
    public StockOutMovement(Long id, Long itemId, int qty, String reason, LocalDateTime timestamp) {
        super(id, itemId, qty, MovementType.OUT, reason, timestamp);
    }

        @Override
        public void applyTo (Stock stock){
            stock.decrease(getQty());
        }
    }
