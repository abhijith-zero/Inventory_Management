package com.company.inventory.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JdbcStockDao implements StockDao {
    private final DataSource ds;

    public JdbcStockDao(DataSource ds) { this.ds = ds; }

    @Override
    public void upsertStock(long itemId, long warehouseId, int qty) throws Exception {
        // Try update first
        String updateSql = "UPDATE stock SET quantity = quantity + ? WHERE item_id = ? AND warehouse_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setInt(1, qty);
            ps.setLong(2, itemId);
            ps.setLong(3, warehouseId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // insert row
                String insertSql = "INSERT INTO stock (item_id, warehouse_id, quantity) VALUES (?, ?, ?)";
                try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                    ins.setLong(1, itemId);
                    ins.setLong(2, warehouseId);
                    ins.setInt(3, Math.max(0, qty)); // initial set
                    ins.executeUpdate();
                }
            }
        }
    }

    @Override
    public int getQuantity(long itemId, long warehouseId) throws Exception {
        String sql = "SELECT quantity FROM stock WHERE item_id = ? AND warehouse_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            ps.setLong(2, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
