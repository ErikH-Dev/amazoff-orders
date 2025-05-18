package dto;

import java.util.List;

public class StockReservationFailed {
    public List<ReserveStockItem> items;
    public String reason;
    public String status = "StockReservationFailed";
    public StockReservationFailed(List<ReserveStockItem> items, String reason) {
        this.items = items;
        this.reason = reason;
    }
}