package repositories;

import java.util.List;

import entities.Order;
import exceptions.errors.OrderNotFoundException;
import interfaces.IOrderRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderRepository implements IOrderRepository {

    private static final Logger LOG = Logger.getLogger(OrderRepository.class);

    SessionFactory sessionFactory;
    public OrderRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Uni<Order> create(Order order) {
        LOG.debug("Persisting new order");
        return sessionFactory.withTransaction(session -> session.persist(order).replaceWith(order))
            .onItem().invoke(o -> LOG.debugf("Order persisted: orderId=%d", o.getId()))
            .onFailure().invoke(e -> {
                LOG.errorf("Failed to create order: %s", e.getMessage());
                throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
            });
    }

    @Override
    public Uni<Order> read(int id) {
        LOG.debugf("Fetching order from DB: orderId=%d", id);
        return sessionFactory.withSession(session ->
            session.createQuery(
                "SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id", Order.class)
                .setParameter("id", id)
                .getSingleResultOrNull()
        ).onItem().invoke(o -> {
            if (o != null) LOG.debugf("Order fetched: orderId=%d", o.getId());
        }).onItem().ifNull().failWith(() -> new OrderNotFoundException(id));
    }

    @Override
    public Uni<List<Order>> readAllByUser(String keycloakId) {
        LOG.debugf("Fetching all orders for user: keycloakId=%s", keycloakId);
        return sessionFactory.withSession(session ->
            session.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.keycloakId = :keycloakId", Order.class)
                .setParameter("keycloakId", keycloakId)
                .getResultList()
        ).onItem().invoke(list -> LOG.debugf("Fetched %d orders for user: keycloakId=%s", list.size(), keycloakId))
        .onFailure().invoke(e -> {
            LOG.errorf("Failed to retrieve orders: %s", e.getMessage());
            throw new RuntimeException("Failed to retrieve orders: " + e.getMessage(), e);
        });
    }

    @Override
    public Uni<Order> update(Order order) {
        LOG.debugf("Updating order: orderId=%d", order.getId());
        return sessionFactory.withTransaction(session -> 
            session.find(Order.class, order.getId())
                .onItem().ifNull().failWith(() -> new OrderNotFoundException(order.getId()))
                .onItem().ifNotNull().transformToUni(found -> session.merge(order))
        ).onItem().invoke(o -> LOG.debugf("Order updated: orderId=%d", o.getId()));
    }

    @Override
    public Uni<Void> delete(int id) {
        LOG.debugf("Deleting order from DB: orderId=%d", id);
        return sessionFactory.withTransaction(session -> session.find(Order.class, id)
            .onItem().ifNull().failWith(() -> new OrderNotFoundException(id))
            .onItem().ifNotNull().call(session::remove)
            .replaceWithVoid())
            .invoke(() -> LOG.debugf("Order deleted from DB: orderId=%d", id));
    }
}