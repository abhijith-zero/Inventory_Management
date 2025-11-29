package com.company.inventory.model;

import java.util.Objects;


public record Sku(String value) {
    public Sku(String value) {
        Objects.requireNonNull(value, "sku");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("SKU cannot be empty");
        // example validation: alphanumeric + hyphen
        if (!trimmed.matches("[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException("Invalid SKU format");
        }
        this.value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sku sku)) return false;
        return value.equals(sku.value);
    }

}
