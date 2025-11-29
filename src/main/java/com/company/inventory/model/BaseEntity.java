package com.company.inventory.model;

import java.time.LocalDateTime;
import java.util.Objects;


public abstract class BaseEntity {
    protected  Long id;
    protected final LocalDateTime createdAt;

    protected BaseEntity() {
        this(null, LocalDateTime.now());
    }

    protected BaseEntity(Long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Long getId() {
        return id;
    }
    protected void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt; // safe: LocalDateTime is immutable
    }
}
