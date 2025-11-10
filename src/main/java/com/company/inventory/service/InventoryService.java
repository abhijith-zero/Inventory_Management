package com.company.inventory.service;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.dao.ItemDao;
import com.company.inventory.dao.StockDao;
import com.company.inventory.model.Item;

import javax.sql.DataSource;
import java.sql.Connection;

public class InventoryService {
    private final ItemDao itemDao;
    private final StockDao stockDao;
    private final long defaultWarehouseId;

    public InventoryService(ItemDao itemDao, StockDao stockDao, long defaultWarehouseId) {
        this.itemDao = itemDao;
        this.stockDao = stockDao;
        this.defaultWarehouseId = defaultWarehouseId;
    }

    /**
     * Create item and, if initialStock > 0, create stock entry for default warehouse.
     * Ensures atomicity using DB transaction.
     *
     * Returns created item id.
     */
    public Long createItem(Item item, int initialStock) {
        try (Connection conn = DbConnectionManager.getConnection()) {
            try {
                conn.setAutoCommit(false);

                // use DAOs that open new connections — to ensure single-connection transaction,
                // we call DAOs using the same connection by temporarily using wrappers.
                // Simpler: implement create logic here using connection directly for atomicity.
                // But for reusing DAOs above, we'll assume DAOs can accept connection.
                // To keep it simple and safe, call itemDao.create (which gets its own connection)
                // only if using DB with proper transaction propagation. Instead, do everything here.

                // Insert item
                String insertItemSql = "INSERT INTO item (name, sku, purchase_price, sale_price, reorder_level) VALUES (?, ?, ?, ?, ?)";
                try (var ps = conn.prepareStatement(insertItemSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.getName());
                    ps.setString(2, item.getSku());
                    if (item.getPurchasePrice() != null) ps.setDouble(3, item.getPurchasePrice()); else ps.setNull(3, java.sql.Types.DECIMAL);
                    if (item.getSalePrice() != null) ps.setDouble(4, item.getSalePrice()); else ps.setNull(4, java.sql.Types.DECIMAL);
                    if (item.getReorderLevel() != null) ps.setInt(5, item.getReorderLevel()); else ps.setNull(5, java.sql.Types.INTEGER);
                    ps.executeUpdate();
                    try (var rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            long itemId = rs.getLong(1);
                            if (initialStock > 0) {
                                String upd = "UPDATE stock SET quantity = quantity + ? WHERE item_id = ? AND warehouse_id = ?";
                                try (var ps2 = conn.prepareStatement(upd)) {
                                    ps2.setInt(1, initialStock);
                                    ps2.setLong(2, itemId);
                                    ps2.setLong(3, defaultWarehouseId);
                                    int updated = ps2.executeUpdate();
                                    if (updated == 0) {
                                        String ins = "INSERT INTO stock (item_id, warehouse_id, quantity) VALUES (?, ?, ?)";
                                        try (var ps3 = conn.prepareStatement(ins)) {
                                            ps3.setLong(1, itemId);
                                            ps3.setLong(2, defaultWarehouseId);
                                            ps3.setInt(3, initialStock);
                                            ps3.executeUpdate();
                                        }
                                    }
                                }
                            }
                            conn.commit();
                            return itemId;
                        } else {
                            throw new RuntimeException("Failed to obtain generated key for item");
                        }
                    }
                }
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw new RuntimeException("Failed to create item: " + ex.getMessage(), ex);
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<Item> listItems() {
        try {
            return itemDao.listAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
