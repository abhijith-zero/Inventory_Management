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
    public static void main(String[] args) {
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
            System.out.println("\nMenu: 1) Add item  2) List items  0) Exit");
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
        }

        System.out.println("Goodbye");


        System.out.println("System ready.");
    }
}
