package dto;

import java.util.List;

import entities.Order;

public class SagaContext {
    public Order order;
    public boolean orderCreated = false;
    public boolean stockReserved = false;
    public boolean orderConfirmed = false;
    public List<ReserveStockItem> reserveItems;
}
