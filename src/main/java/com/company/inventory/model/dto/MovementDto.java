package com.company.inventory.model.dto;

import com.company.inventory.model.MovementType;

import java.time.LocalDateTime;

public record MovementDto(Long id, Long itemId, int qty, MovementType type, String reason, LocalDateTime timestamp) {

    }


