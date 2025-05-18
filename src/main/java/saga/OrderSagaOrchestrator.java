package saga;

import java.util.List;

import dto.CreateOrderRequest;
import dto.ReserveStockItem;
import dto.StockReserved;
import dto.UpdateOrderStatusRequest;
import dto.StockReservationFailed;
import entities.Order;
import enums.OrderStatus;
import exceptions.errors.OrderCreationException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import services.OrderService;
import services.ProductClientService;

@ApplicationScoped
public class OrderSagaOrchestrator {

    private final OrderService orderService;
    private final ProductClientService productClientService;

    public OrderSagaOrchestrator(OrderService orderService, ProductClientService productClientService) {
        this.orderService = orderService;
        this.productClientService = productClientService;
    }

    public Uni<Order> createOrderWithSaga(CreateOrderRequest request) {
        return orderService.createPendingOrder(request)
                .onItem().transformToUni(order -> {
                    List<ReserveStockItem> reserveItems = order.getOrderItems().stream()
                            .map(item -> new ReserveStockItem(item.getId(), item.getQuantity()))
                            .toList();

                    return productClientService.reserveStock(reserveItems)
                            .onItem().transformToUni(stockResult -> {
                                if (stockResult instanceof StockReserved) {
                                    return orderService.updateOrderStatus(new UpdateOrderStatusRequest(order.getId(), OrderStatus.CONFIRMED))
                                            .onItem().transformToUni(v -> Uni.createFrom().item(order));
                                } else if (stockResult instanceof StockReservationFailed failed) {
                                    return orderService.updateOrderStatus(new UpdateOrderStatusRequest(order.getId(), OrderStatus.FAILED))
                                            .onItem().transformToUni(v -> Uni.createFrom().failure(
                                                    new OrderCreationException(
                                                            "Stock reservation failed: " + failed.reason)));
                                } else {
                                    return Uni.createFrom().failure(
                                            new OrderCreationException("Unknown stock reservation response"));
                                }
                            });
                });
    }
}