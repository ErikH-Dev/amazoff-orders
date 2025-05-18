package interfaces;

import java.util.List;

import dto.CreateOrderRequest;
import dto.UpdateOrderStatusRequest;
import entities.Order;
import io.smallrye.mutiny.Uni;

public interface IOrderService {
    Uni<Order> createPendingOrder(CreateOrderRequest orderRequest);
    Uni<Order> read(int id);
    Uni<List<Order>> readAllByUser(int oauthId);
    Uni<Order> updateOrderStatus(UpdateOrderStatusRequest orderStatusRequest);
    Uni<Void> delete(int id);
}