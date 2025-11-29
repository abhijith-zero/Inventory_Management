package com.company.inventory.service;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.dao.*;
import com.company.inventory.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;


public class InventoryService {

    private final ItemDao itemDao;
    private final StockDao stockDao; // used for non-transactional reads
    private final StockMovementDao stockMovementDao; // used for reads/listing movements

    // SQL used for transactional stock & movement writes (kept here so logic is centralized)
    private static final String SELECT_STOCK_SQL = "SELECT id, quantity FROM stock WHERE item_id = ?";
    private static final String UPDATE_STOCK_SQL = "UPDATE stock SET quantity = ? WHERE item_id = ?";
    private static final String INSERT_STOCK_SQL = "INSERT INTO stock (item_id, quantity) VALUES (?, ?)";
    private static final String INSERT_MOVEMENT_SQL = "INSERT INTO stock_movement (item_id, qty, type, reason, timestamp) VALUES (?, ?, ?, ?, ?)";

    public InventoryService(ItemDao itemDao, StockDao stockDao, StockMovementDao stockMovementDao) {
        this.itemDao = itemDao;
        this.stockDao = stockDao;
        this.stockMovementDao = stockMovementDao;
    }

    // ----------------------------
    // Item operations (delegates to DAO)
    // ----------------------------
    public Item createItem(Item item) {
        // basic validation
        if (item == null) throw new IllegalArgumentException("item is null");
        return itemDao.create(item);
    }

    public Item updateItem(Item item) {
        if (item == null || item.getId() == null) throw new IllegalArgumentException("item or id is null");
        return itemDao.update(item);
    }

    public Optional<Item> findItemById(Long id) {
        if (id == null) return Optional.empty();
        return itemDao.findById(id);
    }

    public Optional<Item> findItemBySku(String sku) {
        if (sku == null || sku.isBlank()) return Optional.empty();
        return itemDao.findBySku(sku);
    }

    public List<Item> searchItems(String query) {
        if (query == null) query = "";
        return itemDao.search(query);
    }

    // ----------------------------
    // Stock & Movement operations (transactional)
    // ----------------------------

    /**
     * View stock (non-transactional, delegates to StockDao which manages its own connection).
     */
    public Optional<Stock> viewStock(Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("itemId is null");
        return stockDao.getStock(itemId);
    }

    /**
     * Lists movements for an item.
     */
    public List<StockMovement> listMovements(Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("itemId is null");
        return stockMovementDao.listByItem(itemId);
    }


    public Stock increaseStock(Long itemId, int qty, String reason) {
        if (itemId == null) throw new IllegalArgumentException("itemId is null");
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        try (Connection conn = DbConnectionManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                int resultingQty = upsertAndAdjustStockTx(conn, itemId, qty); // add qty
                insertMovementTx(conn, itemId, qty, MovementType.IN, reason);

                conn.commit();

                // Return fresh Stock — using StockDao (separate connection) for simplicity
                return stockDao.getStock(itemId).orElseGet(() -> new Stock(itemId, resultingQty));
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw new DataAccessException("Failed to increase stock", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }


    public Stock decreaseStock(Long itemId, int qty, String reason) {
        if (itemId == null) throw new IllegalArgumentException("itemId is null");
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        try (Connection conn = DbConnectionManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                // read current quantity (for validation)
                int currentQty = readStockQuantityForUpdate(conn, itemId);
                int newQty = currentQty - qty;
                if (newQty < 0) {
                    throw new IllegalStateException("Insufficient stock: current=" + currentQty + ", requested=" + qty);
                }

                // update or insert accordingly
                writeStockQuantityTx(conn, itemId, newQty);
                insertMovementTx(conn, itemId, qty, MovementType.OUT, reason);

                conn.commit();

                return stockDao.getStock(itemId).orElseGet(() -> new Stock(itemId, newQty));
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw new DataAccessException("Failed to decrease stock", e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * Convenience: adjustStock with positive (IN) or negative (OUT) qty.
     */
    public Stock adjustStock(Long itemId, int delta, String reason) {
        if (delta == 0) {
            // nothing to do — return current
            return stockDao.getStock(itemId).orElseGet(() -> new Stock(itemId, 0));
        }
        if (delta > 0) return increaseStock(itemId, delta, reason);
        else return decreaseStock(itemId, -delta, reason);
    }

    // ----------------------------
    // Internal JDBC helpers for transactional operations
    // ----------------------------

    /**
     * Read current quantity for item for update semantics. If missing returns 0.
     * This method reads the current quantity using a SELECT and returns it.
     */
    private int readStockQuantityForUpdate(Connection conn, Long itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_STOCK_SQL)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Upsert + adjust stock in transaction: returns resulting quantity.
     * This version reads current quantity and writes updated quantity.
     */
    private int upsertAndAdjustStockTx(Connection conn, Long itemId, int delta) throws SQLException {
        int current = readStockQuantityForUpdate(conn, itemId);
        int newQty = Math.addExact(current, delta);
        writeStockQuantityTx(conn, itemId, newQty);
        return newQty;
    }

    /**
     * Write resulting stock quantity (insert or update) within supplied transaction.
     */
    private void writeStockQuantityTx(Connection conn, Long itemId, int quantity) throws SQLException {
        // Try update
        try (PreparedStatement update = conn.prepareStatement(UPDATE_STOCK_SQL)) {
            update.setInt(1, quantity);
            update.setLong(2, itemId);
            int updated = update.executeUpdate();
            if (updated == 0) {
                try (PreparedStatement insert = conn.prepareStatement(INSERT_STOCK_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, itemId);
                    insert.setInt(2, quantity);
                    insert.executeUpdate();
                    // ignore generated id here; Stock model doesn't include DB id by default
                }
            }
        }
    }

    /**
     * Insert stock_movement row in transaction.
     */
    private void insertMovementTx(Connection conn, Long itemId, int qty, MovementType type, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_MOVEMENT_SQL)) {
            ps.setLong(1, itemId);
            ps.setInt(2, qty);
            ps.setString(3, type.name());
            if (reason == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, reason);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

}
