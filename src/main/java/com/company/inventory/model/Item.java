package com.company.inventory.model;

public class Item extends BaseEntity {
    private String name;
    private Sku sku;
    private Long categoryId;
    private Long supplierId;
    private Double purchasePrice;
    private Double salePrice;
    private Integer reorderLevel;

    public Item() {}

    public Item(String name, Sku sku) {
        this(null, name, sku,null,null,null,null,null);
    }

    public Item(Long id, String name, Sku sku,Long categoryId,Long supplierId, Double purchasePrice, Double salePrice, Integer reorderLevel) {
        this.id=id;
        this.name = name;
        this.sku = sku;
        this.categoryId=categoryId;
        this.supplierId=supplierId;
        this.purchasePrice=purchasePrice;
        this.salePrice=salePrice;
        this.reorderLevel=reorderLevel;
    }

    // getters and setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Sku getSku() { return sku; }
    public void setSku(Sku sku) { this.sku = sku; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }

    public Double getSalePrice() { return salePrice; }
    public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    @Override
    public String toString() {
        return String.format("Item[id=%s, name=%s, sku=%s, categoryId=%s, supplierId=%s, purchasePrice=%s, salePrice=%s, reorderLevel=%s]", id, name, sku,categoryId,supplierId,purchasePrice,salePrice,reorderLevel);
    }
}
