package dto;

public class ReserveStockItem {
    public int productId;
    public int quantity;

    public ReserveStockItem(int id, int quantity) {
        this.productId = id;
        this.quantity = quantity;
    }
}