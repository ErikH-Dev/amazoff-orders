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
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import services.OrderService;
import services.ProductClientService;

@ApplicationScoped
public class OrderSagaOrchestrator {

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);

    private final OrderService orderService;
    private final ProductClientService productClientService;

    public OrderSagaOrchestrator(OrderService orderService, ProductClientService productClientService) {
        this.orderService = orderService;
        this.productClientService = productClientService;
    }

    public Uni<Order> createOrderWithSaga(CreateOrderRequest request) {
        LOG.info("Starting order saga orchestration");
        return orderService.createPendingOrder(request)
                .onItem().invoke(order -> {
                    MDC.put("orderId", order.getId());
                    LOG.infof("Order created in saga: orderId=%d", order.getId());
                })
                .onItem().transformToUni(order -> {
                    List<ReserveStockItem> reserveItems = order.getOrderItems().stream()
                            .map(item -> new ReserveStockItem(item.getProductId(), item.getQuantity()))
                            .toList();

                    LOG.infof("Reserving stock for orderId=%d", order.getId());
                    return productClientService.reserveStock(reserveItems)
                            .onItem().transformToUni(stockResult -> {
                                if (stockResult instanceof StockReserved) {
                                    LOG.infof("Stock reserved for orderId=%d", order.getId());
                                    return orderService.updateOrderStatus(new UpdateOrderStatusRequest(order.getId(), OrderStatus.CONFIRMED))
                                            .onItem().transformToUni(v -> Uni.createFrom().item(order));
                                } else if (stockResult instanceof StockReservationFailed failed) {
                                    LOG.warnf("Stock reservation failed for orderId=%d: %s", order.getId(), failed.reason);
                                    return orderService.updateOrderStatus(new UpdateOrderStatusRequest(order.getId(), OrderStatus.FAILED))
                                            .onItem().transformToUni(v -> Uni.createFrom().failure(
                                                    new OrderCreationException(
                                                            "Stock reservation failed: " + failed.reason)));
                                } else {
                                    LOG.errorf("Unknown stock reservation response for orderId=%d", order.getId());
                                    return Uni.createFrom().failure(
                                            new OrderCreationException("Unknown stock reservation response"));
                                }
                            })
                            .eventually(() -> {
                                MDC.remove("orderId");
                                return Uni.createFrom().voidItem();
                            });
                });
    }
}