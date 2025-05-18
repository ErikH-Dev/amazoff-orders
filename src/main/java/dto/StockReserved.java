package dto;

import java.util.List;

public class StockReserved {
    public List<ReserveStockItem> items;
    public String status = "StockReserved";
    public StockReserved(List<ReserveStockItem> items) { this.items = items; }
}