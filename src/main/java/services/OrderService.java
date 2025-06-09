package services;

import java.time.LocalDateTime;
import java.util.List;

import dto.CreateOrderRequest;
import dto.UpdateOrderStatusRequest;
import entities.Order;
import entities.OrderItem;
import enums.OrderStatus;
import exceptions.errors.BuyerNotFoundException;
import exceptions.errors.OrderNotFoundException;
import interfaces.IOrderRepository;
import interfaces.IOrderService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@ApplicationScoped
public class OrderService implements IOrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    private final IOrderRepository orderRepository;
    private final BuyerClientService buyerClientService;
    private final ProductClientService productClientService;

    public OrderService(IOrderRepository orderRepository, BuyerClientService buyerClientService,
            ProductClientService productClientService) {
        this.orderRepository = orderRepository;
        this.buyerClientService = buyerClientService;
        this.productClientService = productClientService;
    }

    @Override
    public Uni<Order> createPendingOrder(CreateOrderRequest orderRequest, String keycloakId) {
        LOG.infof("Creating pending order for buyer keycloakId=%d", keycloakId);
        return buyerClientService.getBuyerByKeycloakId(keycloakId)
                .onItem().ifNull().failWith(new BuyerNotFoundException(keycloakId))
                .onItem().transformToUni(buyer -> {
                    // 2. Collect all productIds from the order request
                    List<String> productIds = orderRequest.orderItems.stream() // Changed from
                                                                               // List<Integer>
                            .map(item -> item.productId)
                            .toList();

                    // 3. Fetch product details for all productIds
                    return productClientService.getProductsByIds(productIds)
                            .onItem().transformToUni(productDTOs -> {
                                // 4. Map productId to ProductDTO for quick lookup
                                var productMap = productDTOs.stream()
                                        .collect(java.util.stream.Collectors
                                                .toMap(
                                                        dto -> dto.id,
                                                        dto -> dto));

                                // 5. Build OrderItems with product info and requested
                                // quantity
                                List<OrderItem> orderItems = orderRequest.orderItems
                                        .stream()
                                        .map(req -> {
                                            var product = productMap.get(
                                                    req.productId);
                                            if (product == null) {
                                                throw new RuntimeException(
                                                        "Product not found: "
                                                                + req.productId);
                                            }
                                            return new OrderItem(
                                                    req.productId, // Now
                                                                   // String
                                                    product.name,
                                                    product.price,
                                                    product.description,
                                                    req.quantity);
                                        })
                                        .toList();

                                // 6. Create Order with PENDING status
                                Order order = new Order(
                                        buyer,
                                        buyer.keycloakId,
                                        orderItems,
                                        OrderStatus.PENDING,
                                        LocalDateTime.now());
                                orderItems.forEach(item -> item.setOrder(order));
                                // Persist order, then log with MDC after ID is
                                // generated
                                return orderRepository.create(order)
                                        .invoke(persistedOrder -> {
                                            MDC.put("orderId",
                                                    persistedOrder.getId());
                                            LOG.infof("Order created: orderId=%d, keycloakId=%d, items=%d",
                                                    persistedOrder.getId(),
                                                    persistedOrder.getKeycloakId(),
                                                    orderItems.size());
                                            MDC.remove("orderId");
                                        });
                            });
                })
                .onFailure().invoke(e -> LOG.errorf("Failed to create order: %s", e.getMessage()));
    }

    @Override
    public Uni<Order> read(int id) {
        MDC.put("orderId", id);
        LOG.infof("Reading order: orderId=%d", id);
        return orderRepository.read(id)
                .onItem().ifNull().failWith(new OrderNotFoundException(id))
                .onItem()
                .invoke(order -> LOG.infof("Order read successfully: orderId=%d", order.getId()))
                .onItem()
                .transformToUni(order -> buyerClientService.getBuyerByKeycloakId(order.getKeycloakId())
                        .onItem().invoke(order::setBuyer)
                        .replaceWith(order))
                .onFailure().invoke(e -> LOG.errorf("Failed to read order: %s", e.getMessage()))
                .eventually(() -> {
                    MDC.remove("orderId");
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<List<Order>> readAllByUser(String keycloakId) {
        LOG.infof("Reading all orders for user: keycloakId=%s", keycloakId);
        return orderRepository.readAllByUser(keycloakId)
                .onItem()
                .invoke(orders -> LOG.infof("Read %d orders for user: keycloakId=%s", orders.size(),
                        keycloakId))
                .onFailure()
                .invoke(e -> LOG.errorf("Failed to read orders for user: %s", e.getMessage()));
    }

    @Override
    public Uni<Order> updateOrderStatus(UpdateOrderStatusRequest updateOrderStatusRequest) {
        MDC.put("orderId", updateOrderStatusRequest.id);
        LOG.infof("Updating order status: orderId=%d, newStatus=%s", updateOrderStatusRequest.id,
                updateOrderStatusRequest.status);
        return orderRepository.read(updateOrderStatusRequest.id)
                .onItem().ifNull().failWith(new OrderNotFoundException(updateOrderStatusRequest.id))
                .onItem()
                .transform(order -> {
                    order.setStatus(updateOrderStatusRequest.status);
                    return order;
                })
                .onItem().transformToUni(orderRepository::update)
                .onFailure()
                .invoke(e -> LOG.errorf("Failed to update order status: %s", e.getMessage()))
                .eventually(() -> {
                    MDC.remove("orderId");
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Void> delete(int id) {
        MDC.put("orderId", id);
        LOG.infof("Deleting order: orderId=%d", id);
        return orderRepository.read(id)
                .onItem().ifNull().failWith(new OrderNotFoundException(id))
                .onItem().transformToUni(order -> orderRepository.delete(id)
                        .invoke(() -> LOG.infof("Order deleted: orderId=%d", id))
                        .replaceWith(Uni.createFrom().voidItem()))
                .onFailure().invoke(e -> LOG.errorf("Failed to delete order: %s", e.getMessage()))
                .eventually(() -> {
                    MDC.remove("orderId");
                    return Uni.createFrom().voidItem();
                });
    }
}