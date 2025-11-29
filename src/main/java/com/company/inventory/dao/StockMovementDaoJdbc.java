package com.company.inventory.dao;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified recorder of movements. Stores:
 * id, item_id, qty, type, reason, timestamp
 */
public class StockMovementDaoJdbc implements StockMovementDao {

    private static final String INSERT_SQL =
            "INSERT INTO stock_movement (item_id, qty, type, reason, timestamp) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ITEM =
            "SELECT id, item_id, qty, type, reason, timestamp FROM stock_movement WHERE item_id = ? ORDER BY timestamp DESC";

    @Override
    public void recordMovement(StockMovement movement) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            ps.setLong(1, movement.getItemId());
            ps.setInt(2, movement.getQty());
            ps.setString(3, movement.getType().name());
            ps.setString(4, movement.getReason());
            ps.setTimestamp(5, Timestamp.valueOf(movement.getTimestamp()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public List<StockMovement> listByItem(Long itemId) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ITEM)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockMovement> res = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    int qty = rs.getInt("qty");
                    String type = rs.getString("type");
                    String reason = rs.getString("reason");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    LocalDateTime time = ts == null ? LocalDateTime.now() : ts.toLocalDateTime();

                    MovementType mt = MovementType.valueOf(type);
                    StockMovement movement;
                    if (mt == MovementType.IN) {
                        movement = new StockInMovement(id, itemId, qty, reason, time);
                    } else {
                        movement = new StockOutMovement(id, itemId, qty, reason, time);
                    }
                    res.add(movement);
                }
                return res;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
