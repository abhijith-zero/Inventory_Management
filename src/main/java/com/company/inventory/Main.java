package com.company.inventory;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.dao.JdbcItemDao;
import com.company.inventory.dao.JdbcStockDao;
import com.company.inventory.model.Item;
import com.company.inventory.service.InventoryService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Inventory Management System...");

        // Initialize database and run migrations
        var dataSource=DbConnectionManager.init();
        System.out.println("Database initialized successfully.");

        // Simple connectivity test
        try (Connection conn = DbConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP()");
            if (rs.next()) {
                System.out.println("H2 Connection OK. Current time: " + rs.getString(1));
            }
        } catch (Exception e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }

        JdbcItemDao itemDao = new JdbcItemDao(dataSource);
        JdbcStockDao stockDao = new JdbcStockDao(dataSource);
        InventoryService service = new InventoryService(itemDao, stockDao, 1L); // default warehouse = 1

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nMenu: 1) Add item  2) List items 3) Update Items  0) Exit");
            String opt = sc.nextLine();
            if ("0".equals(opt)) break;
            if ("1".equals(opt)) {
                System.out.print("Name: ");
                String name = sc.nextLine().trim();
                System.out.print("SKU: ");
                String sku = sc.nextLine().trim();
                System.out.print("Initial stock (integer): ");
                int qty = Integer.parseInt(sc.nextLine().trim());
                Item item = new Item();
                item.setName(name);
                item.setSku(sku);
                try {
                    long id = service.createItem(item, qty);
                    System.out.println("Created item id=" + id);
                } catch (Exception e) {
                    System.err.println("Failed to create item: " + e.getMessage());
                }
            } else if ("2".equals(opt)) {
                service.listItems().forEach(System.out::println);
            }
            else if ("3".equals(opt)) {
                    System.out.print("Item id to update: ");
                    long id = Long.parseLong(sc.nextLine().trim());
                    System.out.print("New name (leave empty to keep): ");
                    String name = sc.nextLine().trim();
                    System.out.print("New SKU (leave empty to keep): ");
                    String sku = sc.nextLine().trim();
                    System.out.print("Category ID (leave empty for null): ");
                    String cat = sc.nextLine().trim();
                    System.out.print("Supplier ID (leave empty for null): ");
                    String sup = sc.nextLine().trim();
                    System.out.print("Purchase price (leave empty for null): ");
                    String pp = sc.nextLine().trim();
                    System.out.print("Sale price (leave empty for null): ");
                    String sp = sc.nextLine().trim();
                    System.out.print("Reorder level (leave empty for null): ");
                    String rl = sc.nextLine().trim();

                    Item item = itemDao.findById(id).orElseThrow(() -> new RuntimeException("Item not found: " + id));
                    if (!name.isEmpty()) item.setName(name);
                    if (!sku.isEmpty()) item.setSku(sku);
                    item.setCategoryId(cat.isEmpty() ? null : Long.valueOf(cat));
                    item.setSupplierId(sup.isEmpty() ? null : Long.valueOf(sup));
                    item.setPurchasePrice(pp.isEmpty() ? null : Double.valueOf(pp));
                    item.setSalePrice(sp.isEmpty() ? null : Double.valueOf(sp));
                    item.setReorderLevel(rl.isEmpty() ? null : Integer.valueOf(rl));

                    try {
                        service.updateItem(item);
                        System.out.println("Updated item " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to update item: " + e.getMessage());
                    }
                }

        }

        System.out.println("Goodbye");


        System.out.println("System ready.");
    }
}
