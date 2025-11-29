package com.company.inventory.dao;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.model.Item;
import com.company.inventory.model.Sku;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDaoJdbc implements ItemDao {

    private static final String INSERT_SQL =
            "INSERT INTO item (name, sku, category_id, supplier_id, purchase_price, sale_price, reorder_level) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE item SET name=?, sku=?, category_id=?, supplier_id=?, purchase_price=?, sale_price=?, reorder_level=? WHERE id=?";

    private static final String SELECT_BY_ID = "SELECT * FROM item WHERE id = ?";
    private static final String SELECT_BY_SKU = "SELECT * FROM item WHERE sku = ?";
    private static final String SEARCH_SQL =
            "SELECT * FROM item WHERE LOWER(name) LIKE ? OR LOWER(sku) LIKE ? LIMIT 100";

    @Override
    public Item create(Item item) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            bindInsert(ps, item);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    System.out.println("Generated ID: " + id);
                    return new Item(id,
                            item.getName(),
                            item.getSku(),
                            item.getCategoryId(),
                            item.getSupplierId(),
                            item.getPurchasePrice(),
                            item.getSalePrice(),
                            item.getReorderLevel());
                } else {
                    throw new DataAccessException("Failed to retrieve generated id for item");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Item update(Item item) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_SQL)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getSku().value());

            if (item.getCategoryId() == null)
                ps.setNull(3, Types.BIGINT);
            else
                ps.setLong(3, item.getCategoryId());

            if (item.getSupplierId() == null)
                ps.setNull(4, Types.BIGINT);
            else
                ps.setLong(4, item.getSupplierId());

            ps.setDouble(5, item.getPurchasePrice());
            ps.setDouble(6, item.getSalePrice());
            ps.setInt(7, item.getReorderLevel());
            ps.setLong(8, item.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) throw new DataAccessException("No item updated with id " + item.getId());

            return item;

        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Optional<Item> findById(Long id) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Optional<Item> findBySku(String sku) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_SKU)) {

            ps.setString(1, sku);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public List<Item> search(String query) {
        try (Connection c = DbConnectionManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SEARCH_SQL)) {

            String like = "%" + query.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) items.add(mapRow(rs));
                return items;
            }

        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private void bindInsert(PreparedStatement ps, Item item) throws SQLException {
        ps.setString(1, item.getName());
        ps.setString(2, item.getSku().value());

        if (item.getCategoryId() == null)
            ps.setNull(3, Types.BIGINT);
        else
            ps.setLong(3, item.getCategoryId());

        if (item.getSupplierId() == null)
            ps.setNull(4, Types.BIGINT);
        else
            ps.setLong(4, item.getSupplierId());

        ps.setDouble(5, item.getPurchasePrice());
        ps.setDouble(6, item.getSalePrice());
        ps.setInt(7, item.getReorderLevel());
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        Sku sku = new Sku(rs.getString("sku"));

        Long categoryId = (rs.getObject("category_id") == null)
                ? null
                : rs.getLong("category_id");

        Long supplierId = (rs.getObject("supplier_id") == null)
                ? null
                : rs.getLong("supplier_id");

        double purchasePrice = rs.getDouble("purchase_price");
        double salePrice = rs.getDouble("sale_price");

        int reorder = rs.getInt("reorder_level");

        return new Item(id, name, sku, categoryId, supplierId, purchasePrice, salePrice, reorder);
    }
}
