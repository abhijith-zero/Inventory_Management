-- 001_init.sql: create minimal tables used in early sprints
CREATE TABLE IF NOT EXISTS category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  contact_name VARCHAR(100),
  phone VARCHAR(30),
  email VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  sku VARCHAR(100) NOT NULL UNIQUE,
  category_id BIGINT,
  supplier_id BIGINT,
  purchase_price DECIMAL(12,2),
  sale_price DECIMAL(12,2),
  reorder_level INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  UNIQUE(item_id, warehouse_id)
);
