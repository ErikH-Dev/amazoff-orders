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

@ApplicationScoped
public class OrderService implements IOrderService {

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
    public Uni<Order> createPendingOrder(CreateOrderRequest orderRequest) {
        // 1. Fetch buyer
        return buyerClientService.getBuyerByOauthId(orderRequest.oauthId)
                .onItem().ifNull().failWith(new BuyerNotFoundException(orderRequest.oauthId))
                .onItem().transformToUni(buyer -> {
                    // 2. Collect all productIds from the order request
                    List<Integer> productIds = orderRequest.orderItems.stream()
                            .map(item -> item.productId)
                            .toList();

                    // 3. Fetch product details for all productIds
                    return productClientService.getProductsByIds(productIds)
                            .onItem().transformToUni(productDTOs -> {
                                // 4. Map productId to ProductDTO for quick lookup
                                var productMap = productDTOs.stream()
                                        .collect(java.util.stream.Collectors.toMap(
                                                dto -> dto.id,
                                                dto -> dto));

                                // 5. Build OrderItems with product info and requested quantity
                                List<OrderItem> orderItems = orderRequest.orderItems.stream()
                                        .map(req -> {
                                            var product = productMap.get(req.productId);
                                            if (product == null) {
                                                throw new RuntimeException("Product not found: " + req.productId);
                                            }
                                            return new OrderItem(
                                                    product.name,
                                                    product.price,
                                                    product.description,
                                                    req.quantity);
                                        })
                                        .toList();

                                // 6. Create Order with PENDING status
                                Order order = new Order(
                                        orderRequest.oauthId,
                                        orderItems,
                                        OrderStatus.PENDING,
                                        LocalDateTime.now());
                                order.setBuyer(buyer);
                                orderItems.forEach(item -> item.setOrder(order));
                                return orderRepository.create(order);
                            });
                });
    }

    @Override
    public Uni<Order> read(int id) {
        return orderRepository.read(id)
                .onItem().ifNull().failWith(new OrderNotFoundException(id))
                .onItem().transformToUni(order -> buyerClientService.getBuyerByOauthId(order.getBuyerId())
                        .onItem().invoke(order::setBuyer)
                        .replaceWith(order));
    }

    @Override
    public Uni<List<Order>> readAllByUser(int oauthId) {
        return orderRepository.readAllByUser(oauthId);
    }

    @Override
    public Uni<Order> updateOrderStatus(UpdateOrderStatusRequest updateOrderStatusRequest) {
        return orderRepository.read(updateOrderStatusRequest.id)
                .onItem().ifNull().failWith(new OrderNotFoundException(updateOrderStatusRequest.id))
                .onItem()
                .transform(order -> new Order(order.getId(), order.getBuyer(), order.getOrderItems(),
                        updateOrderStatusRequest.status, order.getOrderDate()))
                .onItem().transformToUni(orderRepository::update);
    }

    @Override
    public Uni<Void> delete(int id) {
        return orderRepository.read(id)
                .onItem().ifNull().failWith(new OrderNotFoundException(id))
                .onItem().transformToUni(order -> orderRepository.delete(id).replaceWith(Uni.createFrom().voidItem()));
    }
}