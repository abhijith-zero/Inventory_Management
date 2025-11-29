package com.company.inventory.model;

import java.time.LocalDateTime;
import java.util.Objects;


public abstract sealed class StockMovement permits StockInMovement, StockOutMovement {
    private final Long id;
    private final Long itemId;
    private final int qty;
    private final String reason;
    private final LocalDateTime timestamp;
    private final MovementType type;

    protected StockMovement(Long id, Long itemId, int qty, MovementType type, String reason, LocalDateTime timestamp) {
        this.id = id;
        this.itemId = Objects.requireNonNull(itemId);
        this.qty = qty;
        this.type = Objects.requireNonNull(type);
        this.reason = reason == null ? "" : reason;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public int getQty() { return qty; }
    public MovementType getType() { return type; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; } // LocalDateTime is immutable

    /**
     * Apply this movement to the provided stock (IN adds, OUT subtracts).
     * This demonstrates polymorphism: child types can call super or provide behavior.
     */
    public abstract void applyTo(Stock stock);

    @Override
    public String toString() {
        return "StockMovement{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", qty=" + qty +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
