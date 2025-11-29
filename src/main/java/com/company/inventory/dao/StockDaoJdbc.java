package com.company.inventory.dao;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.model.Stock;

import java.sql.*;
import java.util.Optional;

/**
 * Simple Stock DAO where each item has a single stock row.
 * Columns: id, item_id (unique), quantity
 */
public class StockDaoJdbc implements StockDao {

    private static final String SELECT_SQL = "SELECT * FROM stock WHERE item_id = ?";
    private static final String INSERT_SQL = "INSERT INTO stock (item_id, quantity) VALUES (?, ?)";
    private static final String UPDATE_SQL = "UPDATE stock SET quantity = ? WHERE item_id = ?";

    @Override
    public Optional<Stock> getStock(Long itemId) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_SQL)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    int quantity = rs.getInt("quantity");
                    return Optional.of(new Stock(itemId, quantity)); // note Stock constructor sets id to null in model; if you need id use different constructor
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Stock upsertStock(Stock stock) {
        // Try update first
        try (Connection c = DbConnectionManager.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement update = c.prepareStatement(UPDATE_SQL)) {
                update.setInt(1, stock.getQuantity());
                update.setLong(2, stock.getItemId());
                int updated = update.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement insert = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                        insert.setLong(1, stock.getItemId());
                        insert.setInt(2, stock.getQuantity());
                        insert.executeUpdate();
                        try (ResultSet rs = insert.getGeneratedKeys()) {
                            if (rs.next()) {
                                long id = rs.getLong(1);
                                c.commit();
                                return new Stock(stock.getItemId(), stock.getQuantity()); // id not stored in Stock model here
                            }
                        }
                    }
                } else {
                    c.commit();
                    return stock;
                }
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
        throw new DataAccessException("Unexpected error in upsertStock");

    }
}
