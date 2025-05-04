package repositories;

import java.util.List;

import entities.Order;
import exceptions.errors.OrderNotFoundException;
import interfaces.IOrderRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;

@ApplicationScoped
public class OrderRepository implements IOrderRepository {

    SessionFactory sessionFactory;
    public OrderRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Uni<Order> create(Order order) {
        return sessionFactory.withTransaction(session -> session.persist(order).replaceWith(order))
            .onFailure().invoke(e -> {
                throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
            });
    }

    @Override
    public Uni<Order> read(int id) {
        return sessionFactory.withSession(session ->
            session.createQuery(
                "SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id", Order.class)
                .setParameter("id", id)
                .getSingleResultOrNull()
        ).onItem().ifNull().failWith(() -> new OrderNotFoundException(id));
    }

    @Override
    public Uni<List<Order>> readAllByUser(int oauthId) {
        return sessionFactory.withSession(session -> 
            session.createQuery(
                "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.buyerId = :oauthId", Order.class)
                .setParameter("oauthId", oauthId)
                .getResultList()
        ).onFailure().invoke(e -> {
            throw new RuntimeException("Failed to retrieve orders: " + e.getMessage(), e);
        });
    }

    @Override
    public Uni<Order> update(Order order) {
        return sessionFactory.withTransaction(session -> 
            session.find(Order.class, order.getId())
                .onItem().ifNull().failWith(() -> new OrderNotFoundException(order.getId()))
                .onItem().ifNotNull().transformToUni(found -> session.merge(order))
        );
    }

    @Override
    public Uni<Void> delete(int id) {
        return sessionFactory.withTransaction(session -> session.find(Order.class, id)
            .onItem().ifNull().failWith(() -> new OrderNotFoundException(id))
            .onItem().ifNotNull().call(session::remove)
            .replaceWithVoid());
    }
}