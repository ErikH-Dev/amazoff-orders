package dto;

import java.util.List;

public class StockReleased {
    public List<ReserveStockItem> items;
    public String status;

    public StockReleased() {
        this.status = "StockReleased";
    }

    public StockReleased(List<ReserveStockItem> items) {
        this.items = items;
        this.status = "StockReleased";
    }

    public List<ReserveStockItem> getItems() {
        return items;
    }

    public void setItems(List<ReserveStockItem> items) {
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StockReleased{" +
                "items=" + items +
                ", status='" + status + '\'' +
                '}';
    }
}