package com.company.inventory.model;

import java.util.Objects;


public class Stock extends BaseEntity {
    private final Long itemId;
    private int quantity;

    public Stock(Long itemId, int initialQuantity) {
        super(null, java.time.LocalDateTime.now());
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        if (initialQuantity < 0) throw new IllegalArgumentException("initialQuantity < 0");
        this.quantity = initialQuantity;
    }

    public Long getItemId() {
        return itemId;
    }

    public synchronized int getQuantity() {
        return quantity;
    }


    public synchronized void increase(int delta) {
        if (delta <= 0) throw new IllegalArgumentException("delta must be > 0");
        this.quantity = Math.addExact(this.quantity, delta);
    }


    public synchronized void decrease(int delta) {
        if (delta <= 0) throw new IllegalArgumentException("delta must be > 0");
        int result = this.quantity - delta;
        if (result < 0) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.quantity = result;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "itemId=" + itemId +
                ", quantity=" + quantity +
                '}';
    }
}
