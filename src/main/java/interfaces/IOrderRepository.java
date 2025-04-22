package interfaces;

import java.util.List;

import entities.Order;
import io.smallrye.mutiny.Uni;

public interface IOrderRepository {
    Uni<Order> create(Order order);
    Uni<Order> read(int id);
    Uni<List<Order>> readAllByUser(int oauthId);
    Uni<Order> update(Order order);
    Uni<Void> delete(int id);
}