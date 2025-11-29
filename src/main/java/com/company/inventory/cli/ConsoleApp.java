package com.company.inventory.cli;

import com.company.inventory.config.DbConnectionManager;
import com.company.inventory.dao.*;
import com.company.inventory.model.*;
import com.company.inventory.service.InventoryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Simple Console CLI for Inventory system
 */
public class ConsoleApp {

    private final InventoryService service;
    private final ItemDao itemDao;
    private final StockDao stockDao;
    private final StockMovementDao movementDao;
    private final Scanner scanner = new Scanner(System.in);

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConsoleApp(ItemDao itemDao, StockDao stockDao, StockMovementDao movementDao) {
        this.itemDao = itemDao;
        this.stockDao = stockDao;
        this.movementDao = movementDao;
        this.service = new InventoryService(itemDao, stockDao, movementDao);
    }

    public static void main(String[] args) {
        // Create JDBC DAO implementations
        var itemDao = new ItemDaoJdbc();
        var stockDao = new StockDaoJdbc();
        var movementDao = new StockMovementDaoJdbc();

        // Initialize DB schema (H2 default)
        initDb();

        // Start CLI
        ConsoleApp app = new ConsoleApp(itemDao, stockDao, movementDao);
        app.run();
    }

    private static void initDb() {
        // Runs DDL to create tables if not exists. Uses DbConnectionManager.
        try (Connection c = DbConnectionManager.getConnection();
             Statement s = c.createStatement()) {

            // item table
            s.execute("""
                    CREATE TABLE IF NOT EXISTS item (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      sku VARCHAR(100) NOT NULL UNIQUE,
                      category_id BIGINT NULL,
                      supplier_id BIGINT NULL,
                      purchase_price DOUBLE NOT NULL,
                      sale_price DOUBLE NOT NULL,
                      reorder_level INT NOT NULL
                    );
                    """);

            // stock table - one row per item
            s.execute("""
                    CREATE TABLE IF NOT EXISTS stock (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      item_id BIGINT NOT NULL UNIQUE,
                      quantity INT NOT NULL,
                      FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
                    );
                    """);

            // stock_movement table
            s.execute("""
                    CREATE TABLE IF NOT EXISTS stock_movement (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      item_id BIGINT NOT NULL,
                      qty INT NOT NULL,
                      type VARCHAR(10) NOT NULL,
                      reason VARCHAR(255),
                      timestamp TIMESTAMP NOT NULL,
                      FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
                    );
                    """);

            System.out.println("Database initialized.");
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize DB schema", ex);
        }
    }

    public void run() {
        System.out.println("Welcome to Inventory CLI");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Select option").trim();
            try {
                switch (choice) {
                    case "1" -> createItemFlow();
                    case "2" -> updateItemFlow();
                    case "3" -> searchItemsFlow();
                    case "4" -> viewStockFlow();
                    case "5" -> increaseStockFlow();
                    case "6" -> decreaseStockFlow();
                    case "7" -> listMovementsFlow();
                    case "0" -> {
                        running = false;
                        System.out.println("Exiting. Bye!");
                    }
                    default -> System.out.println("Unknown option");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("===== MENU =====");
        System.out.println("1. Add new item");
        System.out.println("2. Update item");
        System.out.println("3. Search items");
        System.out.println("4. View item stock");
        System.out.println("5. Increase stock (IN)");
        System.out.println("6. Decrease stock (OUT)");
        System.out.println("7. List stock movements for item");
        System.out.println("0. Exit");
    }

    private void createItemFlow() {
        System.out.println("--- Create Item ---");
        String name = readLine("Name");
        String skuStr = readLine("SKU (alphanumeric and hyphens only)");
        long categoryId = readLongNullable("CategoryId (enter for null)");
        long supplierId = readLongNullable("SupplierId (enter for null)");
        double purchasePrice = readDouble("Purchase price");
        double salePrice = readDouble("Sale price");
        int reorder = readInt("Reorder level");
        Sku sku = new Sku(skuStr);
        Item item = new Item(null, name, sku, categoryIdOrNull(categoryId), supplierIdOrNull(supplierId), purchasePrice, salePrice, reorder);

        Item created = service.createItem(item);
        System.out.println("Created item: " + created);
    }

    private void updateItemFlow() {
        System.out.println("--- Update Item ---");
        Long id = readLong("Item id");
        Optional<Item> opt = itemDao.findById(id);
        if (opt.isEmpty()) {
            System.out.println("Item not found: " + id);
            return;
        }
        Item existing = opt.get();
        System.out.println("Current: " + existing);
        String name = readLineOrDefault("Name", existing.getName());
        String skuStr = readLineOrDefault("SKU", existing.getSku().value());
        long categoryId = readLongNullableDefault("CategoryId (enter for null)", existing.getCategoryId());
        long supplierId = readLongNullableDefault("SupplierId (enter for null)", existing.getSupplierId());
        double purchasePrice = readDoubleOrDefault("Purchase price", existing.getPurchasePrice());
        double salePrice = readDoubleOrDefault("Sale price", existing.getSalePrice());
        int reorder = readInt("Reorder level", existing.getReorderLevel());

        Item updated = new Item(existing.getId(), name, new Sku(skuStr), nullable(categoryId), nullable(supplierId), purchasePrice, salePrice, reorder);
        service.updateItem(updated);
        System.out.println("Updated item: " + updated);
    }

    private void searchItemsFlow() {
        System.out.println("--- Search Items ---");
        String q = readLine("Query (name or sku)");
        List<Item> items = service.searchItems(q);
        if (items.isEmpty()) {
            System.out.println("No items found.");
            return;
        }
        items.forEach(i -> System.out.println(i.getId() + " | " + i.getName() + " | " + i.getSku() + " | stock=" + stockDao.getStock(i.getId()).map(Stock::getQuantity).orElse(0)));
    }

    private void viewStockFlow() {
        System.out.println("--- View Stock ---");
        Long itemId = readLong("Item id");
        Optional<Stock> st = service.viewStock(itemId);
        if (st.isPresent()) {
            Stock stock = st.get();
            System.out.println("Item " + itemId + " stock = " + stock.getQuantity());
        } else {
            System.out.println("No stock record. (interpreted as 0)");
        }
    }

    private void increaseStockFlow() {
        System.out.println("--- Increase Stock ---");
        Long itemId = readLong("Item id");
        int qty = readInt("Quantity to add");
        String reason = readLine("Reason (optional)");
        Stock s = service.increaseStock(itemId, qty, reason);
        System.out.println("Stock updated: item=" + itemId + " qty=" + s.getQuantity());
    }

    private void decreaseStockFlow() {
        System.out.println("--- Decrease Stock ---");
        Long itemId = readLong("Item id");
        int qty = readInt("Quantity to remove");
        String reason = readLine("Reason (optional)");
        Stock s = service.decreaseStock(itemId, qty, reason);
        System.out.println("Stock updated: item=" + itemId + " qty=" + s.getQuantity());
    }

    private void listMovementsFlow() {
        System.out.println("--- Movements ---");
        Long itemId = readLong("Item id");
        List<StockMovement> movements = service.listMovements(itemId);
        if (movements.isEmpty()) {
            System.out.println("No movements found.");
            return;
        }
        for (StockMovement m : movements) {
            System.out.printf("%d | %s | %d | %s | %s%n",
                    m.getId() == null ? -1L : m.getId(),
                    m.getType(),
                    m.getQty(),
                    m.getReason(),
                    m.getTimestamp().format(DF));
        }
    }
    private String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine();
    }

    private String readLineOrDefault(String prompt, String defaultVal) {
        System.out.print(prompt + " [" + defaultVal + "]: ");
        String s = scanner.nextLine();
        return s.isBlank() ? defaultVal : s;
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                String s = readLine(prompt);
                return Integer.parseInt(s.trim());
            } catch (Exception e) {
                System.out.println("Invalid integer, try again.");
            }
        }
    }

    private int readInt(String prompt, int defaultVal) {
        System.out.print(prompt + " [" + defaultVal + "]: ");
        String s = scanner.nextLine();
        if (s.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input; using default");
            return defaultVal;
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            try {
                String s = readLine(prompt);
                return Double.parseDouble(s.trim());
            } catch (Exception e) {
                System.out.println("Invalid number, try again.");
            }
        }
    }

    private double readDoubleOrDefault(String prompt, double defaultVal) {
        System.out.print(prompt + " [" + defaultVal + "]: ");
        String s = scanner.nextLine();
        if (s.isBlank()) return defaultVal;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input; using default");
            return defaultVal;
        }
    }

    private Long readLong(String prompt) {
        while (true) {
            try {
                String s = readLine(prompt);
                return Long.parseLong(s.trim());
            } catch (Exception e) {
                System.out.println("Invalid long, try again.");
            }
        }
    }

    private long readLongNullable(String prompt) {
        System.out.print(prompt + ": ");
        String s = scanner.nextLine().trim();
        if (s.isEmpty()) return -1L; // sentinel for null in this app
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private long readLongNullableDefault(String prompt, Long defaultVal) {
        System.out.print(prompt + " [" + (defaultVal == null ? "" : defaultVal) + "]: ");
        String s = scanner.nextLine().trim();
        if (s.isEmpty()) return defaultVal == null ? -1L : defaultVal;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultVal == null ? -1L : defaultVal;
        }
    }

    private Long nullable(long sentinel) {
        return sentinel <= 0 ? null : sentinel;
    }

    private Long categoryIdOrNull(long value) {
        return value <= 0 ? null : value;
    }

    private Long supplierIdOrNull(long value) {
        return value <= 0 ? null : value;
    }

}