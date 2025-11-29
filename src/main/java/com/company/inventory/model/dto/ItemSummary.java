package com.company.inventory.model.dto;

import com.company.inventory.model.Sku;

/**
 * Read-only DTO for presenting item + quantity.
 * Demonstrates use of records for simple DTOs.
 */
public record ItemSummary(Long id, String name, Sku sku, int quantity, Double salePrice) {}
