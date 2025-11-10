package com.company.inventory.dao;

import com.company.inventory.model.Item;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcItemDao implements ItemDao {
    private final javax.sql.DataSource ds;

    public JdbcItemDao(javax.sql.DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Long create(Item item) throws Exception {
        // check SKU uniqueness before insert (optional double-check at DB constraint level)
        if (findBySku(item.getSku()).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + item.getSku());
        }

        final String sql = "INSERT INTO item (name, sku, purchase_price, sale_price, reorder_level) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getSku());
            if (item.getPurchasePrice() != null) ps.setDouble(3, item.getPurchasePrice()); else ps.setNull(3, Types.DECIMAL);
            if (item.getSalePrice() != null) ps.setDouble(4, item.getSalePrice()); else ps.setNull(4, Types.DECIMAL);
            if (item.getReorderLevel() != null) ps.setInt(5, item.getReorderLevel()); else ps.setNull(5, Types.INTEGER);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new SQLException("Failed to obtain generated key for item");
                }
            }
        }
    }

    @Override
    public Optional<Item> findById(Long id) throws Exception {
        String sql = "SELECT id, name, sku, purchase_price, sale_price, reorder_level FROM item WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowToItem(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Item> findBySku(String sku) throws Exception {
        String sql = "SELECT id, name, sku, purchase_price, sale_price, reorder_level FROM item WHERE sku = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowToItem(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Item> listAll() throws Exception {
        String sql = "SELECT id, name, sku, purchase_price, sale_price, reorder_level FROM item ORDER BY id";
        List<Item> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rowToItem(rs));
        }
        return out;
    }

    private Item rowToItem(ResultSet rs) throws SQLException {
        Item it = new Item();
        it.setId(rs.getLong("id"));
        it.setName(rs.getString("name"));
        it.setSku(rs.getString("sku"));
        double p = rs.getDouble("purchase_price");
        if (!rs.wasNull()) it.setPurchasePrice(p);
        double s = rs.getDouble("sale_price");
        if (!rs.wasNull()) it.setSalePrice(s);
        int r = rs.getInt("reorder_level");
        if (!rs.wasNull()) it.setReorderLevel(r);
        return it;
    }
}
