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

    public OrderService(IOrderRepository orderRepository, BuyerClientService buyerClientService, ProductClientService productClientService) {
        this.orderRepository = orderRepository;
        this.buyerClientService = buyerClientService;
        this.productClientService = productClientService;
    }

    @Override
    public Uni<Order> create(CreateOrderRequest orderRequest) {
        return buyerClientService.getBuyerByOauthId(orderRequest.oauthId)
            .onItem().ifNull().failWith(new BuyerNotFoundException(orderRequest.oauthId))
            .onItem().transformToUni(buyer ->
                productClientService.getProductsByIds(orderRequest.orderItemsIds)
                    .onItem().transform(productDTOs -> {
                        List<OrderItem> orderItems = productDTOs.stream()
                            .map(product -> new OrderItem(
                                product.name,
                                product.price,
                                product.description,
                                1
                            ))
                            .toList();
                        Order order = new Order(orderRequest.oauthId, orderItems, OrderStatus.PENDING, LocalDateTime.now());
                        orderItems.forEach(item -> item.setOrder(order));
                        order.setBuyer(buyer);
                        return order;
                    })
            )
            .onItem().transformToUni(orderRepository::create);
    }

    @Override
    public Uni<Order> read(int id) {
        return orderRepository.read(id)
            .onItem().ifNull().failWith(new OrderNotFoundException(id))
            .onItem().transformToUni(order ->
                buyerClientService.getBuyerByOauthId(order.getBuyerId())
                    .onItem().invoke(order::setBuyer)
                    .replaceWith(order)
            );
    }

    @Override
    public Uni<List<Order>> readAllByUser(int oauthId) {
        return orderRepository.readAllByUser(oauthId);
    }

    @Override
    public Uni<Order> updateOrderStatus(UpdateOrderStatusRequest updateOrderStatusRequest) {
        return orderRepository.read(updateOrderStatusRequest.id)
            .onItem().ifNull().failWith(new OrderNotFoundException(updateOrderStatusRequest.id))
            .onItem().transform(order -> new Order(order.getId(), order.getBuyer(), order.getOrderItems(), updateOrderStatusRequest.status, order.getOrderDate()))
            .onItem().transformToUni(orderRepository::update);
    }

    @Override
    public Uni<Void> delete(int id) {
        return orderRepository.read(id)
            .onItem().ifNull().failWith(new OrderNotFoundException(id))
            .onItem().transformToUni(order -> orderRepository.delete(id).replaceWith(Uni.createFrom().voidItem()));
    }
}