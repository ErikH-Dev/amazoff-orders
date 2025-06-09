package dto;

import java.util.List;

public class StockReleaseFailed {
    public List<ReserveStockItem> items;
    public String reason;
    public String status;

    public StockReleaseFailed() {
        this.status = "StockReleaseFailed";
    }

    public StockReleaseFailed(List<ReserveStockItem> items, String reason) {
        this.items = items;
        this.reason = reason;
        this.status = "StockReleaseFailed";
    }

    public List<ReserveStockItem> getItems() {
        return items;
    }

    public void setItems(List<ReserveStockItem> items) {
        this.items = items;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StockReleaseFailed{" +
                "items=" + items +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}