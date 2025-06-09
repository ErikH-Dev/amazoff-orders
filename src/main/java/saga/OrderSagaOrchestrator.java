package saga;

import java.util.List;

import dto.CreateOrderRequest;
import dto.ReserveStockItem;
import dto.SagaContext;
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

    public Uni<Order> createOrderWithSaga(CreateOrderRequest request, String keycloakId) {
        LOG.info("Starting order saga orchestration");
        SagaContext context = new SagaContext();

        return createPendingOrder(request, context, keycloakId)
                .onItem().transformToUni(order -> reserveProductStock(order, context))
                .onItem().transformToUni(order -> confirmOrder(order, context))
                .onFailure().recoverWithUni(error -> handleSagaFailure(error, context))
                .eventually(() -> cleanupContext());
    }

    private Uni<Order> createPendingOrder(CreateOrderRequest request, SagaContext context, String keycloakId) {
        return orderService.createPendingOrder(request, keycloakId)
                .onItem().invoke(order -> {
                    context.order = order;
                    context.orderCreated = true;
                    context.reserveItems = extractReserveItems(order);
                    MDC.put("orderId", order.getId());
                    LOG.infof("Order created: orderId=%d", order.getId());
                });
    }

    private Uni<Order> reserveProductStock(Order order, SagaContext context) {
        LOG.infof("Reserving stock for orderId=%d", order.getId());

        return productClientService.reserveStock(context.reserveItems)
                .onItem().transformToUni(stockResult -> handleStockReservationResult(stockResult, order, context));
    }

    private Uni<Order> handleStockReservationResult(Object stockResult, Order order, SagaContext context) {
        if (stockResult instanceof StockReserved) {
            context.stockReserved = true;
            LOG.infof("Stock reserved for orderId=%d", order.getId());
            return Uni.createFrom().item(order);
        }

        if (stockResult instanceof StockReservationFailed failed) {
            LOG.warnf("Stock reservation failed for orderId=%d: %s", order.getId(), failed.reason);
            return Uni.createFrom().failure(new OrderCreationException("Stock reservation failed: " + failed.reason));
        }

        LOG.errorf("Unknown stock reservation response for orderId=%d", order.getId());
        return Uni.createFrom().failure(new OrderCreationException("Unknown stock reservation response"));
    }

    private Uni<Order> confirmOrder(Order order, SagaContext context) {
        return orderService.updateOrderStatus(
                new UpdateOrderStatusRequest(order.getId(), OrderStatus.CONFIRMED))
                .onItem().invoke(updatedOrder -> {
                    context.orderConfirmed = true;
                    LOG.infof("Order confirmed: orderId=%d", order.getId());
                })
                .onFailure().recoverWithUni(updateError -> {
                    LOG.errorf("Failed to confirm order %d: %s", order.getId(), updateError.getMessage());
                    return Uni.createFrom().failure(
                            new OrderCreationException("Order confirmation failed: " + updateError.getMessage()));
                });
    }

    private Uni<Order> handleSagaFailure(Throwable error, SagaContext context) {
        LOG.errorf("Saga failed, compensating: %s", error.getMessage());
        return compensate(context)
                .onItem().transformToUni(v -> Uni.createFrom().failure(error));
    }

    private Uni<Void> compensate(SagaContext context) {
        LOG.info("Starting compensation actions");

        return releaseReservedStock(context)
                .onItem().transformToUni(v -> markOrderAsFailed(context))
                .onItem().invoke(() -> LOG.info("Compensation completed"));
    }

    private Uni<Void> releaseReservedStock(SagaContext context) {
        if (!context.stockReserved || context.reserveItems == null) {
            return Uni.createFrom().voidItem();
        }

        LOG.infof("Compensating: releasing stock for order %d", context.order.getId());
        return productClientService.releaseStock(context.reserveItems)
                .onFailure().invoke(e -> LOG.errorf("Failed to release stock during compensation: %s", e.getMessage()))
                .onFailure().recoverWithItem(v -> null)
                .replaceWith(Uni.createFrom().voidItem());
    }

    private Uni<Void> markOrderAsFailed(SagaContext context) {
        if (!context.orderCreated || context.orderConfirmed) {
            return Uni.createFrom().voidItem();
        }

        LOG.infof("Compensating: marking order %d as failed", context.order.getId());
        return orderService.updateOrderStatus(
                new UpdateOrderStatusRequest(context.order.getId(), OrderStatus.FAILED))
                .onFailure()
                .invoke(e -> LOG.errorf("Failed to mark order as failed during compensation: %s", e.getMessage()))
                .onFailure().recoverWithItem(v -> null)
                .replaceWith(Uni.createFrom().voidItem());
    }

    private List<ReserveStockItem> extractReserveItems(Order order) {
        return order.getOrderItems().stream()
                .map(item -> new ReserveStockItem(item.getProductId(), item.getQuantity()))
                .toList();
    }

    private Uni<Void> cleanupContext() {
        MDC.remove("orderId");
        return Uni.createFrom().voidItem();
    }
}