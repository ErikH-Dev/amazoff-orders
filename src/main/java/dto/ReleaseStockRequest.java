package dto;

import java.util.List;

public class ReleaseStockRequest {
    public List<ReserveStockItem> items;

    public ReleaseStockRequest() {}

    public ReleaseStockRequest(List<ReserveStockItem> items) {
        this.items = items;
    }

    public List<ReserveStockItem> getItems() {
        return items;
    }

    public void setItems(List<ReserveStockItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "ReleaseStockRequest{" +
                "items=" + items +
                '}';
    }
}