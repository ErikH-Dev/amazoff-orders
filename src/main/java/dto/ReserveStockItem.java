package dto;

public class ReserveStockItem {
    public String productId;
    public int quantity;

    public ReserveStockItem() {}

    public ReserveStockItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ReserveStockItem{productId='" + productId + "', quantity=" + quantity + "}";
    }
}